import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { Server, ServerCredentials, status } from '@grpc/grpc-js';
import type { ServerUnaryCall, sendUnaryData } from '@grpc/grpc-js';
import { McpGatewayServiceService, Proposal, ProposalRevision } from '../../src/generated/spoolin/v1/spoolin.js';
import type { ApprovalInput, MovieContextRequest, MovieContext, Outcome, PolicySnapshot, Id } from '../../src/generated/spoolin/v1/spoolin.js';
import { GatewayClient } from '../../src/operations/gateway-client.js';

// Real gRPC/mTLS boundary with an isolated synthetic Core service; not application acceptance.
describe('typed Core gateway over real mutually authenticated gRPC', () => {
  let directory: string;
  let server: Server;
  let client: GatewayClient;
  const observed: string[] = [];
  const policy: PolicySnapshot = { revision: 'a'.repeat(64), textBounds: [] };
  const savedEnvironment = new Map<string, string | undefined>();
  function certificate(name: string, serverCertificate = false) {
    const file = (suffix: string) => join(directory, `${name}.${suffix}`);
    execFileSync('openssl', ['req', '-newkey', 'rsa:2048', '-nodes', '-keyout', file('key'), '-out', file('csr'), '-subj', `/CN=${name === 'server' ? 'localhost' : name}`], { stdio: 'ignore' });
    writeFileSync(file('ext'), serverCertificate ? 'subjectAltName=DNS:localhost\nextendedKeyUsage=serverAuth\n' : 'extendedKeyUsage=clientAuth\n');
    execFileSync('openssl', ['x509', '-req', '-in', file('csr'), '-CA', join(directory, 'ca.crt'), '-CAkey', join(directory, 'ca.key'), '-CAcreateserial', '-out', file('crt'), '-days', '1', '-extfile', file('ext')], { stdio: 'ignore' });
  }
  function check(call: ServerUnaryCall<unknown, unknown>) {
    expect(call.metadata.get('authorization')).toEqual(['Bearer isolated-connector']);
    observed.push(String(call.metadata.get('mcp-ui-assertion')[0] ?? 'ordinary'));
  }
  beforeAll(async () => {
    directory = mkdtempSync(join(tmpdir(), 'spoolin-grpc-'));
    execFileSync('openssl', ['req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-keyout', join(directory, 'ca.key'), '-out', join(directory, 'ca.crt'), '-subj', '/CN=SpoolIn synthetic test CA', '-days', '1'], { stdio: 'ignore' });
    certificate('server', true); certificate('client');
    server = new Server();
    const operation = (call: ServerUnaryCall<ApprovalInput, Outcome>, callback: sendUnaryData<Outcome>) => {
      check(call); callback(null, { operationId: call.request.proposalId, state: 'SUCCEEDED', resultExpired: false });
    };
    server.addService(McpGatewayServiceService, {
      getPolicy: (call: ServerUnaryCall<object, PolicySnapshot>, callback: sendUnaryData<PolicySnapshot>) => { check(call); callback(null, policy); },
      getMovieContext: (call: ServerUnaryCall<MovieContextRequest, MovieContext>, callback: sendUnaryData<MovieContext>) => { check(call); callback(null, { policy, titles: { nodes: [], hasNextPage: false } }); },
      proposeMovie: (call: ServerUnaryCall<Proposal, ProposalRevision>, callback: sendUnaryData<ProposalRevision>) => { check(call); callback(null, ProposalRevision.fromPartial({ proposalId: call.request.proposalId, revision: 1, contentHash: 'b'.repeat(64) })); },
      getMovieOperation: (call: ServerUnaryCall<Id, Outcome>, callback: sendUnaryData<Outcome>) => {
        check(call);
        if (call.request.id === 'failure') callback({ code: status.UNAVAILABLE, message: 'sensitive upstream diagnostic' });
        else callback(null, { operationId: call.request.id, state: 'SUCCEEDED', resultExpired: false });
      },
      approveMovie: operation, rejectMovie: operation,
    });
    const tls = ServerCredentials.createSsl(readFileSync(join(directory, 'ca.crt')), [{ private_key: readFileSync(join(directory, 'server.key')), cert_chain: readFileSync(join(directory, 'server.crt')) }], true);
    const port = await new Promise<number>((resolve, reject) => server.bindAsync('localhost:0', tls, (error, value) => error ? reject(error) : resolve(value)));
    const values = { CORE_GRPC_TARGET: `localhost:${port}`, GRPC_CA_FILE: join(directory, 'ca.crt'), GRPC_CLIENT_KEY_FILE: join(directory, 'client.key'), GRPC_CLIENT_CERT_FILE: join(directory, 'client.crt') };
    for (const [key, value] of Object.entries(values)) { savedEnvironment.set(key, process.env[key]); process.env[key] = value; }
    client = new GatewayClient('isolated-connector');
  });
  afterAll(async () => {
    client?.close();
    if (server) await new Promise<void>((resolve) => server.tryShutdown(() => resolve()));
    for (const [key, value] of savedEnvironment) { if (value === undefined) delete process.env[key]; else process.env[key] = value; }
    if (directory) rmSync(directory, { recursive: true });
  });
  it('uses generated serializers and credential metadata for every permitted RPC', async () => {
    expect((await client.policy()).revision).toBe(policy.revision);
    expect((await client.context('movie', 3)).policy?.revision).toBe(policy.revision);
    expect((await client.propose(Proposal.fromPartial({ proposalId: 'proposal' }))).proposalId).toBe('proposal');
    expect((await client.operation('operation')).state).toBe('SUCCEEDED');
    const input: ApprovalInput = { proposalId: 'proposal', revision: 1, contentHash: 'hash', challenge: 'private' };
    expect((await client.approve(input, 'private-signed-assertion')).state).toBe('SUCCEEDED');
    expect((await client.reject(input, 'private-rejection-assertion')).state).toBe('SUCCEEDED');
    expect(observed).toContain('private-signed-assertion');
    expect(observed).toContain('private-rejection-assertion');
  });
  it('redacts downstream diagnostics on failed RPCs', async () => {
    await expect(client.operation('failure')).rejects.toMatchObject({ problem: { code: 'UNAVAILABLE', message: 'Try again when the service is available.' } });
  });
});
