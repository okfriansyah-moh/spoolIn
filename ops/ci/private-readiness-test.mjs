import { strict as assert } from 'node:assert';
import { EventEmitter } from 'node:events';
import { test } from 'node:test';
import { probe, summary } from '../containers/private-readiness.mjs';

if (process.env.GITHUB_ACTIONS !== 'true' || process.env.RUNNER_ENVIRONMENT !== 'github-hosted') {
  throw new Error('Requires GitHub-hosted remote execution');
}

function transport(body, statusCode = 200, size = undefined) {
  let options;
  const send = (value, callback) => {
    options = value;
    const request = new EventEmitter();
    request.destroy = () => request.emit('error', new Error('private-key-canary'));
    request.end = () => queueMicrotask(() => {
      const response = new EventEmitter();
      response.statusCode = statusCode;
      callback(response);
      response.emit('data', Buffer.from(size ? 'x'.repeat(size) : body));
      response.emit('end');
    });
    return request;
  };
  return { send, options: () => options };
}

test('private readiness keeps TLS validation and fixed owner path while projecting no private body fields', async () => {
  const channel = transport(JSON.stringify({ status: 'UP', private: 'private-body-canary',
    components: { schema: { status: 'UP', details: 'private-body-canary' } } }));
  const result = await probe('catalog', channel.send, () => Buffer.from('private-key-canary'));
  assert.equal(result.result, 'PASS');
  assert.deepEqual(result.components, { schema: 'UP' });
  assert.equal(channel.options().rejectUnauthorized, true);
  assert.equal(channel.options().hostname, 'catalog');
  assert.equal(channel.options().servername, 'catalog');
  assert.equal(channel.options().port, 8081);
  assert.equal(channel.options().path, '/actuator/health/readiness');
  assert(!JSON.stringify(result).includes('private-body-canary'));
  assert(!JSON.stringify(result).includes('private-key-canary'));
});

test('private readiness fails malformed responses, non200, unknown owners and oversized bodies', async () => {
  for (const channel of [transport('not-json'), transport('{"status":"DOWN"}'),
    transport('{"status":"UP"}', 403), transport('', 200, 65537)]) {
    assert.equal((await probe('core', channel.send, () => Buffer.from('test'))).result, 'FAIL');
  }
  assert.equal((await probe('https://other.invalid')).reason, 'unsupported-owner');
});

test('configuration and connection failures expose only fixed safe reasons', async () => {
  const result = await probe('media', () => { throw new Error('private-credential-canary'); }, () => Buffer.from('test'));
  assert.equal(result.result, 'FAIL');
  assert(!JSON.stringify(result).includes('credential-canary'));
  assert.equal(summary('core', '{"status":"UP","components":{"secret":{"status":"UP"}}}').components.secret, undefined);
});
