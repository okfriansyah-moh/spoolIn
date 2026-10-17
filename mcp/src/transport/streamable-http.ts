import http from 'node:http';

export class StreamableHttpTransport {
  async listen(options: { httpPath: string; healthPort: number }): Promise<void> {
    const server = http.createServer((req, res) => {
      if (req.url === options.httpPath) {
        res.writeHead(501, { 'content-type': 'application/json' });
        res.end(JSON.stringify({ error: 'MCP adapter awaiting connector binding' }));
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
