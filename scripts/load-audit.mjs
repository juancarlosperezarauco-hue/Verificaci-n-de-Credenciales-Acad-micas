// Bounded baseline on fictional data; never a production load generator.
import { parseArgs } from 'node:util';
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { performance } from 'node:perf_hooks';
import { base, call } from '../e2e/support/audit-api.mjs';

if (process.env.E2E_ALLOW_FIXTURES !== 'isolated-audit')
  throw new Error('Solo se admite E2E_ALLOW_FIXTURES=isolated-audit.');
const { values } = parseArgs({ options: {
  requests: { type: 'string', default: '40' }, concurrency: { type: 'string', default: '4' },
  'p95-ms': { type: 'string', default: '3000' },
} });
function integer(value, min, max) {
  if (!/^\d+$/.test(value)) throw new Error('Parámetro numérico no válido.');
  const number = Number(value);
  if (number < min || number > max) throw new Error('Parámetro fuera del límite audit.');
  return number;
}
const total = integer(values.requests, 2, 80);
const concurrency = integer(values.concurrency, 1, 8);
const budget = integer(values['p95-ms'], 100, 10_000);
const root = resolve(import.meta.dirname, '..');
const fixture = JSON.parse(readFileSync(resolve(root, 'e2e/test-results/fixture.json'), 'utf8'));
if (!/@[^@]+\.test$/.test(fixture.studentEmail) || !/^CRED-[0-9A-F]{32}$/i.test(fixture.verificationCode))
  throw new Error('El fixture debe ser ficticio y contener un código CRED válido.');
const session = await call('/api/auth/student/login', { email: fixture.studentEmail, password: fixture.password });
const cases = {
  publicVerification: { count: 0, failures: 0, milliseconds: [] },
  personalHistory: { count: 0, failures: 0, milliseconds: [] },
};
let next = 0;
const started = performance.now();
try {
  await Promise.all(Array.from({ length: concurrency }, async () => {
    for (;;) {
      const index = next++;
      if (index >= total) return;
      const personal = index % 2 !== 0, group = personal ? cases.personalHistory : cases.publicVerification;
      const timer = performance.now();
      let valid = false;
      try {
        const response = await fetch(base + (personal ? '/api/me/credentials/page?size=10'
          : '/api/public/credentials/' + fixture.verificationCode), {
          redirect: 'error', signal: AbortSignal.timeout(5000),
          headers: personal ? { Authorization: 'Bearer ' + session.token } : {},
        });
        if (response.ok && response.headers.get('content-type')?.includes('application/json')) {
          const body = await response.json();
          valid = personal ? Array.isArray(body.items) && body.total >= 1
            : body.verificationCode === fixture.verificationCode;
        }
      } catch { /* Record failures without URLs, tokens, private bodies or stack traces. */ }
      group.count++;
      if (!valid) group.failures++;
      group.milliseconds.push(performance.now() - timer);
    }
  }));
} finally {
  // Release the synthetic session rather than accumulating login sessions on repeated runs.
  await call('/api/security/sessions/current', null, session.token, 'DELETE', 204);
}
const elapsed = performance.now() - started;
const metrics = Object.fromEntries(Object.entries(cases).map(([name, group]) => {
  const sorted = group.milliseconds.sort((a, b) => a - b);
  const percentile = p => Math.round(sorted[Math.max(0, Math.ceil(sorted.length * p) - 1)] ?? 0);
  return [name, { requests: group.count, failures: group.failures,
    p50Ms: percentile(0.5), p95Ms: percentile(0.95), maxMs: Math.round(sorted.at(-1) ?? 0) }];
}));
const passed = Object.values(metrics).every(group => group.failures === 0 && group.p95Ms <= budget);
const summary = { scope: 'isolated-audit-fictional-baseline', passed, requests: total, concurrency,
  p95BudgetMs: budget, elapsedMs: Math.round(elapsed), requestsPerSecond: Number((total * 1000 / elapsed).toFixed(2)), metrics };
writeFileSync(resolve(root, 'e2e/test-results/load-summary.json'), JSON.stringify(summary, null, 2) + '\n');
console.log(JSON.stringify(summary, null, 2));
if (!passed) process.exitCode = 1;
