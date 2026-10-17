import http from 'node:http';
import { connectorTokenDigest } from '../identity/connector.js';
import { toolRegistry } from '../tools/registry.js';

export class StreamableHttpTransport {
  async listen(options: { httpPath: string; healthPort: number }): Promise<void> {
    const server = http.createServer((req, res) => {
      if (req.url === options.httpPath) {
        const auth = req.headers.authorization ?? '';
        if (!auth.startsWith('Bearer ')) {
          res.writeHead(401, { 'content-type': 'application/json' });
          res.end(JSON.stringify({ error: 'connector bearer required' }));
          return;
        }
        const token = auth.slice('Bearer '.length).trim();
        const digest = connectorTokenDigest(token).toString('hex');
        res.writeHead(200, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ tools: toolRegistry, tokenDigestPreview: digest.slice(0, 8) }));
        return;
      }
      if (req.url === '/health') {
        res.writeHead(200, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ status: 'starting' }));
        return;
      }
      res.writeHead(404).end();
    });
    await new Promise<void>((resolve) => server.listen(options.healthPort, resolve));
  }
}
