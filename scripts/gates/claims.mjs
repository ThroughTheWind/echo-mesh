#!/usr/bin/env node
// Honest-copy gate (spec NFR-14, threat model §7, WI-026). Every safety claim the app makes cites the
// §7 row it comes from, with a comment on the line just above its string:
//     <!-- §7 P1 -->
//     <string name="protects_1">…</string>
// It fails when:
//   1. a claim string (the "What Echo protects" page and onboarding's protects / doesn't protect
//      lines, by name) cites no row;
//   2. any string cites a row §7 does not have;
//   3. a §7 row is missing from the "What Echo protects" page (protects_N / not_protects_N).
//
// `--self-test` proves each failure on fixtures, so a gate that matches nothing cannot pass.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = new URL('../..', import.meta.url).pathname;

/** The names of strings that are claims and must cite a row. */
export const CLAIM = /^(onboarding_protects_\d+|onboarding_not_\d+|protects_\d+|not_protects_\d+)$/;

/** The "What Echo protects" page, which must show every row. */
export const PAGE = /^(protects_\d+|not_protects_\d+)$/;

/** Row ids in threat model §7, in order. */
export function rows(threatModel) {
  const start = threatModel.indexOf('## 7.');
  const end = threatModel.indexOf('\n## 8.', start);
  if (start < 0 || end < 0) return [];
  const section = threatModel.slice(start, end);
  return [...section.matchAll(/^- \*\*([PN]\d+)\*\* /gm)].map((m) => m[1]);
}

/** Every string with the §7 rows its preceding comment cites (none when there is no such comment). */
export function strings(xml) {
  const found = [];
  const re = /(?:<!--\s*§7\s+([PN]\d+(?:\s*,\s*[PN]\d+)*)\s*-->\s*)?<string\s+name="([\w]+)"/g;
  for (const m of xml.matchAll(re)) {
    found.push({ name: m[2], cites: m[1] ? m[1].split(',').map((s) => s.trim()) : [] });
  }
  return found;
}

export function problems(threatModel, xml) {
  const ids = rows(threatModel);
  const out = [];
  if (ids.length === 0) return ['read no row from threat model §7: the gate is blind'];
  const all = strings(xml);
  if (all.length === 0) return ['read no string from strings.xml: the gate is blind'];
  for (const { name, cites } of all) {
    if (CLAIM.test(name) && cites.length === 0) out.push(`${name} is a safety claim and cites no §7 row`);
    for (const id of cites) if (!ids.includes(id)) out.push(`${name} cites §7 ${id}, which does not exist`);
  }
  const onPage = new Set(all.filter((s) => PAGE.test(s.name)).flatMap((s) => s.cites));
  for (const id of ids) if (!onPage.has(id)) out.push(`§7 ${id} is not on the "What Echo protects" page`);
  return out;
}

function selfTest() {
  const tm = '## 7. Claims\n- **P1** one\n- **N1** two\n\n## 8. Next\n';
  const good =
    '<!-- §7 P1 -->\n<string name="protects_1">a</string>\n' +
    '<!-- §7 N1 -->\n<string name="not_protects_1">b</string>\n<string name="back">Back</string>\n';
  const cases = [
    [problems(tm, good).length, 0, 'a fully cited page passes'],
    [rows(tm).join(','), 'P1,N1', 'rows parse'],
    [problems(tm, good.replace('<!-- §7 P1 -->\n', '')).some((p) => p.includes('cites no')), true, 'an uncited claim fails'],
    [problems(tm, good.replace('§7 N1', '§7 N9')).some((p) => p.includes('does not exist')), true, 'a missing row fails'],
    [problems(tm, good.replace('§7 N1', '§7 P1')).some((p) => p.includes('N1 is not on')), true, 'a row left off the page fails'],
    [problems('', good)[0].includes('blind'), true, 'no §7 is blind'],
    [problems(tm, '<!-- §7 P1, N1 -->\n<string name="share_x">c</string>\n' + good).length, 0, 'two rows in one comment'],
  ];
  const failed = cases.filter(([got, want]) => got !== want);
  for (const [got, want, name] of failed) console.error(`self-test failed: ${name}: got ${got}, want ${want}`);
  if (failed.length) process.exit(1);
  console.log(`claims gate self-test: ${cases.length} cases pass`);
}

function main() {
  if (process.argv.includes('--self-test')) return selfTest();
  const threatModel = readFileSync(join(root, 'docs/threat-model.md'), 'utf8');
  const xml = readFileSync(join(root, 'app/src/main/res/values/strings.xml'), 'utf8');
  const found = problems(threatModel, xml);
  if (found.length) {
    console.error('Claims gate (NFR-14, threat model §7):\n' + found.map((p) => `  ${p}`).join('\n'));
    process.exit(1);
  }
  const cited = strings(xml).filter((s) => s.cites.length).length;
  console.log(`claims gate: ${cited} strings cite §7, all ${rows(threatModel).length} rows on the page`);
}

main();
