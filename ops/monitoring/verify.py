#!/usr/bin/env python3
"""Remote-only actual monitoring assertions; never substitutes for browser or host observations."""
import argparse
import base64
import datetime
import hashlib
import json
import math
import os
from pathlib import Path
import re
import ssl
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[2]
CASES = []
FETCH = r"""
const [url,method,payload]=process.argv.slice(1);
(async()=>{const response=await fetch(url,{method,headers:{'content-type':'application/json'},
body:method==='GET'?undefined:payload,signal:AbortSignal.timeout(5000)});
const body=await response.text(); if(body.length>524288)throw Error('bounded response exceeded');
process.stdout.write(JSON.stringify({status:response.status,body}));})().catch(()=>process.exit(4));
"""
DENIED = r"""
const https=require('node:https'),fs=require('node:fs');
const [hostname,path]=process.argv.slice(1);
const request=https.request({hostname,port:8081,path,method:'GET',
ca:fs.readFileSync('/run/secrets/service-ca'),cert:fs.readFileSync('/run/secrets/mcp-cert'),
key:fs.readFileSync('/run/secrets/mcp-key'),timeout:5000},response=>{
response.resume(); response.on('end',()=>process.stdout.write(String(response.statusCode)));});
request.on('timeout',()=>request.destroy());request.on('error',()=>process.exit(4));request.end();
"""

# Invalid input exercises mTLS/HTTP admission without creating or resolving an episode.
ALERT_POST = r"""
const https=require('node:https'),fs=require('node:fs');
const codes=new Set(['FORBIDDEN','INVALID_ALERT','UNAVAILABLE','RECORDED','PAYLOAD_TOO_LARGE']);
const tls=new Set(['ERR_TLS_CERT_ALTNAME_INVALID','CERT_HAS_EXPIRED','DEPTH_ZERO_SELF_SIGNED_CERT',
'UNABLE_TO_VERIFY_LEAF_SIGNATURE','ECONNREFUSED','ETIMEDOUT','ECONNRESET','EPIPE']);
let finished=false;
function finish(value){if(!finished){finished=true;process.stdout.write(JSON.stringify(value));}}
const request=https.request({hostname:'core',port:8081,path:'/internal/alerts',method:'POST',
ca:fs.readFileSync('/run/diagnostic/ca'),cert:fs.readFileSync('/run/diagnostic/cert'),
key:fs.readFileSync('/run/diagnostic/key'),timeout:5000,
headers:{'content-type':'application/json','content-length':'2'}},response=>{
let body='',bytes=0;response.on('data',chunk=>{bytes+=chunk.length;
if(bytes>65536){response.destroy();finish({httpStatus:response.statusCode,responseBoundExceeded:true});}
else body+=chunk.toString();});response.on('end',()=>{let code;
try{const value=JSON.parse(body).status;if(codes.has(value))code=value;}catch{}
finish({httpStatus:response.statusCode,...(code?{publicCode:code}:{})});});
response.on('error',()=>finish({httpStatus:response.statusCode,transportCode:'RESPONSE_INTERRUPTED'}));});
request.on('timeout',()=>{finish({transportCode:'ETIMEDOUT'});request.destroy();});
request.on('error',failure=>finish({transportCode:tls.has(failure.code)?failure.code:'UNCLASSIFIED'}));
request.end('{}');
"""


class AlertEvidenceFailure(AssertionError):
    def __init__(self, message, observed):
        super().__init__(message)
        self.observed = observed


def safe_alert_probe(value):
    result = {}
    status = value.get("httpStatus")
    if isinstance(status, int) and not isinstance(status, bool) and 100 <= status <= 599:
        result["httpStatus"] = status
    if value.get("publicCode") in {"FORBIDDEN", "INVALID_ALERT", "UNAVAILABLE", "RECORDED", "PAYLOAD_TOO_LARGE"}:
        result["publicCode"] = value["publicCode"]
    transport = value.get("transportCode")
    if transport in {"ERR_TLS_CERT_ALTNAME_INVALID", "CERT_HAS_EXPIRED", "DEPTH_ZERO_SELF_SIGNED_CERT",
                    "UNABLE_TO_VERIFY_LEAF_SIGNATURE", "ECONNREFUSED", "ETIMEDOUT", "ECONNRESET", "EPIPE",
                    "RESPONSE_INTERRUPTED", "UNCLASSIFIED"}:
        result["transportCode"] = transport
    if value.get("responseBoundExceeded") is True:
        result["responseBoundExceeded"] = True
    return result


