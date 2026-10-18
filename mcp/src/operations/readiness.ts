import { readFileSync } from 'node:fs';
import { Client, credentials, Metadata } from '@grpc/grpc-js';
import { BinaryReader, BinaryWriter } from '@bufbuild/protobuf/wire';

export interface ReadinessProbe { check(): Promise<boolean>; close(): void }

/** Standard grpc.health.v1.Check, with the same verified mTLS identity as gateway requests. */
export class CoreReadiness implements ReadinessProbe {
  private readonly client: Client | undefined;
  private checking: Promise<boolean> | undefined;
  private checkedAt = 0;
  private healthy = false;
  private stopped = false;
  constructor(read: (name: string) => string | undefined = (name) => process.env[name]) {
    const target = read('CORE_GRPC_TARGET'), ca = read('GRPC_CA_FILE'), key = read('GRPC_CLIENT_KEY_FILE'), cert = read('GRPC_CLIENT_CERT_FILE');
    if (target && ca && key && cert) this.client = new Client(target, credentials.createSsl(
      readFileSync(ca), readFileSync(key), readFileSync(cert)), { 'grpc.enable_retries': 0, 'grpc.max_receive_message_length': 1024 });
  }
  check(): Promise<boolean> {
    if (this.stopped || !this.client) return Promise.resolve(false);
    if (this.checking) return this.checking;
    if (Date.now() - this.checkedAt < 5000) return Promise.resolve(this.healthy);
    this.checking = Promise.all(['spoolin.v1.IdentityService', 'spoolin.v1.McpGatewayService'].map((service) => this.service(service)))
      .then((values) => { this.healthy = !this.stopped && values.every(Boolean); this.checkedAt = Date.now(); return this.healthy; })
      .finally(() => { this.checking = undefined; });
    return this.checking;
  }
  private service(service: string): Promise<boolean> {
    const request = Buffer.from(new BinaryWriter().uint32(10).string(service).finish());
    return new Promise((resolve) => this.client!.makeUnaryRequest('/grpc.health.v1.Health/Check',
      (value: Buffer) => value, (value: Buffer) => value, request, new Metadata(),
      { deadline: new Date(Date.now() + 1800) }, (error, value: Buffer | undefined) => {
        if (error || !value) { resolve(false); return; }
        try { resolve(serving(value)); } catch { resolve(false); }
      }));
  }
  close(): void { this.stopped = true; this.healthy = false; this.client?.close(); }
}

function serving(value: Uint8Array): boolean {
  const reader = new BinaryReader(value);
  let status = 0;
  while (reader.pos < reader.len) {
    const tag = reader.uint32();
    if (tag === 8) status = reader.int32(); else reader.skip(tag & 7);
  }
  return status === 1;
}
