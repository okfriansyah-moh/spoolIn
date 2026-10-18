#!/usr/bin/env python3
"""Update only the prepared local Maintainer password files during offline recovery."""
import os
import sys

from provision import BASE, LOCAL_DEMO_PASSWORDS

PASSWORD = BASE / 'runtime' / 'maintainer-password'
BACKUP = BASE / 'runtime' / 'maintainer-password.before-repair'
ACCOUNTS = BASE / 'accounts.txt'


def prepare():
    owner = PASSWORD.stat()
    with BACKUP.open('xb') as backup:
        os.chmod(BACKUP, 0o600)
        backup.write(PASSWORD.read_bytes())
    os.chown(BACKUP, owner.st_uid, owner.st_gid)
    PASSWORD.write_text(LOCAL_DEMO_PASSWORDS['maintainer'])


def complete():
    lines = ACCOUNTS.read_text().splitlines()
    email = 'maintainer@example.invalid'
    if sum(line.startswith(email + ': ') for line in lines) != 1:
        raise RuntimeError('Local credentials file has no unique Maintainer entry; recovery backup retained.')
    updated = [f'{email}: {LOCAL_DEMO_PASSWORDS["maintainer"]}' if line.startswith(email + ': ') else line
               for line in lines]
    ACCOUNTS.write_text('\n'.join(updated) + '\n')
    BACKUP.unlink()


def main():
    for path in (BASE, BASE / 'runtime', PASSWORD, BACKUP, ACCOUNTS):
        if path.is_symlink():
            raise RuntimeError('Local credential recovery refuses symbolic links.')
    action = sys.argv[1]
    if action == 'prepare':
        prepare()
    elif action == 'rollback':
        PASSWORD.write_bytes(BACKUP.read_bytes())
        BACKUP.unlink()
    elif action == 'complete':
        complete()
    else:
        raise RuntimeError('Unsupported local credential recovery phase.')


if __name__ == '__main__':
    main()
