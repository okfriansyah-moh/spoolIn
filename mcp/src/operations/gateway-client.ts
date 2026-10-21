import { activeCancellation, mcpTracing, traceparent } from './tracing.js';
import type { GatewayOperation } from './tracing.js';
import type { ClientUnaryCall } from '@grpc/grpc-js';
import { GatewayFailure } from './gateway-failure.js';
import { readFileSync } from 'node:fs';
import { credentials, Metadata } from '@grpc/grpc-js';
import { McpGatewayServiceClient } from '../generated/spoolin/v1/spoolin.js';
import type {
  Actor,
  ApprovalInput,
  MovieContext,
  Outcome,
  PolicySnapshot,
  Proposal,
  ProposalRevision,
  PrepareMoviePosterRequest,
  PreparedPoster,
  MoviePosterReadRequest,
  MoviePosterImage,
  MovieProposalQueueRequest,
  MovieProposalQueue,
} from '../generated/spoolin/v1/spoolin.js';

type Invocation<T> = (
  metadata: Metadata,
  options: { deadline: Date },
  callback: (error: Error | null, value: T) => void,
) => ClientUnaryCall;
function unary<T>(invoke: Invocation<T>, metadata: Metadata, timeout = 5000): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const signal = activeCancellation();
    let finished = false;
    const cancel = () => active?.cancel();
    const active = invoke(
      metadata,
      { deadline: new Date(Date.now() + timeout) },
      (error, value) => {
        if (finished) return;
        finished = true;
        signal?.removeEventListener('abort', cancel);
        if (error) reject(GatewayFailure.fromGrpc(error));
        else resolve(value);
      },
    );
    if (!finished) signal?.addEventListener('abort', cancel, { once: true });
    if (signal?.aborted) cancel();
  });
}

/** The only backend connection is the authenticated, typed Core gRPC service. */
export class GatewayClient {
  private readonly client: McpGatewayServiceClient;
  constructor(private readonly connectorToken: string) {
    const required = (name: string): string => {
      const value = process.env[name];
      if (!value) throw new Error(`Missing configuration: ${name}`);
      return value;
    };
    this.client = new McpGatewayServiceClient(
      required('CORE_GRPC_TARGET'),
      credentials.createSsl(
        readFileSync(required('GRPC_CA_FILE')),
        readFileSync(required('GRPC_CLIENT_KEY_FILE')),
        readFileSync(required('GRPC_CLIENT_CERT_FILE')),
      ),
      { 'grpc.max_send_message_length': 17 * 1024 * 1024, 'grpc.max_receive_message_length': 17 * 1024 * 1024 },
    );
  }
  ready(): Promise<void> {
    return new Promise((resolve, reject) =>
      this.client.waitForReady(new Date(Date.now() + 60_000), (error) =>
        error ? reject(new Error('Core gateway unavailable')) : resolve(),
      ),
    );
  }
  private call<T>(
    operation: GatewayOperation,
    invoke: (
      metadata: Metadata,
      options: { deadline: Date },
      callback: (error: Error | null, value: T) => void,
    ) => ClientUnaryCall,
    assertion?: string,
  ): Promise<T> {
    return mcpTracing.gateway(operation, (span) => {
      const metadata = new Metadata();
      metadata.set('authorization', `Bearer ${this.connectorToken}`);
      metadata.set('traceparent', traceparent(span));
      if (assertion) metadata.set('mcp-ui-assertion', assertion);
      return unary(invoke, metadata, operation === 'PrepareMoviePoster' ? 25_000 : operation === 'ReadMoviePoster' ? 3000 : 5000);
    });
  }

  reviewIdentity(): Promise<Actor> {
    return this.call('GetReviewIdentity', (m, o, cb) => this.client.getReviewIdentity({}, m, o, cb));
  }
  policy(): Promise<PolicySnapshot> {
    return this.call('GetPolicy', (m, o, cb) =>
      this.client.getPolicy({}, m, o, cb),
    );
  }
  context(query: string, limit: number): Promise<MovieContext> {
    return this.call('GetMovieContext', (m, o, cb) =>
      this.client.getMovieContext({ query, limit }, m, o, cb),
    );
  }
  reviewQueue(request: MovieProposalQueueRequest): Promise<MovieProposalQueue> {
    return this.call('ListMovieProposals', (m, o, cb) => this.client.listMovieProposals(request, m, o, cb));
  }
  propose(request: Proposal): Promise<ProposalRevision> {
    return this.call('ProposeMovie', (m, o, cb) =>
      this.client.proposeMovie(request, m, o, cb),
    );
  }
  preparePoster(request: PrepareMoviePosterRequest): Promise<PreparedPoster> {
    return this.call('PrepareMoviePoster', (m, o, cb) =>
      this.client.prepareMoviePoster(request, m, o, cb),
    );
  }
  readPoster(request: MoviePosterReadRequest): Promise<MoviePosterImage> {
    return this.call('ReadMoviePoster', (m, o, cb) =>
      this.client.readMoviePoster(request, m, o, cb),
    );
  }
  operation(id: string): Promise<Outcome> {
    return this.call('GetMovieOperation', (m, o, cb) =>
      this.client.getMovieOperation({ id }, m, o, cb),
    );
  }
  approve(request: ApprovalInput, assertion: string): Promise<Outcome> {
    return this.call(
      'ApproveMovie',
      (m, o, cb) => this.client.approveMovie(request, m, o, cb),
      assertion,
    );
  }
  reject(request: ApprovalInput, assertion: string): Promise<Outcome> {
    return this.call(
      'RejectMovie',
      (m, o, cb) => this.client.rejectMovie(request, m, o, cb),
      assertion,
    );
  }
  close(): void {
    this.client.close();
  }
}
