#!/usr/bin/env python3
"""Protect the reviewed real-world seed from obsolete synthetic generation."""
import sys


def main():
    print('Synthetic seed generation is retired. Use collect_public_pages.py for explicit '
          'HTML preparation, review the cached data, then validate_manifest.py.', file=sys.stderr)
    return 2


if __name__ == '__main__':
    raise SystemExit(main())
