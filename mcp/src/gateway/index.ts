import { StreamableHttpTransport } from '../transport/streamable-http.js';

export type GatewayOptions = Readonly<{
  httpPath: string;
  healthPort: number;
}>;

export function createGateway() {
  const transport = new StreamableHttpTransport();
  return {
    async start(options: GatewayOptions): Promise<void> {
      await transport.listen(options);
    },
  };
}
