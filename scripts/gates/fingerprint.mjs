#!/usr/bin/env node
// Signing fingerprint gate (extended spec FR-19.2, WI-038). The release signing certificate's SHA-256
// lives once, in docs/signing-certificate.sha256, written from the first signed dry run; the release
// workflow refuses an APK whose certificate differs from it. This gate holds every place that shows
// it to that file: README.md and SECURITY.md carry it between
//     <!-- signing-fingerprint -->  and  <!-- /signing-fingerprint -->
// and before the first signed release, the file is absent and both say so.
//
// `--self-test` proves each failure on fixtures.
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = new URL('../..', import.meta.url).pathname;
const FILE = 'docs/signing-certificate.sha256';
const PLACES = ['README.md', 'SECURITY.md'];
export const NONE = 'No signed release yet';

/** "ab:cd…" or "abcd…" to "AB:CD:…", or null when it is not 32 bytes of hex. */
export function normalise(text) {
  const hex = text.replace(/[\s:]/g, '').toUpperCase();
  if (!/^[0-9A-F]{64}$/.test(hex)) return null;
  return hex.match(/../g).join(':');
}

/** The text between the markers, or null when they are missing. */
export function block(doc) {
  const m = doc.match(/<!-- signing-fingerprint -->([\s\S]*?)<!-- \/signing-fingerprint -->/);
  return m ? m[1] : null;
}

export function problems(fingerprint, docs) {
  const out = [];
  for (const [name, doc] of Object.entries(docs)) {
    const shown = block(doc);
    if (shown === null) {
      out.push(`${name} has no <!-- signing-fingerprint --> block`);
      continue;
    }
    if (fingerprint === null) {
      if (!shown.includes(NONE)) out.push(`${name} shows a fingerprint, but ${FILE} does not exist yet`);
    } else {
      const found = [...shown.matchAll(/[0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){31}|[0-9A-Fa-f]{64}/g)].map((m) => normalise(m[0]));
      if (!found.includes(fingerprint)) out.push(`${name} does not show the fingerprint in ${FILE}`);
      if (found.some((f) => f !== fingerprint)) out.push(`${name} shows a fingerprint other than the one in ${FILE}`);
    }
  }
  return out;
}

function selfTest() {
  const fp = normalise('a'.repeat(64));
  const shown = (text) => ({ 'README.md': `x\n<!-- signing-fingerprint -->\n${text}\n<!-- /signing-fingerprint -->\n` });
  const cases = [
    [problems(null, shown(NONE)).length, 0, 'no release yet, said so, passes'],
    [problems(null, shown(fp)).length, 1, 'a fingerprint before the file exists fails'],
    [problems(fp, shown(fp)).length, 0, 'the right fingerprint passes'],
    [problems(fp, shown(fp.replace(/:/g, '').toLowerCase())).length, 0, 'any spelling of it passes'],
    [problems(fp, shown(normalise('b'.repeat(64)))).length, 2, 'a different fingerprint fails'],
    [problems(fp, { 'README.md': 'nothing' }).length, 1, 'a missing block fails'],
    [normalise('ab:cd'), null, 'a short value is not a fingerprint'],
  ];
  const failed = cases.filter(([got, want]) => got !== want);
  for (const [got, want, name] of failed) console.error(`self-test failed: ${name}: got ${got}, want ${want}`);
  if (failed.length) process.exit(1);
  console.log(`fingerprint gate self-test: ${cases.length} cases pass`);
}

function main() {
  if (process.argv.includes('--self-test')) return selfTest();
  const path = join(root, FILE);
  let fingerprint = null;
  if (existsSync(path)) {
    fingerprint = normalise(readFileSync(path, 'utf8'));
    if (fingerprint === null) {
      console.error(`${FILE} does not hold a SHA-256 fingerprint`);
      process.exit(1);
    }
  }
  const docs = Object.fromEntries(PLACES.map((p) => [p, readFileSync(join(root, p), 'utf8')]));
  const found = problems(fingerprint, docs);
  if (found.length) {
    console.error('Fingerprint gate (FR-19.2):\n' + found.map((p) => `  ${p}`).join('\n'));
    process.exit(1);
  }
  console.log(fingerprint ? `fingerprint gate: ${PLACES.join(' and ')} show ${fingerprint}` : 'fingerprint gate: no signed release yet, and the docs say so');
}

main();
