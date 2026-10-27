"""Optional monitoring configuration for the existing local installation."""
import copy
import json
import os

SERVICES = ('prometheus', 'alertmanager', 'grafana', 'loki', 'alloy', 'docker-log-proxy',
            'cadvisor', 'postgres-exporter')


def configured_port(value):
    if not value.isdecimal() or not 1024 <= int(value) <= 65535:
        raise RuntimeError('GRAFANA_HTTPS_PORT must be a port from 1024 through 65535.')
    return int(value)


def configure(document, deployment, root, state, port):
    for name in SERVICES[:3]:
        document['services'][name] = copy.deepcopy(deployment['services'][name])
        document['services'][name].pop('depends_on', None)
    extension = json.loads((root / 'ops/monitoring/local-services.json').read_text())
    for service in extension['services'].values():
        if 'build' in service:
            service['build']['context'] = str(root)
        for volume in service.get('volumes', []):
            if isinstance(volume, dict) and volume.get('type') == 'bind':
                volume['source'] = str(root / volume['source'])
    document['services'].update(extension['services'])
    document.setdefault('volumes', {}).update(extension['volumes'])
    document.setdefault('networks', {}).update(extension['networks'])
    document['secrets']['postgres-monitor-password'] = {'file': str(state / 'identities/postgres-monitor-password')}
    configuration_files(root, state)
    grafana = document['services']['grafana']
    grafana['ports'] = [{'target': 3000, 'published': str(port), 'host_ip': '127.0.0.1', 'protocol': 'tcp'}]
    grafana['healthcheck'] = {'test': ['CMD-SHELL', 'wget --no-check-certificate -q -T 3 -O /dev/null https://127.0.0.1:3000/api/health'],
                              'interval': '10s', 'timeout': '4s', 'retries': 18}
    grafana['volumes'].append({'type': 'bind', 'source': str(root / 'ops/monitoring/grafana/local'),
                               'target': '/etc/grafana/dashboards/local', 'read_only': True})
    grafana['volumes'].append({'type': 'bind', 'source': str(root / 'ops/monitoring/local-datasource.yaml'),
                               'target': '/etc/grafana/provisioning/datasources/local.yaml', 'read_only': True})
    for name in ('prometheus', 'alertmanager', 'grafana'):
        document['services'][name]['networks'] = {'private': None, 'telemetry': None}
    prometheus = document['services']['prometheus']
    prometheus['volumes'].extend([
        {'type': 'bind', 'source': str(state / 'monitoring-prometheus.yaml'),
         'target': '/etc/prometheus/local.yaml', 'read_only': True},
        {'type': 'bind', 'source': str(root / 'ops/monitoring/local-alerts.yml'),
         'target': '/etc/prometheus/local-alerts.yml', 'read_only': True}])
    document['services']['alertmanager']['volumes'] = [
        volume for volume in document['services']['alertmanager']['volumes']
        if volume.get('target') != '/etc/alertmanager/alertmanager.yaml']
    document['services']['alertmanager']['volumes'].append(
        {'type': 'bind', 'source': str(state / 'monitoring-alertmanager.yaml'),
         'target': '/etc/alertmanager/alertmanager.yaml', 'read_only': True})
    prometheus['volumes'].append({'type': 'bind', 'source': str(root / 'ops/monitoring/local-scrapes.yaml'),
                                'target': '/etc/prometheus/local-scrapes.yaml', 'read_only': True})
    prometheus['command'][0] = '--config.file=/etc/prometheus/local.yaml'
    for service in document['services'].values():
        service['logging'] = {'driver': 'json-file', 'options': {'max-size': '10m', 'max-file': '3'}}
    return document


def port_for(state):
    saved = state / 'monitoring-port'
    return configured_port(os.environ.get('GRAFANA_HTTPS_PORT', saved.read_text().strip() if saved.exists() else '3000'))


def configuration_files(root, state):
    source = root / 'ops/monitoring'
    prometheus = (source / 'prometheus.yaml').read_text()
    prometheus = 'scrape_config_files: [/etc/prometheus/local-scrapes.yaml]\n' + prometheus
    prometheus = prometheus.replace('rule_files:\n', 'rule_files:\n  - /etc/prometheus/local-alerts.yml\n')
    alertmanager = (source / 'alertmanager.yaml').read_text()
    alertmanager = alertmanager.replace('route:\n', 'route:\n  routes:\n    - receiver: local-infrastructure\n      matchers: [\'class="infrastructure"\']\n')
    alertmanager = alertmanager.replace('receivers:\n', 'receivers:\n  - name: local-infrastructure\n')
    for name, content in (('prometheus', prometheus), ('alertmanager', alertmanager)):
        path = state / ('monitoring-' + name + '.yaml')
        path.write_text(content)
        path.chmod(0o644)
