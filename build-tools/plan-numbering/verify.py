"""Audit a plan identity migration against its recorded Git source."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import yaml


def document(text):
    _, header, body = text.split('---', 2)
    return yaml.safe_load(header), body


def evidence(text):
    match = re.search(r'^## Evidence\n.*?(?=^## |\Z)', text, re.M | re.S)
    return match.group(0) if match else ''


def source(root, revision, path):
    return subprocess.check_output(['git', 'show', f'{revision}:{path}'], cwd=root).decode()


def task_ids(rows):
    old = [r['old_id'] for r in rows]
    new = [r['new_id'] for r in rows]
    if len(old) != len(set(old)) or len(new) != len(set(new)):
        raise ValueError('Task mapping must be one-to-one')
    expected = [f'T{n:03}' for n in range(1, len(rows) + 1)]
    if new != expected or set(old) != set(expected):
        raise ValueError('Mapping must cover consecutive source and target task IDs')
    return dict(zip(old, new))


def task_record(root, revision, row, mapping):
    before, old_body = document(source(root, revision, row['old_path']))
    after, new_body = document((root / row['new_path']).read_text())
    expected_deps = [mapping[d] for d in before['depends_on']]
    expected = dict(before, id=row['new_id'], group=row['new_group'], depends_on=expected_deps)
    if before['id'] != row['old_id'] or before['group'] != row['old_group']:
        raise ValueError('Source identity differs from mapping')
    if after != expected or row['status_before'] != before['status']:
        raise ValueError('Metadata or completion status changed beyond numbering')
    if row['depends_on_before'] != before['depends_on'] or row['depends_on_after'] != expected_deps:
        raise ValueError('Mapped dependencies changed')
    if any(dep >= row['new_id'] for dep in expected_deps):
        raise ValueError('Prerequisite does not precede its consumer')
    if evidence(old_body) != evidence(new_body):
        raise ValueError('Historical Evidence section changed')
    digest = hashlib.sha256(evidence(old_body).encode()).hexdigest()
    if digest != row['evidence_sha256']:
        raise ValueError('Historical Evidence digest differs from mapping')
    navigation = re.search(r'\[Feature group\]\(([^)]+)\)', new_body)
    expected_group = (root / row['new_path']).parent.parent / 'group.md'
    if not navigation or ((root / row['new_path']).parent / navigation[1]).resolve() != expected_group.resolve():
        raise ValueError('Task navigation does not resolve to its own stage')
    return before, after


def coverage(body):
    rows = {}
    for line in body.splitlines():
        if not line.startswith('| '):
            continue
        cells = [c.strip() for c in line.strip('|').split('|')]
        if len(cells) == 7 and cells[0] not in ['Obligation', '---']:
            rows[cells[0]] = cells
    return rows


def coverage_preserved(before, after, mapping):
    expected = {}
    for key, cells in coverage(before).items():
        cells = cells.copy()
        for i in [4, 5]:
            cells[i] = re.sub(r'\bT\d{3}\b', lambda m: mapping[m[0]], cells[i])
        expected[key] = cells
    if coverage(after) != expected:
        raise ValueError('Obligation rows or implementation/verification owners changed')
    return len(expected)


def group_records(root, meta, rows):
    expected = [f'G{n:02}' for n in range(1, len(meta['groups']) + 1)]
    if [g['id'] for g in meta['groups']] != expected:
        raise ValueError('Group numbering is not consecutive')
    ordered = []
    for number, group in enumerate(meta['groups'], 1):
        path = root / 'docs' / group['path']
        gm, _ = document(path.read_text())
        if gm['id'] != group['id'] or not path.parent.name.startswith(f'{number:02}-'):
            raise ValueError('Folder and group identities disagree')
        members = [r['new_id'] for r in rows if r['new_group'] == gm['id']]
        if gm['tasks'] != members or gm['integration_owner'] not in members:
            raise ValueError('Stage members or integration owner changed')
        ordered.extend(members)
    if ordered != [r['new_id'] for r in rows]:
        raise ValueError('Stage task ranges are not contiguous')


def fixture_names_for_owner(tree, owner, pattern):
    count = 0
    for path in tree.rglob('*.kt'):
        names = re.findall(r'(?:OwnerFixture|CryptoFixture)\(\s*"(t\d{3}[^"\n]+\.json)"', path.read_text())
        for name in names:
            sample = re.sub(r'\$(?:\{[^}]+\}|[A-Za-z_]\w*)', 'fixture', name)
            if not re.fullmatch(pattern, sample):
                raise ValueError(f'Fixture producer {name} cannot pass {owner} cleanup')
            count += 1
    return count


def fixture_names(root):
    """Check fixture task prefixes against each owner's cleanup boundary."""
    observed = 0
    for owner in ['core', 'catalog', 'media']:
        tree = root / f'services/{owner}/src/integrationTest/kotlin'
        target = tree / f'com/spoolin/{owner}/boundary/bootstrap/OwnerFixture.kt'
        if not target.is_file():
            continue
        match = re.search(r'evidenceName\.matches\(Regex\("([^"\n]+)"\)\)', target.read_text())
        if not match:
            raise ValueError('Owner fixture cleanup pattern is missing')
        pattern = match[1].replace('\\\\', '\\')
        observed += fixture_names_for_owner(tree, owner, pattern)
    return observed


def verify(root, mapping_path):
    data = json.loads(mapping_path.read_text())
    if data['schema_version'] != 1 or data['target_numbering'] != 'sequential':
        raise ValueError('Unsupported migration format')
    rows = data['tasks']
    mapping = task_ids(rows)
    old_meta, old_body = document(source(root, data['source_revision'], data['source_plan']))
    new_meta, new_body = document((root / data['source_plan']).read_text())
    if new_meta.get('numbering') != 'sequential':
        raise ValueError('Target plan must explicitly opt into sequential numbering')
    old_paths = subprocess.check_output(
        ['git', 'ls-tree', '-r', '--name-only', data['source_revision'], 'docs/plan'], cwd=root, text=True,
    ).splitlines()
    if {r['old_path'] for r in rows} != {p for p in old_paths if '/tasks/' in p and p.endswith('.md')}:
        raise ValueError('Source task coverage is incomplete')
    actual = {str(p.relative_to(root)) for p in (root / 'docs/plan').glob('*/tasks/*.md')}
    if actual != {r['new_path'] for r in rows}:
        raise ValueError('Target task coverage is incomplete')
    states = {}
    for row in rows:
        _, after = task_record(root, data['source_revision'], row, mapping)
        states[after['status']] = states.get(after['status'], 0) + 1
    group_records(root, new_meta, rows)
    count = coverage_preserved(old_body, new_body, mapping)
    if old_meta['sources'] != new_meta['sources']:
        raise ValueError('Requirement source identities changed')
    return {'tasks': len(rows), 'groups': len(new_meta['groups']), 'obligations': count, 'statuses': states, 'fixture_producers': fixture_names(root)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=Path.cwd())
    args = parser.parse_args()
    root = args.repo.resolve()
    print(json.dumps(verify(root, root / 'docs/plan-numbering-v1.json'), indent=2))


if __name__ == '__main__':
    main()