def alert_ingress():
    installation = os.environ["SPOOLIN_INSTALLATION"]
    assert re.fullmatch(r"ci-[a-z0-9-]{1,60}", installation), "Owned CI installation required"
    container = compose("ps", "-q", "mcp").strip()
    doc = json.loads(command("docker", "inspect", container))[0]
    image = doc["Image"]
    assert re.fullmatch(r"sha256:[a-f0-9]{64}", image), "Actual pinned runtime image required"
    network = installation + "_private"
    assert network in doc["NetworkSettings"]["Networks"], "Owned private network required"
    args = ["docker", "run", "--rm", "--pull", "never", "--network", network,
            "--name", installation + "-alert-probe-" + uuid.uuid4().hex[:8],
            "--user", "10001:10001", "--read-only", "--cap-drop", "ALL",
            "--security-opt", "no-new-privileges:true", "--pids-limit", "32", "--memory", "128m", "--cpus", "0.25"]
    for variable, target in [("SERVICE_CA_FILE", "ca"), ("ALERTMANAGER_SERVICE_CERT_FILE", "cert"),
                             ("ALERTMANAGER_SERVICE_KEY_FILE", "key")]:
        source = Path(os.environ[variable])
        assert source.is_absolute() and not source.is_symlink(), "Protected TLS mount required"
        args += ["--mount", "type=bind,src=" + str(source) + ",dst=/run/diagnostic/" + target + ",readonly"]
    args += ["--entrypoint", "node", image, "-e", ALERT_POST]
    observed = safe_alert_probe(json.loads(command(*args, timeout=15)))
    observed.update({"identity": "alertmanager", "input": "invalid empty JSON object; not an alert episode",
                     "boundary": "private trusted TLS Core management POST"})
    if observed.get("httpStatus") != 400 or observed.get("publicCode") != "INVALID_ALERT":
        raise AlertEvidenceFailure("Alertmanager identity did not reach application input validation", observed)
    return observed


def alertmanager_metric_values(body):
    names = {"alertmanager_notifications_total", "alertmanager_notifications_failed_total",
             "alertmanager_notifications_requests_total", "alertmanager_notifications_requests_failed_total"}
    totals = {}
    for line in body.splitlines():
        match = re.fullmatch(r"([a-z_]+)(?:\{[^\n]*\})?\s+([^\s]+)(?:\s+[0-9]+)?", line)
        if not match or match[1] not in names:
            continue
        try:
            value = float(match[2])
        except ValueError:
            continue
        if not math.isfinite(value) or value < 0:
            continue
        totals[match[1]] = totals.get(match[1], 0) + value
    return {"values": totals, "absent": sorted(names - totals.keys()), "scope": "actual process all receivers"}


def alertmanager_metrics():
    response = json.loads(compose("exec", "-T", "mcp", "node", "-e", FETCH,
                                  "http://alertmanager:9093/metrics", "GET", "null"))
    assert response["status"] == 200, "Actual Alertmanager metrics unavailable"
    return alertmanager_metric_values(response["body"])


def alertmanager_diagnostics():
    try:
        return alertmanager_metrics()
    except Exception:
        return {"result": "unavailable", "failureCode": "ALERTMANAGER_METRICS_UNAVAILABLE"}


def command(*args, input_text=None, timeout=20):
    result = subprocess.run(args, cwd=ROOT, input=input_text, capture_output=True, text=True, timeout=timeout)
    if result.returncode:
        raise RuntimeError("Remote command failed: " + args[0] + " (exit " + str(result.returncode) + ")")
    if len(result.stdout) > 524288:
        raise RuntimeError("Remote output exceeded bounded inspection limit")
    return result.stdout


def compose(*args, **kwargs):
    installation = os.environ["SPOOLIN_INSTALLATION"]
    assert re.fullmatch(r"ci-[a-z0-9-]{1,60}", installation), "Only isolated owned CI installations are permitted"
    return command("docker", "compose", "--project-name", installation, "-f", str(ROOT / "ops/compose.yaml"), *args, **kwargs)


