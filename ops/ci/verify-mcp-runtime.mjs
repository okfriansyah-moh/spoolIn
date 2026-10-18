import { randomUUID } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import { Client } from '@modelcontextprotocol/client';
import { StdioClientTransport } from '@modelcontextprotocol/client/stdio';

function requireProof(condition, description) {
  if (!condition) throw new Error(description);
}

function publicResult(reply, sensitive) {
  const challenge = reply._meta?.ui?.challenge;
  if (typeof challenge === 'string' && challenge) sensitive.push(challenge);
  requireProof(!reply.isError && reply.structuredContent, 'Expected successful structured MCP result');
  const visible = JSON.stringify({ content: reply.content, structuredContent: reply.structuredContent });
  requireProof(sensitive.every((value) => !value || !visible.includes(value)), 'Private material reached model-visible output');
  return reply.structuredContent;
}

async function connect(tokenFile) {
  const transport = new StdioClientTransport({
    command: process.execPath,
    args: ['mcp/dist/transport/stdio.js'],
    env: { ...process.env, MCP_CONNECTOR_TOKEN_FILE: tokenFile,
      MCP_ENDPOINT: `http://127.0.0.1:${process.env.MCP_HTTP_PORT ?? '8787'}/mcp` },
    stderr: 'pipe',
  });
  let diagnostics = '';
  transport.stderr?.on('data', (chunk) => {
    diagnostics = (diagnostics + chunk.toString()).slice(-16_384);
  });
  const client = new Client({ name: 'remote-protocol-acceptance', version: '1.0.0' });
  try {
    await client.connect(transport);
    return { client, close: async () => {
      await client.close();
      requireProof(!diagnostics.includes(readFileSync(tokenFile, 'utf8').trim()), 'Credential reached bridge diagnostics');
    } };
  } catch {
    await transport.close();
    throw new Error('Actual production MCP connection was denied or unavailable');
  }
}

async function publicationDenied(client) {
  let denied = false;
  try { await client.callTool({ name: 'approve_movie', arguments: {} }); } catch { denied = true; }
  requireProof(denied, 'Model invocation of disabled publication was accepted');
}

async function propose(client, context, sensitive) {
  const operationId = randomUUID();
  const argumentsValue = { operationId, title: `A Remote Protocol ${randomUUID().slice(0, 8)}`,
    synopsis: 'An original synthetic synopsis for an isolated protocol boundary check.',
    genreIds: [context.genres[0].id], release: { year: 2026 }, synopsisLanguage: 'en',
    sources: [{ field: 'synopsis', url: 'https://www.wikidata.org/wiki/Q11621',
      reuseBasis: 'CC0 structured facts; original synthetic test wording.' }] };
  const reply = await client.callTool({ name: 'propose_movie', arguments: argumentsValue });
  const challenge = reply._meta?.ui?.challenge;
  requireProof(typeof challenge === 'string' && challenge.length >= 32, 'Private UI challenge missing');
  sensitive.push(challenge);
  const first = publicResult(reply, sensitive);
  requireProof(first.revision === 1 && first.status === 'REVIEW', 'Initial immutable review revision missing');
  requireProof(first.normalizedMovie.title === argumentsValue.title, 'Proposal normalization changed title');
  const repeated = publicResult(await client.callTool({ name: 'propose_movie', arguments: argumentsValue }), sensitive);
  requireProof(repeated.proposalId === first.proposalId && repeated.contentHash === first.contentHash,
    'Compatible replay changed immutable proposal identity');
  const resource = await client.readResource({ uri: first.reviewResourceUri });
  const content = JSON.stringify(resource);
  requireProof(sensitive.every((value) => !content.includes(value)), 'Private material reached readable proposal resource');
  const revision = publicResult(await client.callTool({ name: 'propose_movie', arguments: {
    ...argumentsValue, operationId: randomUUID(), proposalId: first.proposalId,
    expectedRevision: first.revision, title: `${argumentsValue.title} revised`,
  } }), sensitive);
  requireProof(revision.revision === 2 && revision.proposalId === first.proposalId && revision.contentHash !== first.contentHash,
    'Correction did not create a new exact-content revision');
  const receipt = publicResult(await client.callTool({ name: 'movie_operation', arguments: { operationId } }), sensitive);
  requireProof(receipt.operationId === operationId && receipt.state === 'PENDING', 'Original publish receipt is not pending');
  await publicationDenied(client);
  return { operationId, proposalId: first.proposalId, revision: revision.revision, result: 'passed' };
}

async function positive(tokenFile) {
  const sensitive = [readFileSync(tokenFile, 'utf8').trim()];
  const manifest = JSON.parse(readFileSync('data/seed/manifest.json', 'utf8'));
  const seed = manifest.records[0];
  const session = await connect(tokenFile);
  let proposal;
  try {
    const tools = await session.client.listTools();
    const names = tools.tools.map((tool) => tool.name);
    requireProof(['movie_context', 'propose_movie', 'movie_operation'].every((name) => names.includes(name)), 'Production tools missing');
    requireProof(!names.includes('approve_movie') && !names.includes('reject_movie'), 'Unsupported host publication exposed');
    const context = publicResult(await session.client.callTool({ name: 'movie_context',
      arguments: { query: seed.normalized.title, limit: 20 } }), sensitive);
    requireProof(context.titles.some((title) => title.id === seed.stableUUID), 'Real seeded catalog result missing');
    requireProof(context.genres.length > 0 && context.roles.length >= 4 && /^[a-f0-9]{64}$/.test(context.policyRevision),
      'Actual taxonomy or effective policy missing');
    proposal = await propose(session.client, context, sensitive);
  } finally { await session.close(); }
  const next = await connect(tokenFile);
  try {
    const outcome = publicResult(await next.client.callTool({ name: 'movie_operation',
      arguments: { operationId: proposal.operationId } }), sensitive);
    requireProof(outcome.state === 'PENDING', 'Durable publish receipt missing after actual stdio reconnect');
  } finally { await next.close(); }
  return { result: 'passed', boundary: 'real SDK stdio→production HTTP MCP→mTLS Core→Catalog→PostgreSQL',
    checks: ['seeded context', 'taxonomy/policy', 'proposal/replay/revision', 'model/resource privacy',
      'direct publication denial', 'owner receipt', 'stdio reconnect'],
    publication: 'pending required supported host provenance and actual host approval/rejection', ...proposal };
}

async function main() {
  requireProof(process.env.GITHUB_ACTIONS === 'true' && process.env.RUNNER_ENVIRONMENT === 'github-hosted', 'Hosted CI required');
  const [mode, tokenFile, output] = process.argv.slice(2);
  requireProof(['positive', 'revoked'].includes(mode) && tokenFile && output, 'Explicit mode and protected inputs required');
  const report = { testedSha: process.env.TESTED_SHA, runId: process.env.GITHUB_RUN_ID, result: 'failed' };
  let status = 1;
  try {
    if (mode === 'positive') Object.assign(report, await positive(tokenFile));
    else {
      let denied = false;
      try { const session = await connect(tokenFile); await session.close(); } catch { denied = true; }
      requireProof(denied, 'Revoked connector still initialized a production session');
      Object.assign(report, { result: 'passed', checks: ['actual revoked connector initialization denial'] });
    }
    status = 0;
  } catch {
    report.failure = 'Actual protocol boundary assertion failed; private transport details withheld';
  } finally {
    writeFileSync(output, JSON.stringify(report, null, 2) + '\n');
  }
  process.exitCode = status;
}
await main();
