"""Synthetic database fixtures exclusively for operator recovery/retention fault cases."""
import uuid


def identity():
    return str(uuid.uuid4())


def receipt(identifier, terminal=True, age=100):
    state = "SUCCEEDED" if terminal else "PENDING"
    return ("INSERT INTO appdata.operation_receipt(id,actor_scope,kind,request_hash,state,result,correlation_id,expires_at) "
            f"VALUES('{identifier}','SERVICE_CI_OPERATOR','CI_OPERATOR',decode(repeat('31',32),'hex'),'{state}',"
            f"'{{\"marker\":\"synthetic-operator-test\"}}','{identity()}',CURRENT_TIMESTAMP-INTERVAL '{age} days');")


def work(identifier, operation, state="EXHAUSTED", notification=None):
    columns = ""
    values = ""
    if notification:
        columns = ",notification_key,notification_mode,notification_state,notification_summary"
        values = f",'ci-{identifier}','slack','{notification}','{{\"code\":\"NOTIFICATION_CONNECT_FAILED\"}}'"
    kind = "NOTIFICATION" if notification else "CI_RETENTION"
    return ("INSERT INTO appdata.durable_work(id,operation_id,kind,payload,state,attempt_count,max_attempts,"
            "deadline_at,lease_epoch,correlation_id,base_delay_ms,cap_delay_ms,backoff_multiplier,created_at,updated_at"
            f"{columns}) VALUES('{identifier}','{operation}','{kind}','{{\"fixture\":true}}','{state}',1,3,"
            f"CURRENT_TIMESTAMP-INTERVAL '100 days',1,'{identity()}',500,5000,2,"
            f"CURRENT_TIMESTAMP-INTERVAL '100 days',CURRENT_TIMESTAMP-INTERVAL '100 days'{values});")


def phases(work_id, operation, uncertain=False, paired=True):
    statements = []
    for event in (["ATTEMPT_STARTED", "ATTEMPT_FINISHED"] if paired else ["ATTEMPT_STARTED"]):
        outcome = "NULL" if event == "ATTEMPT_STARTED" else "'FAILED'"
        attempt = "NULL" if event == "ATTEMPT_STARTED" else ("'UNCERTAIN'" if uncertain else "'TRANSIENT'")
        error = "NULL" if event == "ATTEMPT_STARTED" else "'NOTIFICATION_CONNECT_FAILED'"
        statements.append("INSERT INTO appdata.operational_event(id,operation_id,actor_kind,action,subject_kind,"
                          "outcome,correlation_id,event_type,work_id,generation,attempt_no,lease_epoch,attempt_outcome,"
                          f"error_code,created_at) VALUES('{identity()}','{operation}','SERVICE','CI_OPERATOR','WORK',"
                          f"{outcome},'{identity()}','{event}','{work_id}',0,1,1,{attempt},{error},"
                          "CURRENT_TIMESTAMP-INTERVAL '100 days');")
    return "".join(statements)


def recovery_fixture():
    operation, work_id = identity(), identity()
    sql = receipt(operation, terminal=False) + work(work_id, operation, notification="FAILED") + phases(work_id, operation)
    return operation, work_id, sql


def retention_fixture():
    operation = identity()
    entries = {}
    sql = receipt(operation)
    for label, state, uncertain, paired in (
        ("eligible", "DONE", False, True), ("exhausted", "EXHAUSTED", False, True),
        ("uncertain", "DONE", True, True), ("unpaired", "DONE", False, False),
    ):
        entries[label] = identity()
        # Separate operations prevent a deliberately unresolved sibling from blocking the eligible row.
        parent = identity()
        sql += receipt(parent) + work(entries[label], parent, state) + phases(entries[label], parent, uncertain, paired)
    expired = [identity() for _ in range(101)]
    sql += "".join(receipt(item, age=200) for item in expired)
    return entries, expired, sql
