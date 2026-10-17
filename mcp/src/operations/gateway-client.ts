import { bearerAuthorization } from '../identity/connector.js';

export type GatewayClientOptions = Readonly<{
  baseUrl: string;
  connectorToken: string;
}>;

/** HTTP façade over Core McpGatewayService (transport validates bearer digest server-side). */
export class GatewayClient {
  constructor(private readonly options: GatewayClientOptions) {}

  private headers(): HeadersInit {
    return {
      authorization: bearerAuthorization(this.options.connectorToken),
      'content-type': 'application/json',
    };
  }

  async postJson<T>(path: string, body: unknown): Promise<T> {
    const response = await fetch(`${this.options.baseUrl}${path}`, {
      method: 'POST',
      headers: this.headers(),
      body: JSON.stringify(body),
    });
    if (!response.ok) {
      throw new Error(`gateway ${path} failed: ${response.status}`);
    }
    return (await response.json()) as T;
  }
}
