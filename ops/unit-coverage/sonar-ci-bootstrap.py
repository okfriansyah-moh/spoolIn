"""Create credentials only for a fresh, isolated CI Sonar database."""
import base64
import json
import os
from pathlib import Path
import secrets
import urllib.parse
import urllib.request


def post(endpoint, values, password):
    request = urllib.request.Request(
        'http://127.0.0.1:9000/api/' + endpoint,
        data=urllib.parse.urlencode(values).encode(),
        headers={'Authorization': 'Basic ' + base64.b64encode(('admin:' + password).encode()).decode()},
        method='POST',
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        body = response.read()
        return json.loads(body) if body else {}


def main():
    if os.environ.get('GITHUB_ACTIONS') != 'true' or not os.environ.get('GITHUB_ENV'):
        raise ValueError('Bootstrap is limited to the fresh GitHub Actions dashboard')
    password = secrets.token_urlsafe(40)
    post('users/change_password', {'login': 'admin', 'previousPassword': 'admin', 'password': password}, 'admin')
    result = post('user_tokens/generate', {'name': 'isolated-ci-analysis'}, password)
    token = result['token']
    print('::add-mask::' + token)
    with Path(os.environ['GITHUB_ENV']).open('a') as target:
        target.write('SONAR_TOKEN=' + token + '\n')


if __name__ == '__main__':
    main()
