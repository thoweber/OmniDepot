#!/usr/bin/env node

/**
 * OmniDepot ArchUnit Checker MCP Server
 * Model Context Protocol (MCP) server wrapping ArchUnit boundary verification via Maven.
 */

const readline = require('readline');
const { spawn } = require('child_process');
const path = require('path');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

function sendResponse(response) {
  process.stdout.write(JSON.stringify(response) + '\n');
}

function sendError(id, code, message) {
  sendResponse({
    jsonrpc: '2.0',
    id: id,
    error: {
      code: code,
      message: message
    }
  });
}

function logDebug(msg) {
  process.stderr.write(`[archunitChecker MCP] ${msg}\n`);
}

logDebug('Server started, listening on STDIN');

rl.on('line', (line) => {
  line = line.trim();
  if (!line) return;

  let request;
  try {
    request = JSON.parse(line);
  } catch (err) {
    logDebug(`Failed to parse JSON: ${err.message}`);
    return;
  }

  const { jsonrpc, id, method, params } = request;

  if (id === undefined || id === null) {
    logDebug(`Received notification: ${method}`);
    return;
  }

  logDebug(`Received request: ${method} (id: ${id})`);

  switch (method) {
    case 'initialize':
      sendResponse({
        jsonrpc: '2.0',
        id: id,
        result: {
          protocolVersion: '2024-11-05',
          capabilities: {
            tools: {}
          },
          serverInfo: {
            name: 'archunitChecker',
            version: '1.0.0'
          }
        }
      });
      break;

    case 'ping':
      sendResponse({
        jsonrpc: '2.0',
        id: id,
        result: {}
      });
      break;

    case 'tools/list':
      sendResponse({
        jsonrpc: '2.0',
        id: id,
        result: {
          tools: [
            {
              name: 'check_architecture_boundaries',
              description: 'Verifies package visibility, Hexagonal layer boundaries, and CDI scopes via ArchUnit tests',
              inputSchema: {
                type: 'object',
                properties: {
                  testName: {
                    type: 'string',
                    description: 'ArchUnit test class name to execute (default: ArchitectureBoundaryTest)'
                  }
                }
              }
            }
          ]
        }
      });
      break;

    case 'tools/call':
      handleToolCall(id, params);
      break;

    default:
      sendError(id, -32601, `Method not found: ${method}`);
      break;
  }
});

function handleToolCall(id, params) {
  const toolName = params?.name;
  const args = params?.arguments || {};

  if (toolName !== 'check_architecture_boundaries') {
    sendResponse({
      jsonrpc: '2.0',
      id: id,
      result: {
        content: [{ type: 'text', text: `Unknown tool: ${toolName}` }],
        isError: true
      }
    });
    return;
  }

  const testName = args.testName || 'ArchitectureBoundaryTest';
  const projectRoot = process.cwd();
  const mvnwPath = path.join(projectRoot, 'mvnw');

  logDebug(`Running ArchUnit verification: ${testName}`);

  const child = spawn(mvnwPath, ['test', `-Dtest=${testName}`], {
    cwd: projectRoot,
    env: process.env
  });

  let stdoutData = '';
  let stderrData = '';

  child.stdout.on('data', (data) => {
    stdoutData += data.toString();
  });

  child.stderr.on('data', (data) => {
    stderrData += data.toString();
  });

  child.on('close', (code) => {
    logDebug(`ArchUnit verification finished with exit code ${code}`);
    const fullOutput = stdoutData + (stderrData ? `\n--- STDERR ---\n${stderrData}` : '');
    sendResponse({
      jsonrpc: '2.0',
      id: id,
      result: {
        content: [
          {
            type: 'text',
            text: fullOutput
          }
        ],
        isError: code !== 0
      }
    });
  });

  child.on('error', (err) => {
    logDebug(`Failed to execute mvnw: ${err.message}`);
    sendResponse({
      jsonrpc: '2.0',
      id: id,
      result: {
        content: [
          {
            type: 'text',
            text: `Execution failed: ${err.message}`
          }
        ],
        isError: true
      }
    });
  });
}
