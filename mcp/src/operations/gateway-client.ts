import { readFileSync } from 'node:fs';
import { credentials, Metadata } from '@grpc/grpc-js';
import { McpGatewayServiceClient } from '../generated/spoolin/v1/spoolin.js';
import type { ApprovalInput, MovieContext, Outcome, PolicySnapshot, Proposal, ProposalRevision } from '../generated/spoolin/v1/spoolin.js';

/** The only backend connection is the authenticated, typed Core gRPC service. */
export class GatewayClient {
  private readonly client: McpGatewayServiceClient;
  constructor(private readonly connectorToken: string) {
    const required = (name: string): string => {
      const value = process.env[name];
      if (!value) throw new Error(`Missing configuration: ${name}`);
      return value;
    };
    this.client = new McpGatewayServiceClient(required('CORE_GRPC_TARGET'), credentials.createSsl(
      readFileSync(required('GRPC_CA_FILE')), readFileSync(required('GRPC_CLIENT_KEY_FILE')),
      readFileSync(required('GRPC_CLIENT_CERT_FILE')),
    ));
  }
  ready(): Promise<void> {
    return new Promise((resolve, reject) => this.client.waitForReady(new Date(Date.now() + 60_000),
      (error) => error ? reject(new Error('Core gateway unavailable')) : resolve()));
  }
  private call<T>(invoke: (metadata: Metadata, options: { deadline: Date }, callback: (error: Error | null, value: T) => void) => unknown, assertion?: string): Promise<T> {
    const metadata = new Metadata();
    metadata.set('authorization', `Bearer ${this.connectorToken}`);
    if (assertion) metadata.set('mcp-ui-assertion', assertion);
    return new Promise((resolve, reject) => invoke(metadata, { deadline: new Date(Date.now() + 5000) },
      (error, value) => error ? reject(new Error('Core gateway request failed')) : resolve(value)));
  }
  policy(): Promise<PolicySnapshot> { return this.call((m, o, cb) => this.client.getPolicy({}, m, o, cb)); }
  context(query: string, limit: number): Promise<MovieContext> { return this.call((m, o, cb) => this.client.getMovieContext({ query, limit }, m, o, cb)); }
  propose(request: Proposal): Promise<ProposalRevision> { return this.call((m, o, cb) => this.client.proposeMovie(request, m, o, cb)); }
  operation(id: string): Promise<Outcome> { return this.call((m, o, cb) => this.client.getMovieOperation({ id }, m, o, cb)); }
  approve(request: ApprovalInput, assertion: string): Promise<Outcome> { return this.call((m, o, cb) => this.client.approveMovie(request, m, o, cb), assertion); }
  reject(request: ApprovalInput, assertion: string): Promise<Outcome> { return this.call((m, o, cb) => this.client.rejectMovie(request, m, o, cb), assertion); }
  close(): void { this.client.close(); }
}
