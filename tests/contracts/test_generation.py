from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import subprocess
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / '.ai/evidence/numbering-v1/t002-generation'


class GeneratedCompatibility(unittest.TestCase):
    def command(self, argv, label):
        EVIDENCE.mkdir(parents=True, exist_ok=True)
        started = datetime.now(timezone.utc).isoformat()
        result = subprocess.run(argv, cwd=ROOT, capture_output=True, timeout=600)
        log = EVIDENCE / (label + '.txt')
        log.write_bytes(result.stdout + result.stderr)
        metadata = {'argv': argv, 'started_at_utc': started,
                    'finished_at_utc': datetime.now(timezone.utc).isoformat(),
                    'exit_code': result.returncode,
                    'log_sha256': hashlib.sha256(log.read_bytes()).hexdigest()}
        EVIDENCE.joinpath(label + '.command.json').write_text(json.dumps(metadata, indent=2) + '\n')
        self.assertEqual(result.returncode, 0, log.read_text(errors='replace')[-7000:])

    def test_actual_java_and_typescript_consumers_and_packaged_sdl(self):
        self.command(['node', 'build-tools/generate-contracts.mjs', '--force'], 'generate')
        self.command(['./gradlew', ':contracts:grpc:runContractConsumer', ':contracts:grpc:jar', ':services:core:jar',
                      '--no-daemon', '--console', 'plain'], 'jvm-consumers')
        self.command(['node', 'node_modules/typescript/bin/tsc', '--strict', '--module', 'NodeNext',
                      '--moduleResolution', 'NodeNext', '--target', 'ES2024', '--types', 'node',
                      '--rootDir', '.', '--outDir', 'build/contract-consumers/ts',
                      'tests/contracts/consumer.ts'], 'typescript-consumers')
        self.command(['node', 'build/contract-consumers/ts/tests/contracts/consumer.js'], 'typescript-roundtrip')
        original = ROOT.joinpath('contracts/graphql/schema.graphql').read_bytes()
        resource = ROOT.joinpath('services/core/build/resources/main/graphql/schema.graphql')
        self.assertEqual(resource.read_bytes(), original)
        jars = list(ROOT.joinpath('services/core/build/libs').glob('*-plain.jar'))
        self.assertEqual(len(jars), 1)
        with zipfile.ZipFile(jars[0]) as archive:
            self.assertEqual(archive.read('graphql/schema.graphql'), original)
        self.assertTrue(ROOT.joinpath('contracts/grpc/build/descriptors/spoolin.pb').stat().st_size)

    def test_00_clean_kotlin_compile_generates_dtos_first(self):
        self.command(['./gradlew', ':services:core:clean', ':services:core:compileKotlin',
                      '--no-daemon', '--console', 'plain'], 'clean-kotlin-order')
        dto = ROOT / 'services/core/build/generated/sources/graphql/com/spoolin/core/boundary/graphql/generated/MoviePatch.java'
        self.assertTrue(dto.is_file(), 'Kotlin compilation must wait for its generated Java DTO sources')

    def test_generation_is_reproducible_and_repairs_missing_outputs(self):
        self.command(['node', 'build-tools/generate-contracts.mjs', '--force'], 'repeat-generate-first')
        first = json.loads(ROOT.joinpath('build/contract-generation.json').read_text())['outputs']
        victim = ROOT / 'frontend/src/lib/generated/graphql.ts'
        victim.unlink()
        imported_proto = ROOT / 'mcp/src/generated/google/protobuf/timestamp.ts'
        imported_proto.unlink()
        self.command(['node', 'build-tools/generate-contracts.mjs'], 'repair-generate')
        second = json.loads(ROOT.joinpath('build/contract-generation.json').read_text())['outputs']
        self.assertEqual(first, second)
        self.assertTrue(victim.is_file())
        self.assertTrue(imported_proto.is_file())

    def test_design_tokens_resolve_both_consumers_and_themes(self):
        self.command(['node', 'build-tools/generate-tokens.mjs'], 'tokens')
        css = ROOT.joinpath('frontend/src/lib/generated/tokens.css').read_text()
        self.assertEqual(css, ROOT.joinpath('mcp/src/generated/tokens.css').read_text())
        self.assertIn('--bg: #101820;', css)
        self.assertIn('--bg: #f5f3ef;', css)
        self.assertIn('--accent: #8fd8ee;', css)
        self.assertIn('--accent: #643f67;', css)
        self.assertIn('[data-theme="light"]', css)
        self.assertNotIn('{primitive.', css)
        self.assertNotIn('{semantic.', css)
