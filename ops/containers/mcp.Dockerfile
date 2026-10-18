FROM node:24.21.0-bookworm-slim@sha256:d6aa754f16b3197301076f047b5def2f02ea1dbbc2ca920407d46d7ec7f87b20
WORKDIR /app
COPY package.json package-lock.json ./
COPY frontend/package.json frontend/package.json
COPY tests/browser/package.json tests/browser/package.json
COPY mcp/package.json mcp/package.json
RUN npm install --global npm@11.19.0 && npm ci --omit=dev --ignore-scripts --workspace mcp
COPY mcp/dist mcp/dist
COPY ops/containers/private-readiness.mjs /app/private-readiness.mjs
USER 10001:10001
ENTRYPOINT ["node", "mcp/dist/server.js"]
