import { GatewayClient } from '../operations/gateway-client.js';
import { StreamableHttpTransport } from '../transport/streamable-http.js';

export type GatewayOptions = Readonly<{
  httpPath: string;
  healthPort: number;
}>;

export function createGateway() {
  const transport = new StreamableHttpTransport();
  return {
    close: () => transport.close(),
    async start(options: GatewayOptions): Promise<void> {
      const probe = new GatewayClient('');
      try { await probe.ready(); } finally { probe.close(); }
      await transport.listen(options);
    },
  };
}
