from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[2]


class WireRules(unittest.TestCase):
    def node(self, program):
        result = subprocess.run(['node', '--input-type=module', '-e', program], cwd=ROOT, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stderr.decode())

    def test_long_rejects_signed_overflow_without_narrowing(self):
        program = """
import assert from 'node:assert/strict';
import {longString} from './build-tools/contracts/wire.mjs';
assert.equal(longString('9007199254740993'), '9007199254740993');
assert.equal(longString('9223372036854775807'), '9223372036854775807');
assert.throws(() => longString('9223372036854775808'), /range/);
assert.throws(() => longString('-9223372036854775809'), /range/);
"""
        result = subprocess.run(['node', '--input-type=module', '-e', program], cwd=ROOT, capture_output=True)
        self.assertEqual(result.returncode, 0, result.stderr.decode())

    def test_nonrepresentable_int_and_numeric_long_reject(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {int32,longString} from './build-tools/contracts/wire.mjs';
assert.equal(int32(2147483647),2147483647);
assert.throws(()=>int32(2147483648),/range/);
assert.throws(()=>int32(1.5),/range/);
assert.throws(()=>longString(9007199254740993),/string/);
assert.equal(longString('-9223372036854775808'),'-9223372036854775808');
""")

    def test_uuid_utc_and_timestamp_microseconds_are_strict(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {uuid,dateTime,truncateTimestamp} from './build-tools/contracts/wire.mjs';
assert.equal(uuid('190db10d-02b8-437e-8e7c-ece9c105fe2a'),'190db10d-02b8-437e-8e7c-ece9c105fe2a');
for(const value of ['190DB10D-02B8-437E-8E7C-ECE9C105FE2A','190db10d02b8437e8e7cece9c105fe2a','wrong']) assert.throws(()=>uuid(value),/UUID/);
assert.equal(dateTime('2026-10-12T03:00:00.123456Z'),'2026-10-12T03:00:00.123456Z');
for(const value of ['2026-10-12T03:00:00.123456789Z','2026-10-12T03:00:00+00:00','2025-02-29T00:00:00Z']) assert.throws(()=>dateTime(value));
assert.deepEqual(truncateTimestamp({seconds:'1791774000',nanos:123456789}),{seconds:'1791774000',nanos:123456000});
""")

    def test_standard_base64_and_url_challenge_have_distinct_encodings(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {base64,approvalChallenge} from './build-tools/contracts/wire.mjs';
assert.equal(base64('Zg=='),'Zg==');
for(const value of ['Zg','Zg=','Zg===','Zg==\n','data:text/plain;base64,Zg==','Zh==','____']) assert.throws(()=>base64(value));
const challenge=Buffer.alloc(32,255).toString('base64url');
assert.equal(approvalChallenge(challenge),challenge);
assert.throws(()=>base64(challenge));
assert.throws(()=>approvalChallenge(challenge+'='));
assert.throws(()=>approvalChallenge(challenge.slice(0,-1)+'_'));
""")

    def test_timestamp_seconds_include_endpoints_and_reject_adjacent_values(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {truncateTimestamp} from './build-tools/contracts/wire.mjs';
for (const seconds of ['-62135596800','253402300799']) {
  assert.deepEqual(truncateTimestamp({seconds,nanos:999999999}),{seconds,nanos:999999000});
  assert.deepEqual(truncateTimestamp({seconds,nanos:0}),{seconds,nanos:0});
}
for (const seconds of ['-62135596801','253402300800']) {
  assert.throws(()=>truncateTimestamp({seconds,nanos:0}),/timestamp seconds/i);
}
""")

    def test_ordinary_text_uses_unicode_white_space_without_normalizing(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {ordinaryText} from './build-tools/contracts/wire.mjs';
const points=[9,10,11,12,13,32,133,160,5760,8192,8193,8194,8195,8196,8197,8198,8199,8200,8201,8202,8232,8233,8239,8287,12288];
for (const point of points) {
  const space=String.fromCodePoint(point);
  assert.equal(ordinaryText(space+'A  e\u0301\nB'+space),'A  e\u0301\nB');
  assert.equal(ordinaryText(space),'');
}
assert.equal(ordinaryText('\uFEFF'),'\uFEFF');
assert.throws(()=>ordinaryText('A\u0000B'),/NUL/);
assert.throws(()=>ordinaryText('\ud800'),/Unicode/);
""")

    def test_request_and_result_envelopes_are_independent_utf8_bounds(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {envelope} from './build-tools/contracts/wire.mjs';
assert.equal(Buffer.byteLength(envelope('a'.repeat(131070))),131072);
assert.equal(Buffer.byteLength(envelope('b'.repeat(131070))),131072);
assert.throws(()=>envelope('a'.repeat(131071)),/Envelope/);
assert.throws(()=>envelope('b'.repeat(131071)),/Envelope/);
assert.throws(()=>envelope('💬'.repeat(32768)),/Envelope/);
""")

    def test_unsupported_json_values_are_not_dropped_or_replaced(self):
        self.node(r"""
import assert from 'node:assert/strict';
import {envelope} from './build-tools/contracts/wire.mjs';
for (const value of [{field:undefined},{field:NaN},{field:Infinity},{field:'\ud800'},[undefined]]) assert.throws(()=>envelope(value));
""")


if __name__ == '__main__':
    unittest.main(verbosity=2)
