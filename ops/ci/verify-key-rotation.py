#!/usr/bin/env python3
"""Actual protected rotation CLI and HTTPS continuity on the owned make-start CI installation."""
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import ssl
import subprocess
import tempfile
import time
import urllib.request
import urllib.error
import uuid

ROOT = Path(__file__).resolve().parents[2]


def require(value, message):
    if not value:
        raise RuntimeError(message)


def compose(*args, **kwargs):
    command = ['docker', 'compose', '--project-name', os.environ['SPOOLIN_INSTALLATION'],
               '-f', str(ROOT / 'ops/compose.yaml'), *args]
    return subprocess.run(command, cwd=ROOT, check=True, timeout=180, **kwargs)


def database():
    sql = """BEGIN READ ONLY;
SELECT json_build_object(
 'rotationEvents',(SELECT COALESCE(json_agg(e ORDER BY id),'[]') FROM (SELECT id,outcome,changes,operator_id FROM appdata.operational_event WHERE action='KEY_ROTATION') e),
 'keys',(SELECT json_agg(k ORDER BY id) FROM (SELECT id,purpose,state,next_nonce FROM appdata.crypto_key_version) k),
 'accounts',(SELECT json_agg(a ORDER BY id) FROM (SELECT id,pii_key_id,md5(pii_nonce::text||pii_cipher::text) envelope FROM appdata.user_account) a),
 'drafts',(SELECT COALESCE(json_agg(d ORDER BY id),'[]') FROM (SELECT id,body_key_id,version,md5(body_nonce::text||body_cipher::text) envelope FROM appdata.form_draft) d),
 'aliases',(SELECT COALESCE(json_agg(e ORDER BY account_id,key_id),'[]') FROM (SELECT account_id,key_id,md5(digest::text) value FROM appdata.account_email_lookup) e));
COMMIT;"""
    shell = 'export PGPASSWORD="$(cat /run/secrets/core-backup-password)"; exec psql -X -qAt -v ON_ERROR_STOP=1 -h /var/run/postgresql -U spoolin_core_backup -d spoolin_core'
    result = compose('exec', '-T', 'postgres', '/bin/sh', '-ec', shell,
                     input=sql, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return json.loads(result.stdout)


class Api:
    def __init__(self):
        self.origin = 'https://localhost:' + os.environ.get('CORE_HTTPS_PORT', '8443')
        context = ssl.create_default_context(cafile=os.environ['SERVICE_CA_FILE'])
        self.opener = urllib.request.build_opener(urllib.request.HTTPSHandler(context=context),
                                                  urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.last_request = 0
        self.csrf = None

    def request(self, query, variables=None):
        # Respect the unchanged configured admission window; do not clear rate buckets for this proof.
        time.sleep(max(0, 5 - (time.monotonic() - self.last_request)))
        headers = {'Content-Type': 'application/json', 'Origin': self.origin, 'X-Spoolin-Client': 'web'}
        if self.csrf:
            headers['X-CSRF-Token'] = self.csrf
        request = urllib.request.Request(self.origin + '/graphql',
                                         json.dumps({'query': query, 'variables': variables or {}}).encode(), headers)
        self.last_request = time.monotonic()
        payload = self.open(request)
        require(not payload.get('errors') and isinstance(payload.get('data'), dict), 'GraphQL request failed; response withheld')
        return payload['data']

    def open(self, request):
        for attempt in range(4):
            try:
                with self.opener.open(request, timeout=20) as response:
                    return json.load(response)
            except urllib.error.HTTPError as failure:
                wait = failure.headers.get('Retry-After', '')
                if failure.code != 429 or not wait.isdecimal() or not 1 <= int(wait) <= 60 or attempt == 3:
                    raise RuntimeError('HTTPS admission/request failed; response withheld') from None
                time.sleep(int(wait))
        raise RuntimeError('Bounded admission retries exhausted')

    def session(self):
        result = self.request('query { session { authenticated accountId displayName csrfToken } }')['session']
        self.csrf = result['csrfToken']
        return result

    def login(self, email, password):
        self.session()
        result = self.request('mutation($id:UUID!,$email:String!,$password:String!){signIn(operationId:$id,email:$email,password:$password){operation{state} problem{code}}}',
                              {'id': str(uuid.uuid4()), 'email': email, 'password': password})['signIn']
        require(result['operation']['state'] == 'SUCCEEDED' and not result.get('problem'), 'Canary sign-in failed')
        session = self.session()
        require(session['authenticated'], 'Canary session was not established')
        return session

    def draft(self, identity, operation, policy, values):
        variables = {'id': identity, 'op': operation,
                     'input': {'targetKind': 'MOVIE', 'values': values, 'policyRevision': policy}}
        result = self.request('mutation($id:UUID!,$op:UUID!,$input:DraftInput!){saveDraft(operationId:$op,id:$id,input:$input){operation{id state resultId version} problem{code}}}', variables)['saveDraft']
        require(result['operation']['state'] == 'SUCCEEDED' and not result.get('problem'), 'Actual draft save failed')
        return result


def readiness():
    context = ssl.create_default_context(cafile=os.environ['SERVICE_CA_FILE'])
    url = 'https://localhost:' + os.environ.get('CORE_HTTPS_PORT', '8443') + '/'
    deadline = time.monotonic() + 120
    while time.monotonic() < deadline:
        try:
            with urllib.request.urlopen(url, context=context, timeout=5) as response:
                if response.status == 200:
                    return
        except (OSError, TimeoutError):
            time.sleep(2)
    raise RuntimeError('Actual Core readiness did not recover after rotation')


def protected_json(file):
    return json.loads(subprocess.check_output(['sudo', 'cat', str(file)], stderr=subprocess.PIPE))


def install(file, value, temporary):
    staged = temporary / secrets.token_hex(8)
    staged.write_bytes(value if isinstance(value, bytes) else json.dumps(value).encode())
    staged.chmod(0o600)
    next_file = file.with_name(file.name + '.rotation-next')
    subprocess.run(['sudo', 'install', '-o', '10001', '-g', '10001', '-m', '600', str(staged), str(next_file)], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    subprocess.run(['sudo', 'mv', '-T', str(next_file), str(file)], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    staged.unlink()


def provision(snapshot, temporary, run_label):
    runtime = Path(os.environ['SPOOLIN_RUNTIME_CONFIG_DIR'])
    expected = Path(os.environ['RUNNER_TEMP']) / ('spoolin-' + os.environ['GITHUB_RUN_ID'] + '-' + os.environ['GITHUB_RUN_ATTEMPT']) / 'runtime'
    require(runtime == expected, 'Only the owned synthetic runtime configuration may change')
    core = protected_json(runtime / 'core.json')
    mounts = core['spoolin']['account-protection']['keys']
    plan = {}
    for purpose in ('PII', 'DRAFT', 'EMAIL_LOOKUP'):
        source = [row['id'] for row in snapshot['keys'] if row['purpose'] == purpose and row['state'] == 'WRITE']
        require(len(source) == 1 and source[0] in mounts, 'Missing source in live application configuration')
        target = 'ci_' + os.environ['GITHUB_RUN_ID'] + '_' + os.environ['GITHUB_RUN_ATTEMPT'] + '_' + run_label + '_' + purpose.lower()
        require(target not in mounts, 'Target key already exists; do not replace protected versions')
        install(runtime / target, secrets.token_bytes(32), temporary)
        mounts[target] = {'purpose': purpose, 'file': '/run/spoolin-config/' + target}
        plan[purpose] = (source[0], target)
    # Both mounted application settings and the live reload map know every new version before activation.
    install(runtime / 'rotation-keys.json', mounts, temporary)
    install(runtime / 'core.json', core, temporary)
    return plan


def rotate(label, purpose, source, target, reports, execute=False, register=False, expected=(0,)):
    command = ['bash', 'ops/bin/rotate-keys.sh', '--from', source, '--to', target,
               '--purpose', purpose, '--max-batches', '1']
    if execute:
        command.append('--execute')
    if register:
        command.append('--register-target')
    result = subprocess.run(command, cwd=ROOT, text=True, capture_output=True, timeout=180, check=False)
    require(result.returncode in expected, 'Rotation command failed; raw output withheld')
    rows = []
    for line in result.stdout.splitlines():
        try:
            row = json.loads(line)
            if isinstance(row, dict) and set(('ok', 'purpose', 'dryRun', 'backfilled', 'remaining')).issubset(row):
                rows.append(row)
        except json.JSONDecodeError:
            pass
    require(len(rows) == 1 and rows[0]['purpose'] == purpose, 'Missing actual rotation outcome')
    record = {'exit': result.returncode, **rows[0]}
    (reports / (label + '.json')).write_text(json.dumps(record, indent=2) + '\n')
    return record


def activate(plan, reports):
    for purpose, (source, target) in plan.items():
        before = database()
        dry = rotate('unregistered-dry-' + purpose, purpose, source, target, reports, expected=(1,))
        require(not dry['ok'] and dry['dryRun'], 'Unregistered dry-run incorrectly claimed success')
        require(database() == before, 'Dry-run changed database state')
        result = rotate('execute-' + purpose, purpose, source, target, reports, execute=True, register=True, expected=(0, 1))
        attempts = 1
        while result['remaining'] and attempts < 20:
            result = rotate('resume-' + purpose + '-' + str(attempts), purpose, source, target, reports, execute=True, expected=(0, 1))
            attempts += 1
        require(result['ok'] and result['remaining'] == 0, 'Actual backfill remains incomplete')
        snapshot = database()
        replay = rotate('replay-' + purpose, purpose, source, target, reports, execute=True)
        require(replay['ok'] and replay['backfilled'] == 0 and replay['remaining'] == 0, 'Rotation repeat duplicated domain work')
        after = database()
        require(all(snapshot[key] == after[key] for key in ('accounts', 'drafts', 'aliases')), 'Rotation repeat rewrote protected rows')
        dry = rotate('registered-dry-' + purpose, purpose, source, target, reports)
        require(dry['ok'] and database() == after, 'Registered dry-run changed database state')


def verify_keys(snapshot, plan):
    states = {row['id']: (row['purpose'], row['state']) for row in snapshot['keys']}
    for purpose, (source, target) in plan.items():
        require(states[source] == (purpose, 'READ') and states[target] == (purpose, 'WRITE'), 'Source retention or WRITE selection differs')
    for purpose in plan:
        require(any(row['outcome'] == 'SUCCEEDED' and row['changes']['phase'] == 'COMPLETED'
                    and row['changes']['purpose'] == purpose and row['operator_id'].startswith('local-uid:')
                    for row in snapshot['rotationEvents']), 'Immutable rotation completion evidence missing')
    require(all(row['pii_key_id'] == plan['PII'][1] for row in snapshot['accounts']), 'PII backfill incomplete')
    require(all(row['body_key_id'] == plan['DRAFT'][1] for row in snapshot['drafts']), 'Draft backfill incomplete')
    aliases = {(row['account_id'], row['key_id']) for row in snapshot['aliases']}
    require(all((row['id'], key) in aliases for row in snapshot['accounts'] for key in plan['EMAIL_LOOKUP']), 'Dual lookup aliases incomplete')


def prepare_api():
    client = Api()
    password = Path(os.environ['SPOOLIN_BROWSER_MAINTAINER_PASSWORD_FILE']).read_text()
    session = client.login('maintainer@example.invalid', password)
    policy = client.request('query { policy { revision } }')['policy']['revision']
    draft, operation = str(uuid.uuid4()), str(uuid.uuid4())
    values = {'title': 'Rotation canary draft', 'synopsis': 'Retained before and after actual key activation.'}
    result = client.draft(draft, operation, policy, values)
    return client, session, draft, operation, policy, values, result


def verify_api(retained):
    client, session, draft, operation, policy, values, result = retained
    after = client.session()
    require(after['authenticated'] and after['accountId'] == session['accountId'] and after['displayName'] == session['displayName'], 'Existing account PII/session continuity differs')
    drafts = client.request('query { drafts { id values version } }')['drafts']
    require(any(row['id'] == draft and row['values'] == values for row in drafts), 'Existing draft failed actual API decryption')
    before = database()
    require(client.draft(draft, operation, policy, values) == result, 'Original save receipt replay changed after rotation')
    require(database() == before, 'Draft replay allocated nonce or changed retained state')
    client.draft(str(uuid.uuid4()), str(uuid.uuid4()), policy, {'title': 'Post-rotation draft'})
    viewer = Api()
    viewer.session()
    email = 'rotation-' + os.environ['GITHUB_RUN_ID'] + '-' + os.environ['GITHUB_RUN_ATTEMPT'] + '@example.invalid'
    password = secrets.token_urlsafe(24)
    created = viewer.request('mutation($id:UUID!,$input:RegisterInput!){register(operationId:$id,input:$input){operation{state resultId} problem{code}}}',
                             {'id': str(uuid.uuid4()), 'input': {'name': 'Rotation Probe', 'email': email, 'password': password}})['register']
    require(created['operation']['state'] == 'SUCCEEDED' and not created.get('problem'), 'New account failed after rotation')
    require(viewer.login(email, password)['accountId'] == created['operation']['resultId'], 'New lookup alias or PII read failed')


def main():
    require(os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ.get('RUNNER_ENVIRONMENT') == 'github-hosted'
            and os.environ.get('RUNNER_OS') == 'Linux', 'Requires verified GitHub-hosted Linux')
    require(os.environ['SPOOLIN_INSTALLATION'] == 'ci-' + os.environ['GITHUB_RUN_ID'] + '-' + os.environ['GITHUB_RUN_ATTEMPT'], 'Requires uniquely owned primary CI installation')
    require(os.environ.get('ROTATION_KEYS_FILE', '/run/spoolin-config/rotation-keys.json') == '/run/spoolin-config/rotation-keys.json', 'Unexpected application reload map')
    run_label = uuid.uuid4().hex[:8]
    reports = ROOT / 'artifacts/key-rotation' / run_label
    reports.mkdir(parents=True, exist_ok=False)
    retained = prepare_api()
    snapshot = database()
    require(snapshot['accounts'] and snapshot['drafts'], 'Actual PII and draft rows are required')
    compose('stop', 'core', stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    with tempfile.TemporaryDirectory(prefix='spoolin-key-rotation-', dir=os.environ['RUNNER_TEMP']) as staging:
        # Failure keeps Core stopped for diagnosis; never resume a writer with unverified material.
        plan = provision(snapshot, Path(staging), run_label)
        activate(plan, reports)
    verify_keys(database(), plan)
    compose('up', '-d', '--no-deps', 'core', stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    readiness()
    verify_api(retained)
    verify_keys(database(), plan)
    report = {'testedSha': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
              'runId': os.environ['GITHUB_RUN_ID'], 'attempt': os.environ['GITHUB_RUN_ATTEMPT'],
              'runner': os.environ['RUNNER_ENVIRONMENT'], 'runnerOS': os.environ['RUNNER_OS'],
              'imageVersion': os.environ.get('ImageVersion'), 'result': 'passed',
              'proof': ['actual container dry-run/execute', 'all three purposes', 'protected application/reload provisioning',
                        'quiesced activation/backfill', 'repeat-safe rotation', 'API account/draft continuity after restart',
                        'original draft receipt replay', 'new account/draft writes and dual lookup aliases'],
              'sourceKeysRetainedRead': True, 'retirement': 'Not attempted; retained backups require separate horizon review',
              'interruptedBatchProof': 'Only observed if execute reports remaining rows; owner integration tests remain required for forced interruption'}
    (reports / 'summary.json').write_text(json.dumps(report, indent=2) + '\n')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        raise SystemExit('Key rotation verification failed; inspect bounded redacted outcomes; credentials and responses withheld')