def internal(url, method="GET", payload=None):
    response = json.loads(compose("exec", "-T", "mcp", "node", "-e", FETCH, url, method, json.dumps(payload)))
    assert response["status"] in (200, 202), "Private monitoring API request failed"
    return json.loads(response["body"]) if response["body"] else None


def query(expression):
    data = internal("http://prometheus:9090/api/v1/query?" + urllib.parse.urlencode({"query": expression}))
    assert data["status"] == "success", "Prometheus query failed"
    return data["data"]["result"]


def protected(path):
    source = Path(path)
    assert source.is_absolute() and not source.is_symlink() and source.is_file(), "Protected credential path required"
    assert source.stat().st_mode & 0o777 == 0o600, "Protected credential permissions required"
    return source.read_text().strip()


def grafana(path, authenticated=True):
    context = ssl.create_default_context(cafile=os.environ["SERVICE_CA_FILE"])
    request = urllib.request.Request("https://localhost:" + os.environ.get("GRAFANA_HTTPS_PORT", "3000") + path)
    if authenticated:
        password = protected(os.environ["GRAFANA_ADMIN_PASSWORD_CLIENT_FILE"])
        value = base64.b64encode(("operator:" + password).encode()).decode()
        request.add_header("Authorization", "Basic " + value)
    try:
        with urllib.request.urlopen(request, context=context, timeout=5) as response:
            data = response.read(524289)
            assert len(data) <= 524288, "Grafana response exceeds bounded inspection limit"
            return response.status, json.loads(data)
    except urllib.error.HTTPError as failure:
        return failure.code, None


def case(identifier, boundary, action):
    start = time.monotonic()
    try:
        observed = action()
        CASES.append({"id": identifier, "boundary": boundary, "result": "passed", "observed": observed,
                      "durationSeconds": round(time.monotonic() - start, 3)})
    except Exception as failure:
        failed = {"id": identifier, "boundary": boundary, "result": "failed", "failure": str(failure)[:300],
                  "durationSeconds": round(time.monotonic() - start, 3)}
        if isinstance(failure, AlertEvidenceFailure):
            failed["observed"] = failure.observed
        CASES.append(failed)


def configurations():
    compose("exec", "-T", "prometheus", "promtool", "check", "config", "/etc/prometheus/prometheus.yaml")
    compose("exec", "-T", "prometheus", "promtool", "test", "rules", "/etc/prometheus/alert-rules.test.yaml")
    compose("exec", "-T", "alertmanager", "amtool", "check-config", "/etc/alertmanager/alertmanager.yaml")
    return {"promtoolConfig": "passed", "promtoolRules": "passed", "amtoolConfig": "passed"}


def scrape():
    deadline = time.monotonic() + 75
    targets = []
    while time.monotonic() < deadline:
        targets = internal("http://prometheus:9090/api/v1/targets")["data"]["activeTargets"]
        if len(targets) == 4 and all(target["health"] == "up" for target in targets):
            break
        time.sleep(3)
    assert len(targets) == 4 and all(target["health"] == "up" for target in targets), "All three real JVM exporters and MCP exporter must be scraped successfully"
    assert {target["labels"]["service"] for target in targets} == {"core", "catalog", "media", "mcp"}
    assert all(target["scrapeUrl"].startswith("https://") for target in targets if target["labels"]["service"] != "mcp")
    required = ["spoolin_work_items", "spoolin_oldest_due_work_seconds", "spoolin_notification_events",
                "spoolin_pool_active_connections", "spoolin_pool_max_connections", "spoolin_service_ready"]
    for metric in required:
        samples = query(metric)
        assert {sample["metric"]["service"] for sample in samples} >= {"core", "catalog", "media"}, "Required owner metric missing: " + metric
        assert all(sample["value"][1] not in ("NaN", "+Inf", "-Inf") for sample in samples), "Owner measurement unhealthy: " + metric
    assert query("http_server_requests_seconds_count"), "Actual HTTP server measurement is missing"
    return {"targets": [{"service": target["labels"]["service"], "health": target["health"],
                         "scrapeUrl": target["scrapeUrl"]} for target in targets], "ownerMetricFamilies": required}


