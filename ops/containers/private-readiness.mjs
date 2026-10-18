import { readFileSync } from 'node:fs';
import { request as httpsRequest } from 'node:https';
import { pathToFileURL } from 'node:url';

const OWNERS = new Set(['core', 'catalog', 'media']);
const STATUSES = new Set(['UP', 'DOWN', 'OUT_OF_SERVICE', 'UNKNOWN']);
const COMPONENTS = new Set(['readinessState', 'schema', 'rateReadiness', 'db']);
const MAXIMUM_BYTES = 65536;
const CONNECTION_CODES = new Set(['ECONNREFUSED', 'ENOTFOUND', 'CERT_HAS_EXPIRED', 'ERR_TLS_CERT_ALTNAME_INVALID',
  'DEPTH_ZERO_SELF_SIGNED_CERT', 'UNABLE_TO_VERIFY_LEAF_SIGNATURE', 'SELF_SIGNED_CERT_IN_CHAIN',
  'ECONNRESET', 'ETIMEDOUT', 'ERR_SSL_TLSV13_ALERT_CERTIFICATE_REQUIRED']);

export function summary(service, body) {
  const result = { service, probe: 'https-readiness', boundary: 'private-network-mtls', result: 'FAIL' };
  try {
    const value = JSON.parse(body);
    result.observed = STATUSES.has(value?.status) ? value.status : 'unavailable';
    result.components = {};
    for (const [key, component] of Object.entries(value?.components ?? {})) {
      if (COMPONENTS.has(key)) result.components[key] = STATUSES.has(component?.status) ? component.status : 'unavailable';
    }
    if (value?.status === 'UP') result.result = 'PASS';
  } catch {
    result.reason = 'invalid-health-json';
  }
  return result;
}

export function probe(service, send = httpsRequest, read = readFileSync) {
  const failure = (reason) => ({ service: OWNERS.has(service) ? service : 'unavailable', probe: 'https-readiness',
    boundary: 'private-network-mtls', result: 'FAIL', reason });
  if (!OWNERS.has(service)) return Promise.resolve(failure('unsupported-owner'));
  return new Promise((resolve) => {
    let complete = false;
    let timer;
    let request;
    const finish = (value) => {
      if (complete) return;
      complete = true;
      clearTimeout(timer);
      resolve(value);
    };
    try {
      request = send({ hostname: service, servername: service, port: 8081, method: 'GET',
        path: '/actuator/health/readiness', rejectUnauthorized: true, minVersion: 'TLSv1.2',
        ca: read('/run/secrets/service-ca'), cert: read('/run/secrets/mcp-cert'), key: read('/run/secrets/mcp-key') },
      (response) => consume(response, service, finish, () => request.destroy()));
      request.on('error', (error) => finish({ ...failure('trusted-health-connection-failed'),
        connectionCode: CONNECTION_CODES.has(error.code) ? error.code : 'TLS_CONNECTION_FAILED' }));
      timer = setTimeout(() => { finish(failure('trusted-health-deadline')); request.destroy(); }, 2000);
      request.end();
    } catch {
      finish(failure('trusted-health-configuration-failed'));
    }
  });
}

function consume(response, service, finish, abort) {
  const failure = (reason) => ({ service, probe: 'https-readiness', boundary: 'private-network-mtls', result: 'FAIL', reason });
  let bytes = 0;
  const chunks = [];
  response.on('data', (chunk) => {
    bytes += chunk.length;
    if (bytes > MAXIMUM_BYTES) {
      finish(failure('health-response-too-large'));
      abort();
    } else chunks.push(chunk);
  });
  response.on('aborted', () => finish(failure('health-response-interrupted')));
  response.on('error', () => finish(failure('health-response-interrupted')));
  response.on('end', () => finish(response.statusCode === 200
    ? summary(service, Buffer.concat(chunks).toString('utf8')) : failure('health-http-status-failed')));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const result = await probe(process.argv.length === 3 ? process.argv[2] : undefined);
  process.stdout.write(`${JSON.stringify(result)}\n`);
  process.exitCode = result.result === 'PASS' ? 0 : 1;
}
