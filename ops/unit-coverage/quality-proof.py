"""Connect Docker unit and Sonar reports to the installed shared quality contract."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
CONFIG = json.loads((ROOT / '.ai/config.json').read_text())
sys.path.insert(0, str(ROOT / '.ai' / CONFIG['bundle']))
from agent_workflow import gate_reports, gate_selection, quality

OUTPUT = ROOT / '.local/unit-coverage'


def read(path):
    return json.loads(path.read_text())


def write(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + '\n')


def capture(registry):
    """Called after clearing reports and before executing any test producer."""
    value = {'schema_version': 1, 'measured_at': datetime.now(timezone.utc).isoformat(),
             'revision': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
             'components': {}}
    for contract in registry['quality']['components']:
        value['components'][contract['id']] = {
            'inputs': quality.snapshot(ROOT, contract['inputs']),
            'toolchain_files': quality.snapshot(ROOT, contract['toolchain_files']),
            'commands': contract['commands'],
            'contract_sha256': gate_selection.digest(contract),
        }
    write(OUTPUT / 'producer-inputs.json', value)


def check_inputs(registry, captured):
    contracts = registry['quality']['components']
    if set(captured['components']) != {contract['id'] for contract in contracts}:
        raise ValueError('Measurement component inventory changed')
    for contract in contracts:
        item = captured['components'][contract['id']]
        if item['commands'] != contract['commands'] or item['contract_sha256'] != gate_selection.digest(contract):
            raise ValueError('Measurement contract changed')
        for field in ('inputs', 'toolchain_files'):
            current = quality.snapshot(ROOT, contract[field])
            if item[field] != current:
                changed = sorted(path for path in set(item[field]) | set(current) if item[field].get(path) != current.get(path))
                raise ValueError(f"Measurement inputs changed during unit execution: {contract['id']} {field}: {changed}")


def finish(registry):
    captured = read(OUTPUT / 'producer-inputs.json')
    check_inputs(registry, captured)
    for contract in registry['quality']['components']:
        item = dict(captured['components'][contract['id']])
        paths = quality.report_paths(ROOT, contract['unit_report']['reports'])
        paths += quality.report_paths(ROOT, [contract['test_report']['glob']])
        item.update(input_sha256=gate_selection.digest(item['inputs']), revision=captured['revision'], measured_at=captured['measured_at'],
                    finished_at=datetime.now(timezone.utc).isoformat(),
                    unit_reports=gate_selection.hash_paths(ROOT, [p.relative_to(ROOT).as_posix() for p in paths]))
        write(OUTPUT / contract['id'] / 'producer.json', item)


def component_proof(contract, captured, summary, sonar, base):
    item = dict(captured['components'][contract['id']])
    # Retained reports without a measurement-time input receipt stay blocked.
    for field in ('inputs', 'toolchain_files'):
        if item[field] != quality.snapshot(ROOT, contract[field]):
            raise ValueError(f"Inputs changed after measurement: {contract['id']} {field}")
    native = next(v for v in summary['components'] if v['component'] == contract['id'])
    analysis = next(v for v in sonar['components'] if v['component'] == contract['id'])
    if native['command'] != 0 or native['status'] != 'passed' or analysis['status'] != 'passed':
        raise ValueError('Unit producer or Sonar verifier failed')
    if analysis.get('executionStatus') != 'SUCCESS':
        raise ValueError('Missing completed Sonar execution proof')
    analyzer = {'status': analysis['executionStatus'], 'gate': analysis['gate'],
                'analysis_id': analysis['analysisId'],
                'input_sha256': analysis['input_sha256'], 'producer_sha256': analysis['producer_sha256'],
                'lines': {'covered': analysis['covered'], 'total': analysis['total']},
                'conditions': {k: analysis['conditions'][k] for k in ('covered', 'total')},
                **{key: analysis['quality'][source] for key, source in (
                    ('security_issues', 'securityIssues'), ('reliability_issues', 'reliabilityIssues'),
                    ('duplicated_lines', 'duplicatedLines'), ('duplicated_blocks', 'duplicatedBlocks'))},
                'classifications': analysis['classifications'],
                'classified_issue_ids': analysis['classified_issue_ids']}
    analyzer_path = OUTPUT / contract['id'] / 'analyzer-proof.json'
    write(analyzer_path, analyzer)
    unit = contract['unit_report']
    paths = quality.report_paths(ROOT, unit['reports'])
    tests = quality.report_paths(ROOT, [contract['test_report']['glob']])
    lines = gate_reports.coverage_lines(paths, unit['format'], unit.get('source_prefix', ''))
    production = quality.snapshot(ROOT, contract['production'])
    production = sorted(p for p in production if not gate_selection.matches(p, contract.get('exclude', [])))
    # Never manufacture report entries for omitted production files.
    if set(production) - set(lines):
        raise ValueError(f"Production files absent from unit report: {sorted(set(production) - set(lines))}")
    changed = gate_reports.measure_coverage(ROOT, gate_reports.changed_lines(ROOT, base, contract['roots']), lines, contract.get('exclude', []))
    item.update(measured_at=captured['measured_at'], revision=captured['revision'],
                production_files=production, execution=summary['execution'],
                producer_report=f".local/unit-coverage/{contract['id']}/producer.json",
                unit={'kind': 'unit', 'lines': {'covered': sum(sum(lines[p].values()) for p in production),
                                               'total': sum(len(lines[p]) for p in production)},
                      'tests': gate_reports.test_summary(tests, contract['test_report']['format'])},
                changed={'base': base, 'covered': changed['covered'], 'total': changed['executable'], 'missing_files': changed['missing_files']},
                analyzer=analyzer, analyzer_report=analyzer_path.relative_to(ROOT).as_posix())
    artifacts = [p.relative_to(ROOT).as_posix() for p in paths + tests + [analyzer_path]]
    artifacts += [item['producer_report'], '.local/unit-coverage/summary.json', '.local/unit-coverage/sonar-results.json', '.local/unit-coverage/producer-inputs.json']
    item['reports'] = gate_selection.hash_paths(ROOT, artifacts)
    quality.validate_component(ROOT, contract, item, base)
    return item


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=('capture', 'finish', 'check-inputs', 'verify'))
    parser.add_argument('--base')
    args = parser.parse_args()
    registry = read(ROOT / '.ai/gates.json')
    OUTPUT.mkdir(parents=True, exist_ok=True)
    if args.action == 'capture':
        capture(registry)
        return
    if args.action == 'check-inputs':
        check_inputs(registry, read(OUTPUT / 'producer-inputs.json'))
        print('Measurement source, tests, configuration and toolchain inventories match.')
        return
    if args.action == 'finish':
        finish(registry)
        return
    if not args.base:
        raise ValueError('A reviewed comparison base is required')
    target = OUTPUT / 'quality-proof.json'
    target.unlink(missing_ok=True)
    captured = read(OUTPUT / 'producer-inputs.json')
    summary, sonar = (read(OUTPUT / name) for name in ('summary.json', 'sonar-results.json'))
    if summary['fingerprint'] != sonar['fingerprint'] or summary['revision'] != captured['revision']:
        raise ValueError('Sonar and unit measurements differ')
    proof = {'schema_version': 1, 'components': {}}
    for contract in registry['quality']['components']:
        proof['components'][contract['id']] = component_proof(contract, captured, summary, sonar, args.base)
    write(target, proof)
    results = quality.results(ROOT, registry, args.base)
    write(OUTPUT / 'quality-result.json', results)
    if any(v['status'] != 'passed' for v in results):
        raise ValueError('Shared quality verification blocked')
    print('All six component quality contracts passed; broader required checks remain mandatory.')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, StopIteration) as error:
        OUTPUT.mkdir(parents=True, exist_ok=True)
        write(OUTPUT / 'quality-result.json', {'status': 'blocked', 'reason': str(error)})
        print(f'Quality verification blocked: {error}', file=sys.stderr)
        sys.exit(1)
