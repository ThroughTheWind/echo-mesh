#!/usr/bin/env node
// Fast, SDK-free privacy gate for `node .ai/rungs.mjs check` (ADR-0003). It catches the two
// mistakes a person makes by hand, before Gradle runs:
//   1. a source AndroidManifest.xml that asks for android.permission.INTERNET;
//   2. a gradle/libs.versions.toml entry whose group is in gradle/forbidden-dependencies.txt.
// The Gradle gates (checkManifestPermissions, checkForbiddenDependencies) stay the authority: they
// read merged manifests and resolved graphs, which also catches what a library brings in.
//
// `--self-test` proves both directions on fixtures, so a gate that matches nothing cannot pass.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const root = new URL('../..', import.meta.url).pathname;

export function parseRules(text) {
  const rules = text.split('\n')
    .map((line, i) => ({ text: line.split('#')[0].trim(), line: i + 1 }))
    .filter((r) => r.text)
    .map(({ text, line }) => {
      const [group, artifact] = text.split(':');
      return { group, artifact, line };
    });
  if (rules.length === 0) throw new Error('forbidden-dependencies.txt has no rules');
  return rules;
}

export function forbiddenMatch(rules, group, artifact) {
  return rules.find((r) =>
    (group === r.group || group.startsWith(`${r.group}.`)) && (!r.artifact || r.artifact === artifact));
}

// Catalog entries look like: name = { module = "group:artifact", ... } or "group:artifact:version".
export function catalogModules(toml) {
  const found = [];
  const re = /(?:module\s*=\s*"|^\s*[\w.-]+\s*=\s*")([\w.-]+):([\w.-]+)(?::[^"]*)?"/gm;
  for (const m of toml.matchAll(re)) found.push({ group: m[1], artifact: m[2] });
  return found;
}

export function declaresInternet(manifestXml) {
  const withoutComments = manifestXml.replace(/<!--[\s\S]*?-->/g, '');
  return /<uses-permission(?:-sdk-23)?\b[^>]*android:name\s*=\s*"android\.permission\.INTERNET"/.test(withoutComments);
}

function manifests(dir, out = []) {
  for (const name of readdirSync(dir)) {
    if (['.git', 'build', '.gradle', 'node_modules'].includes(name)) continue;
    const path = join(dir, name);
    if (statSync(path).isDirectory()) manifests(path, out);
    else if (name === 'AndroidManifest.xml') out.push(path);
  }
  return out;
}

function selfTest() {
  const rules = parseRules('com.google.android.gms\ncom.example:tracker # x\n');
  const cases = [
    [!!forbiddenMatch(rules, 'com.google.android.gms', 'play-services-nearby'), true, 'gms is forbidden'],
    [!!forbiddenMatch(rules, 'com.google.crypto.tink', 'tink'), false, 'tink is allowed'],
    [!!forbiddenMatch(rules, 'com.example', 'tracker'), true, 'single artifact forbidden'],
    [!!forbiddenMatch(rules, 'com.example', 'fine'), false, 'its neighbour allowed'],
    [catalogModules('a = { module = "com.x:y", version.ref = "v" }\nb = "org.p:q:1.0"\n').length, 2, 'catalog parse'],
    [declaresInternet('<uses-permission android:name="android.permission.INTERNET" />'), true, 'INTERNET found'],
    [declaresInternet('<!-- <uses-permission android:name="android.permission.INTERNET" /> -->'), false, 'comment ignored'],
    [declaresInternet('<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />'), false, 'other permission'],
  ];
  const failed = cases.filter(([got, want]) => got !== want);
  for (const [got, want, name] of failed) console.error(`self-test failed: ${name}: got ${got}, want ${want}`);
  if (failed.length) process.exit(1);
  console.log(`privacy gate self-test: ${cases.length} cases pass`);
}

function main() {
  if (process.argv.includes('--self-test')) return selfTest();
  const problems = [];
  const rules = parseRules(readFileSync(join(root, 'gradle/forbidden-dependencies.txt'), 'utf8'));
  const modules = catalogModules(readFileSync(join(root, 'gradle/libs.versions.toml'), 'utf8'));
  if (modules.length === 0) problems.push('read no module from gradle/libs.versions.toml: the gate is blind');
  for (const { group, artifact } of modules) {
    const rule = forbiddenMatch(rules, group, artifact);
    if (rule) problems.push(`gradle/libs.versions.toml: ${group}:${artifact} is forbidden (forbidden-dependencies.txt line ${rule.line})`);
  }
  const found = manifests(root);
  if (found.length === 0) problems.push('found no AndroidManifest.xml: the gate is blind');
  for (const path of found) {
    if (declaresInternet(readFileSync(path, 'utf8'))) problems.push(`${relative(root, path)} asks for android.permission.INTERNET`);
  }
  if (problems.length) {
    console.error('Privacy gate (ADR-0003):\n' + problems.map((p) => `  ${p}`).join('\n'));
    process.exit(1);
  }
  console.log(`privacy gate: ${modules.length} catalog modules and ${found.length} manifests, nothing forbidden`);
}

main();