def authorization():
    observed = {}
    for owner in ("core", "catalog", "media"):
        paths = ("/actuator/prometheus", "/internal/alerts") if owner == "core" else ("/actuator/prometheus",)
        for path in paths:
            result = compose("exec", "-T", "mcp", "node", "-e", DENIED, owner, path)
            status = int(result) if result.isdecimal() else None
            observed[owner + path] = status
            assert status == 403, "MCP identity must be denied at " + owner + path + "; observed HTTP " + str(status)
    return {"mcpDenied": observed, "approvedScrapeIdentity": "prometheus"}


def dashboard():
    assert grafana("/api/search", authenticated=False)[0] == 401, "Dashboard API must require authentication"
    code, health = grafana("/api/health")
    assert code == 200 and health["database"] == "ok", "Grafana persistence is unavailable"
    code, source = grafana("/api/datasources/uid/spoolin-prometheus/health")
    assert code == 200 and source["status"] == "OK", "Provisioned datasource must query actual Prometheus"
    code, doc = grafana("/api/dashboards/uid/spoolin-operations")
    assert code == 200 and doc["meta"]["provisioned"], "Expected provisioned dashboard missing"
    panels = doc["dashboard"]["panels"]
    assert len(panels) >= 15, "Required dashboard panels missing"
    assert all("vector(0)" not in target["expr"] for panel in panels for target in panel.get("targets", [])), "Absent metrics cannot be hidden with zeros"
    return {"version": health["version"], "dashboardUid": doc["dashboard"]["uid"], "panels": len(panels),
            "authentication": "required", "source": "actual Prometheus", "renderedBrowserObservation": "separate obligation"}


def evidence_counts(start):
    assert re.fullmatch(r"[0-9]{4}-[0-9T:.-]+Z", start)
    sql = "SELECT json_build_object('firing',count(*) FILTER (WHERE notification_summary->>'status'='firing')," + \
          "'resolved',count(*) FILTER (WHERE notification_summary->>'status'='resolved')," + \
          "'keys',count(DISTINCT notification_key),'recorded',bool_and(notification_state='RECORDED')) " + \
          "FROM appdata.durable_work WHERE kind='NOTIFICATION' AND notification_summary->>'alertname'='WorkQueueBacklog' " + \
          "AND notification_summary->>'startsAt'='" + start + "';"
    script = 'export PGPASSWORD="$(cat /run/secrets/core-backup-password)"; exec psql -X -qAt -h localhost -U spoolin_core_backup -d spoolin_core -v ON_ERROR_STOP=1'
    value = compose("exec", "-T", "postgres", "bash", "-c", script, input_text=sql)
    return json.loads(value.strip())


def wait_counts(start, expected):
    deadline = time.monotonic() + 65
    while time.monotonic() < deadline:
        counts = evidence_counts(start)
        if all(counts.get(key) == value for key, value in expected.items()):
            return counts
        time.sleep(2)
    metrics = alertmanager_diagnostics()
    raise AlertEvidenceFailure("Actual durable notification episode evidence did not reach expected counts",
                               {"expectedCounts": expected, "durableCounts": counts, "alertmanagerMetrics": metrics})


def alert_delivery():
    assert os.environ.get("NOTIFICATION_MODE", "database") == "database", "This isolated pipeline journey requires approved database notification mode"
    start = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0)
    stamp = lambda date: date.isoformat().replace("+00:00", "Z")
    alert = {"labels": {"alertname": "WorkQueueBacklog", "service": "core", "severity": "warning", "class": "work"},
             "annotations": {"summary": "Durable work backlog elevated"}, "startsAt": stamp(start),
             "endsAt": stamp(start + datetime.timedelta(minutes=5))}
    before_metrics = alertmanager_diagnostics()
    try:
        internal("http://alertmanager:9093/api/v2/alerts", "POST", [alert])
        firing = wait_counts(stamp(start), {"firing": 1, "resolved": 0, "keys": 1, "recorded": True})
        internal("http://alertmanager:9093/api/v2/alerts", "POST", [alert])
        time.sleep(2)
        assert evidence_counts(stamp(start)) == firing, "Duplicate firing delivery changed durable evidence"
        alert["endsAt"] = stamp(datetime.datetime.now(datetime.timezone.utc))
        internal("http://alertmanager:9093/api/v2/alerts", "POST", [alert])
        resolved = wait_counts(stamp(start), {"firing": 1, "resolved": 1, "keys": 2, "recorded": True})
        return {"boundary": "actual Alertmanager to mTLS Core ingress to PostgreSQL", "firing": firing, "resolved": resolved,
                "alertmanagerBefore": before_metrics, "alertmanagerAfter": alertmanager_diagnostics(),
                "injection": "owned synthetic Alertmanager API episode; threshold evaluation separately tested by promtool",
                "dedup": "actual Alertmanager duplicate coalescing and durable unique episode counts"}
    except AlertEvidenceFailure as failure:
        failure.observed["alertmanagerBefore"] = before_metrics
        raise
    finally:
        alert["endsAt"] = stamp(datetime.datetime.now(datetime.timezone.utc))
        internal("http://alertmanager:9093/api/v2/alerts", "POST", [alert])


