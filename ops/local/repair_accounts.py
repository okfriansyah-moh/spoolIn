#!/usr/bin/env python3
"""Update prepared local demo password files during offline recovery."""
import os
import sys

from provision import BASE, LOCAL_DEMO_PASSWORDS

ACCOUNTS = BASE / 'accounts.txt'


def password(role):
    return BASE / 'runtime' / f'{role}-password'


def backup(role):
    return BASE / 'runtime' / f'{role}-password.before-repair'


def rollback(roles):
    for role in roles:
        if backup(role).exists():
            password(role).write_bytes(backup(role).read_bytes())
            backup(role).unlink()


def prepare(roles):
    if any(backup(role).exists() for role in roles):
        raise RuntimeError('A previous local credential recovery needs completion; backups retained.')
    created = []
    try:
        for role in roles:
            owner = password(role).stat()
            original = password(role).read_bytes()
            with backup(role).open('xb') as destination:
                created.append(backup(role))
                os.chmod(backup(role), 0o600)
                destination.write(original)
            os.chown(backup(role), owner.st_uid, owner.st_gid)
    except Exception:
        # No password has changed yet; incomplete backups must never be restored.
        for path in created:
            path.unlink(missing_ok=True)
        raise
    try:
        for role in roles:
            password(role).write_text(LOCAL_DEMO_PASSWORDS[role])
    except Exception:
        rollback(roles)
        raise


def complete(roles):
    lines = ACCOUNTS.read_text().splitlines()
    replacements = {f'{role}@example.invalid': LOCAL_DEMO_PASSWORDS[role] for role in roles}
    if any(sum(line.startswith(email + ': ') for line in lines) != 1 for email in replacements):
        raise RuntimeError('Local credentials file lacks unique prepared accounts; recovery backups retained.')
    updated = [f'{line.partition(": ")[0]}: {replacements[line.partition(": ")[0]]}'
               if line.partition(': ')[0] in replacements else line for line in lines]
    ACCOUNTS.write_text('\n'.join(updated) + '\n')
    for role in roles:
        backup(role).unlink()


def main():
    scope = sys.argv[2]
    if scope not in ('maintainer', 'accounts'):
        raise RuntimeError('Unsupported local credential recovery scope.')
    roles = ('viewer', 'maintainer') if scope == 'accounts' else ('maintainer',)
    paths = [BASE, BASE / 'runtime', ACCOUNTS]
    paths.extend(path for role in roles for path in (password(role), backup(role)))
    if any(path.is_symlink() for path in paths):
        raise RuntimeError('Local credential recovery refuses symbolic links.')
    action = sys.argv[1]
    if action not in ('prepare', 'rollback', 'complete'):
        raise RuntimeError('Unsupported local credential recovery phase.')
    {'prepare': prepare, 'rollback': rollback, 'complete': complete}[action](roles)


if __name__ == '__main__':
    main()
