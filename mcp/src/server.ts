import { createGateway } from './gateway/index.js';
import { runMcpProcess } from './operations/diagnostics.js';
import { initializeTracing, shutdownTracing } from './operations/tracing.js';

await runMcpProcess(async (register) => {
  initializeTracing();
  const gateway = createGateway();
  register(async () => {
    try {
      await gateway.close();
    } finally {
      await shutdownTracing();
    }
  });
  await gateway.start({
    httpPath: '/mcp',
    healthPort: Number(process.env.MCP_HEALTH_PORT ?? '8788'),
  });
});
