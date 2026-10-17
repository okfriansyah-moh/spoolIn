import { createGateway } from './gateway/index.js';

const healthPort = Number(process.env.MCP_HEALTH_PORT ?? '8788');

const gateway = createGateway();
await gateway.start({
  httpPath: '/mcp',
  healthPort,
});