def identities():
    result = {}
    for service in ("prometheus", "alertmanager", "grafana"):
        container = compose("ps", "-q", service).strip()
        assert container, "Selected monitoring process missing"
        doc = json.loads(command("docker", "inspect", container))[0]
        assert doc["State"]["Running"] and doc["Config"]["User"] == "10001:10001"
        assert doc["HostConfig"]["ReadonlyRootfs"] is True
        port, setting = {
            "prometheus": (9090, "PROMETHEUS_HTTP_PORT"),
            "alertmanager": (9093, "ALERTMANAGER_HTTP_PORT"),
            "grafana": (3000, "GRAFANA_HTTPS_PORT"),
        }[service]
        configured = os.environ.get(setting, str(port))
        expected = {str(port) + "/tcp": [{"HostIp": "127.0.0.1", "HostPort": configured}]}
        assert doc["HostConfig"].get("PortBindings") == expected, "Operator monitoring port must bind only configured loopback"
        published = {port: bindings for port, bindings in doc["NetworkSettings"]["Ports"].items() if bindings}
        assert published == expected, "Actual monitoring listener publication differs from loopback contract"
        result[service] = {"imageId": doc["Image"], "imageTag": doc["Config"]["Image"],
                           "user": doc["Config"]["User"], "publishedPorts": expected}

    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    if os.environ.get("GITHUB_ACTIONS") != "true" or os.environ.get("RUNNER_ENVIRONMENT") != "github-hosted" or sys.platform != "linux":
        raise SystemExit("Refusing actual monitoring execution outside GitHub-hosted Linux")
    for identifier, boundary, action in [("MON-CONFIG", "actual pinned validators", configurations),
        ("MON-ISOLATION", "actual container configuration", identities), ("MON-SCRAPE", "actual mTLS scrapes", scrape),
        ("MON-AUTHORIZATION", "actual management identity denial", authorization),
        ("MON-DASHBOARD", "actual authenticated Grafana and datasource API", dashboard),
        ("MON-ALERT-INGRESS", "actual Alertmanager certificate management admission", alert_ingress),
        ("MON-ALERT-EPISODE", "actual Alertmanager Core database flow", alert_delivery)]:
        case(identifier, boundary, action)
    doc = {"schemaVersion": 1, "testedSha": command("git", "rev-parse", "HEAD").strip(),
           "workflowRunId": os.environ.get("GITHUB_RUN_ID"), "workflowJob": os.environ.get("GITHUB_JOB"),
           "runnerOS": os.environ.get("RUNNER_OS"), "runnerImage": os.environ.get("ImageOS"),
           "runnerImageVersion": os.environ.get("ImageVersion"), "results": CASES,
           "configurationHashes": {str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
               for path in sorted((ROOT / "ops/monitoring").rglob("*")) if path.is_file()},
           "remainingProof": ["rendered Grafana browser observations", "missing logical instrumenters remain separately tracked",
                              "registered tunnel and required assisted-client hosts", "external provider acceptance where selected"]}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(doc, indent=2) + "\n")
    if any(result["result"] != "passed" for result in CASES):
        raise SystemExit("Monitoring assertions failed; inspect the bounded report")


if __name__ == "__main__":
    main()
