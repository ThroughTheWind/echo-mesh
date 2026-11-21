#!/usr/bin/env node
// Localization gate (extended spec NFR-21, FR-18.2, FR-18.4, WI-037). For every resource directory
// with English strings, each shipped language must:
//   1. define every English string and plural, and no string English does not have;
//   2. keep each string's format arguments (%1$s, %d, …), since a missing one crashes at runtime;
//   3. give every plural at least an "one" and an "other" quantity.
// And the code must lay out with start and end, never left and right (FR-18.4), so a right-to-left
// language can come later without rework.
//
// `--self-test` proves each failure on fixtures, so a gate that reads nothing cannot pass.
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const root = new URL('../..', import.meta.url).pathname;

/** The languages that ship (FR-18.1). Adding one here is what makes the gate hold it complete. */
export const SHIPPED = ['fr', 'es'];

/** Resource directories that hold English strings. */
const RES = ['app/src/main/res', 'core/designsystem/src/main/res'];

/** Layout in absolute directions (FR-18.4). */
const ABSOLUTE =
  /\b(Alignment\.(TopLeft|CenterLeft|BottomLeft|TopRight|CenterRight|BottomRight)|AbsoluteAlignment|Arrangement\.Absolute|TextAlign\.(Left|Right)|absolutePadding|absoluteOffset)\b|\b(layout_margin|padding)(Left|Right)\b|gravity="(left|right)/;

const unescape = (s) => s.replace(/\\'/g, "'").replace(/\\"/g, '"');

/** Strings and plurals of one strings.xml: name → { args, quantities? }. Non-translatable ones are left out. */
export function parse(xml) {
  const out = new Map();
  for (const m of xml.matchAll(/<string\s+name="(\w+)"([^>]*)>([\s\S]*?)<\/string>/g)) {
    if (/translatable="false"/.test(m[2])) continue;
    out.set(m[1], { args: args(unescape(m[3])) });
  }
  for (const m of xml.matchAll(/<plurals\s+name="(\w+)"[^>]*>([\s\S]*?)<\/plurals>/g)) {
    const items = [...m[2].matchAll(/<item\s+quantity="(\w+)">([\s\S]*?)<\/item>/g)];
    const quantities = items.map((i) => i[1]);
    const other = items.find((i) => i[1] === 'other');
    out.set(m[1], { args: other ? args(unescape(other[2])) : '', quantities });
  }
  return out;
}

/** The format arguments of a string, sorted: "%1$s,%2$d". */
function args(text) {
  return [...text.replace(/%%/g, '').matchAll(/%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[sdfxXc]/g)].map((m) => m[0]).sort().join(',');
}

export function compare(english, other, lang, file) {
  const out = [];
  for (const [name, en] of english) {
    const tr = other.get(name);
    if (!tr) {
      out.push(`${file}: ${lang} has no "${name}"`);
      continue;
    }
    if (tr.args !== en.args) out.push(`${file}: ${lang} "${name}" has arguments [${tr.args}], English has [${en.args}]`);
    if (en.quantities) {
      for (const q of ['one', 'other']) {
        if (!tr.quantities?.includes(q)) out.push(`${file}: ${lang} plural "${name}" has no "${q}"`);
      }
    }
  }
  for (const name of other.keys()) if (!english.has(name)) out.push(`${file}: ${lang} has "${name}", which English does not`);
  return out;
}

export function absolute(source, file) {
  return source
    .split('\n')
    .flatMap((line, i) => (ABSOLUTE.test(line) ? [`${file}:${i + 1}: lays out left or right; use start and end (FR-18.4)`] : []));
}

function walk(dir, found = []) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      if (name !== 'build') walk(path, found);
    } else if (/\.(kt|xml)$/.test(name)) found.push(path);
  }
  return found;
}

function selfTest() {
  const en = parse('<string name="a">Hi %1$s</string><string name="b">Bye</string><plurals name="p"><item quantity="one">%d x</item><item quantity="other">%d xs</item></plurals><string name="k" translatable="false">K</string>');
  const fr = '<string name="a">Salut %1$s</string><string name="b">Au revoir</string><plurals name="p"><item quantity="one">%d x</item><item quantity="other">%d x</item></plurals>';
  const cases = [
    [compare(en, parse(fr), 'fr', 'f').length, 0, 'a complete translation passes'],
    [en.size, 3, 'a non-translatable string is left out'],
    [compare(en, parse(fr.replace('<string name="b">Au revoir</string>', '')), 'fr', 'f').some((p) => p.includes('no "b"')), true, 'a planted missing French string fails'],
    [compare(en, parse(fr.replace('Salut %1$s', 'Salut')), 'fr', 'f').some((p) => p.includes('arguments')), true, 'a dropped argument fails'],
    [compare(en, parse(fr + '<string name="z">Z</string>'), 'fr', 'f').some((p) => p.includes('"z", which English')), true, 'an extra string fails'],
    [compare(en, parse(fr.replace('<item quantity="one">%d x</item>', '')), 'fr', 'f').some((p) => p.includes('no "one"')), true, 'a plural with no "one" fails'],
    [args("l\\'heure %1$s 100%% %2$d"), '%1$s,%2$d', 'arguments ignore %% and apostrophes'],
    [absolute('Box(contentAlignment = Alignment.TopLeft)', 'f').length, 1, 'left alignment fails'],
    [absolute('Modifier.padding(start = 4.dp)\nAlignment.TopStart', 'f').length, 0, 'start and end pass'],
  ];
  const failed = cases.filter(([got, want]) => got !== want);
  for (const [got, want, name] of failed) console.error(`self-test failed: ${name}: got ${got}, want ${want}`);
  if (failed.length) process.exit(1);
  console.log(`locales gate self-test: ${cases.length} cases pass`);
}

function main() {
  if (process.argv.includes('--self-test')) return selfTest();
  const problems = [];
  let compared = 0;
  for (const res of RES) {
    const englishFile = join(root, res, 'values/strings.xml');
    const english = parse(readFileSync(englishFile, 'utf8'));
    if (english.size === 0) problems.push(`${res}: read no English string: the gate is blind`);
    for (const lang of SHIPPED) {
      const file = join(root, res, `values-${lang}/strings.xml`);
      if (!existsSync(file)) {
        problems.push(`${relative(root, file)} is missing: ${lang} ships (FR-18.1)`);
        continue;
      }
      problems.push(...compare(english, parse(readFileSync(file, 'utf8')), lang, relative(root, file)));
      compared += english.size;
    }
  }
  for (const dir of ['app/src/main', 'core']) {
    for (const file of walk(join(root, dir))) {
      if (file.includes('/test/') || file.includes('/build/')) continue;
      problems.push(...absolute(readFileSync(file, 'utf8'), relative(root, file)));
    }
  }
  if (problems.length) {
    console.error('Locales gate (NFR-21, FR-18):\n' + problems.map((p) => `  ${p}`).join('\n'));
    process.exit(1);
  }
  console.log(`locales gate: ${compared} strings checked in ${SHIPPED.join(', ')}, no left or right layout`);
}

main();
