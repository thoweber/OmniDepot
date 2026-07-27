#!/usr/bin/env node

/**
 * omnidepot Liquibase Validator MCP Server
 * Model Context Protocol (MCP) server wrapping dual-dialect Liquibase update and rollback testing
 * for H2 embedded database and PostgreSQL 16+.
 */

const readline = require('readline');
const { spawn } = require('child_process');
const path = require('path');
const net = require('net');

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
  process.stderr.write(`[liquibaseValidator MCP] ${msg}\n`);
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
            name: 'liquibaseValidator',
            version: '1.1.0'
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
              name: 'validate_liquibase_changelog',
              description: 'Validates Liquibase XML changelogs against H2 and PostgreSQL databases by executing Maven update and testing rollback cycles',
              inputSchema: {
                type: 'object',
                properties: {
                  module: {
                    type: 'string',
                    description: 'Maven reactor module to validate (default: omnidepot-infra-db)'
                  },
                  dialects: {
                    type: 'array',
                    items: { type: 'string' },
                    description: 'Target database dialects to validate (options: ["h2", "postgresql"], default: ["h2", "postgresql"])'
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

const DIALECT_CONFIGS = {
  h2: {
    name: 'h2',
    description: 'Embedded H2 Memory Database',
    driver: 'org.h2.Driver',
    url: 'jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1',
    username: 'sa',
    password: ''
  },
  postgresql: {
    name: 'postgresql',
    description: 'PostgreSQL 16+',
    driver: 'org.postgresql.Driver',
    url: 'jdbc:postgresql://localhost:5432/omnidepot',
    username: 'omnidepot',
    password: 'omnidepot_secret',
    port: 5432
  }
};

function handleToolCall(id, params) {
  const toolName = params?.name;
  const args = params?.arguments || {};

  if (toolName !== 'validate_liquibase_changelog') {
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

  const module = args.module || 'omnidepot-infra-db';
  let requestedDialects = args.dialects || ['h2', 'postgresql'];
  if (typeof requestedDialects === 'string') {
    requestedDialects = [requestedDialects];
  }

  const selectedDialects = requestedDialects
    .map(d => d.toLowerCase().trim())
    .filter(d => DIALECT_CONFIGS[d]);

  if (selectedDialects.length === 0) {
    selectedDialects.push('h2', 'postgresql');
  }

  logDebug(`Starting validation for module '${module}' across dialects: ${selectedDialects.join(', ')}`);

  runDialectValidationsSequentially(module, selectedDialects, 0, [], (results) => {
    let combinedOutput = '';
    let hasFailure = false;

    for (const res of results) {
      combinedOutput += `===================================================\n`;
      combinedOutput += `=== DIALECT VALIDATION: ${res.dialect.toUpperCase()} (${res.config.description}) ===\n`;
      combinedOutput += `===================================================\n`;
      combinedOutput += res.output.trim() + '\n\n';
      if (res.exitCode !== 0) {
        hasFailure = true;
      }
    }

    sendResponse({
      jsonrpc: '2.0',
      id: id,
      result: {
        content: [
          {
            type: 'text',
            text: combinedOutput.trim()
          }
        ],
        isError: hasFailure
      }
    });
  });
}

function runDialectValidationsSequentially(module, dialects, index, results, onComplete) {
  if (index >= dialects.length) {
    onComplete(results);
    return;
  }

  const dialectKey = dialects[index];
  const config = DIALECT_CONFIGS[dialectKey];

  if (dialectKey === 'postgresql') {
    ensurePostgresAvailable((ready, errorMsg) => {
      if (!ready) {
        logDebug(`PostgreSQL not available: ${errorMsg}`);
        results.push({
          dialect: dialectKey,
          config: config,
          exitCode: 1,
          output: `PostgreSQL connection failed on localhost:${config.port}.\nError: ${errorMsg}\nPlease ensure postgres container is running via 'docker compose up -d postgres'.`
        });
        runDialectValidationsSequentially(module, dialects, index + 1, results, onComplete);
        return;
      }

      executeMavenForDialect(module, config, (res) => {
        results.push({
          dialect: dialectKey,
          config: config,
          exitCode: res.exitCode,
          output: res.output
        });
        runDialectValidationsSequentially(module, dialects, index + 1, results, onComplete);
      });
    });
  } else {
    executeMavenForDialect(module, config, (res) => {
      results.push({
        dialect: dialectKey,
        config: config,
        exitCode: res.exitCode,
        output: res.output
      });
      runDialectValidationsSequentially(module, dialects, index + 1, results, onComplete);
    });
  }
}

function ensurePostgresAvailable(callback) {
  logDebug('Checking TCP port 5432 for PostgreSQL...');
  const socket = net.connect({ port: 5432, host: 'localhost' }, () => {
    socket.destroy();
    logDebug('PostgreSQL port 5432 is open.');
    callback(true, null);
  });

  socket.on('error', (err) => {
    logDebug(`TCP port 5432 connect error: ${err.message}. Attempting docker container start...`);
    const child = spawn('docker', ['compose', 'up', '-d', 'postgres'], {
      cwd: process.cwd(),
      env: process.env
    });

    child.on('close', (code) => {
      if (code !== 0) {
        callback(false, `docker compose exit code ${code}`);
        return;
      }
      setTimeout(() => {
        const retrySocket = net.connect({ port: 5432, host: 'localhost' }, () => {
          retrySocket.destroy();
          callback(true, null);
        });
        retrySocket.on('error', (retryErr) => {
          callback(false, retryErr.message);
        });
      }, 2000);
    });

    child.on('error', (spawnErr) => {
      callback(false, spawnErr.message);
    });
  });
}

function executeMavenForDialect(module, config, callback) {
  const projectRoot = process.cwd();
  const mvnwPath = path.join(projectRoot, 'mvnw');

  const args = [
    'compile',
    'liquibase:updateTestingRollback',
    '-pl', module,
    '-am',
    `-Dliquibase.driver=${config.driver}`,
    `-Dliquibase.url=${config.url}`,
    `-Dliquibase.username=${config.username}`,
    `-Dliquibase.password=${config.password}`
  ];

  logDebug(`Running Maven Liquibase for ${config.name}: ${mvnwPath} ${args.join(' ')}`);

  const child = spawn(mvnwPath, args, {
    cwd: projectRoot,
    env: process.env
  });

  let stdoutData = '';
  let stderrData = '';

  child.stdout.on('data', (data) => stdoutData += data.toString());
  child.stderr.on('data', (data) => stderrData += data.toString());

  child.on('close', (code) => {
    logDebug(`Finished Liquibase for ${config.name} with exit code ${code}`);
    callback({
      exitCode: code,
      output: stdoutData + (stderrData ? `\n--- STDERR ---\n${stderrData}` : '')
    });
  });

  child.on('error', (err) => {
    callback({
      exitCode: 1,
      output: `Execution failed: ${err.message}`
    });
  });
}
