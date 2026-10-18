"""Trusted HTTPS application requests; credentials stay in process memory and never enter samples."""
from dataclasses import dataclass
import http.client
from http.cookies import SimpleCookie
import json
import ssl
import time
import uuid

MAX_RESPONSE_BYTES = 1048576
SAFE_CODES = frozenset({"RATE_LIMITED", "RESOURCE_EXHAUSTED", "UNAVAILABLE", "DEADLINE_EXCEEDED",
                        "UNAUTHENTICATED", "PERMISSION_DENIED", "FIELD_INVALID", "NOT_FOUND", "CONFLICT",
                        "POLICY_CHANGED", "VERSION_CONFLICT", "IDEMPOTENCY_CONFLICT", "INTERNAL", "CANCELLED"})


@dataclass
class Response:
    status: int
    body: dict
    elapsed_ns: int
    request_bytes: int
    response_bytes: int
    transport: str | None = None

    def safe_result(self):
        errors = self.body.get("errors")
        roots = self.body.get("data") or {}
        problems = [row.get("problem") for row in roots.values() if isinstance(row, dict)]
        operations = [row.get("operation") for row in roots.values() if isinstance(row, dict)]
        problems += [row.get("problem") for row in operations if isinstance(row, dict)]
        codes = [row.get("code") for row in problems if isinstance(row, dict)]
        if isinstance(errors, list):
            codes += [(row.get("extensions") or {}).get("code") for row in errors if isinstance(row, dict)]
        code = next((item for item in codes if item in SAFE_CODES), "UNCLASSIFIED" if codes or errors else None)
        states = [row.get("state") for row in operations if isinstance(row, dict)]
        return {"httpStatus": self.status, "transportError": self.transport, "problem": code,
                "domainError": bool(errors or codes or any(state != "SUCCEEDED" for state in states)),
                "errorCodes": [item if item in SAFE_CODES else "UNCLASSIFIED" for item in codes],
                "graphqlErrorCount": len(errors) if isinstance(errors, list) else int(bool(errors)),
                "operationStates": [state for state in states if state in {"SUCCEEDED", "FAILED", "PENDING", "EXPIRED"}],
                "clientElapsedNanos": self.elapsed_ns, "requestBytes": self.request_bytes,
                "responseBytes": self.response_bytes}


class ApplicationClient:
    def __init__(self, environment):
        self.port = int(environment["CORE_HTTPS_PORT"])
        self.origin = f"https://localhost:{self.port}"
        self.context = ssl.create_default_context(cafile=environment["SERVICE_CA_FILE"])
        self.connection = None
        self.cookies = {}
        self.csrf = None

    def close(self):
        if self.connection is not None:
            self.connection.close()
        self.connection = None

    def request(self, query, variables=None):
        payload = json.dumps({"query": query, "variables": variables or {}}, separators=(",", ":")).encode()
        headers = {"Content-Type": "application/json", "Origin": self.origin, "X-Spoolin-Client": "web"}
        if self.cookies:
            headers["Cookie"] = "; ".join(f"{name}={value}" for name, value in self.cookies.items())
        if self.csrf:
            headers["X-CSRF-Token"] = self.csrf
        started = time.monotonic_ns()
        try:
            if self.connection is None:
                self.connection = http.client.HTTPSConnection("localhost", self.port, timeout=10, context=self.context)
            self.connection.request("POST", "/graphql", body=payload, headers=headers)
            response = self.connection.getresponse()
            raw = response.read(MAX_RESPONSE_BYTES + 1)
            if len(raw) > MAX_RESPONSE_BYTES:
                raise ValueError("Bounded application response exceeded")
            self.accept_cookies(response.getheaders())
            body = json.loads(raw)
            if not isinstance(body, dict):
                raise ValueError("Invalid application response shape")
            return Response(response.status, body, time.monotonic_ns() - started, len(payload), len(raw))
        except (OSError, TimeoutError, http.client.HTTPException, ValueError) as error:
            self.close()
            kind = "TIMEOUT" if isinstance(error, TimeoutError) else "TRANSPORT_OR_SHAPE"
            return Response(0, {}, time.monotonic_ns() - started, len(payload), 0, kind)

    def accept_cookies(self, headers):
        for name, value in headers:
            if name.lower() == "set-cookie":
                parsed = SimpleCookie()
                parsed.load(value)
                self.cookies.update({key: item.value for key, item in parsed.items()})

    def data(self, query, variables=None):
        response = self.request(query, variables)
        observed = response.safe_result()
        if response.status != 200 or observed["transportError"] or observed["domainError"]:
            raise RuntimeError("Actual application request failed; response contents withheld")
        data = response.body.get("data")
        if not isinstance(data, dict):
            raise RuntimeError("Actual application response omitted data")
        return data

    def sign_in(self, password):
        self.csrf = self.data("query {session {csrfToken}} ")["session"]["csrfToken"]
        query = "mutation($id:UUID!,$password:String!){signIn(operationId:$id,email:\"maintainer@example.invalid\",password:$password){operation{state} problem{code}}}"
        self.data(query, {"id": str(uuid.uuid4()), "password": password})
        state = self.data("query {session {authenticated role csrfToken}} ")["session"]
        if state["authenticated"] is not True or state["role"] != "MAINTAINER":
            raise RuntimeError("Prepared Maintainer session was not established")
        self.csrf = state["csrfToken"]
