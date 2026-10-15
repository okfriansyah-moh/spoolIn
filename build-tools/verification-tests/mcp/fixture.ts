import type * as Client from '@modelcontextprotocol/client';
import type * as Core from '@modelcontextprotocol/core';
import type * as Apps from '@modelcontextprotocol/ext-apps';
import type * as NodeTransport from '@modelcontextprotocol/node';
import type * as Server from '@modelcontextprotocol/server';

// Compile the selected SDK graph without starting a connector or protocol process.
export type SelectedSdkGraph = {
  client: typeof Client;
  core: typeof Core;
  apps: typeof Apps;
  node: typeof NodeTransport;
  server: typeof Server;
};
