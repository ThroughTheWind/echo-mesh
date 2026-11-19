**Authoritative for:** how a version is numbered, signed, and published to GitHub Releases.
**Not authoritative for:** what goes into a version (the roadmap) or what changed (the [changelog](../CHANGELOG.md)).

# Releasing

The app ships as a signed APK on GitHub Releases (ADR-0003). Pushing a tag `vX.Y.Z` runs
[`release.yml`](../.github/workflows/release.yml), which tests, builds, signs and verifies the APK,
then publishes it with its release notes. The `dev` pre-release is separate: CI refreshes it with a
debug build on every push to `main`.

## Versions

- The version lives once, as `app` in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml).
- The Android `versionCode` is derived from it in `app/build.gradle.kts` as
  `MAJOR * 1_000_000 + MINOR * 1_000 + PATCH`, so 0.1.0 is 1000. Never type it by hand.
- Debug builds append `-dev` to the version name and install as a separate app (`…debug`).
- Versions follow [Semantic Versioning](https://semver.org/). Before 1.0, a minor version is a
  roadmap milestone and a patch version is a fix. 1.0 needs the independent security review
  ([SECURITY.md](../SECURITY.md)).

## One-time setup: the signing key

Only the maintainer does this, once. **The key is the app's identity:** Android installs an update
only if it is signed with the same key, and for this app a stolen key would let someone ship a
malicious update that phones accept. Keep the keystore and its password in a password manager, a
second copy offline, and the CI copy below, and nowhere else.

1. Create the keystore (needs a JDK). Pick a long password; it is asked twice, and the same one is
   used for the key.

   ```bash
   keytool -genkeypair -v -storetype PKCS12 -keystore release.jks \
     -alias release -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Release"
   ```

2. Add four repository secrets under *Settings → Secrets and variables → Actions*, or with the
   GitHub CLI (`<repo>` is `ThroughTheWind/<repo-name>`):

   | Secret | Value |
   | --- | --- |
   | `RELEASE_KEYSTORE_BASE64` | The keystore file, base64-encoded on one line |
   | `RELEASE_KEYSTORE_PASSWORD` | The keystore password |
   | `RELEASE_KEY_ALIAS` | `release` |
   | `RELEASE_KEY_PASSWORD` | The same password (PKCS12 keystores use one password) |

   ```bash
   base64 -w0 release.jks | gh secret set RELEASE_KEYSTORE_BASE64 --repo <repo>
   gh secret set RELEASE_KEYSTORE_PASSWORD --repo <repo>   # prompts for the value
   gh secret set RELEASE_KEY_ALIAS --repo <repo> --body release
   gh secret set RELEASE_KEY_PASSWORD --repo <repo>        # prompts for the value
   ```

3. Check the setup with a dry run: *Actions → Release → Run workflow* on `main`. It builds and signs
   the APK and attaches it to the run without publishing anything.
4. **Publish the fingerprint** (extended spec FR-19.2), in one pull request: copy the signing
   certificate's SHA-256 from the dry run's notice into `docs/signing-certificate.sha256`, and
   between the `signing-fingerprint` markers in `README.md` and `SECURITY.md`. The `fingerprint`
   gate fails until all three agree, and from then on the release workflow refuses an APK signed
   with any other certificate. A tag is refused until the file exists.

Never commit the keystore. `*.jks` and `*.keystore` are in `.gitignore`.

### Keeping the key (FR-19.1)

The maintainer holds the key alone (SECURITY.md states the risk). Keep:

- **The master copy offline:** the keystore file on an encrypted USB drive (VeraCrypt, LUKS or
  BitLocker), kept somewhere the maintainer controls and away from the phone and laptop carried to
  protests.
- **An encrypted offline backup** in a second place, so a lost or seized drive does not end the
  app's ability to ship updates.
- **The password** in a password manager, never next to either copy.
- **The CI copy:** the repository secrets above, which let the release workflow sign. They are a
  second, online copy of the key, kept so releases are built and signed in one reviewed workflow
  (Antoine, 2026-11-19; extended spec FR-19.1). Protect what can reach it: two-factor
  authentication on the maintainer's GitHub account, and no workflow that runs on a fork's pull
  request with access to secrets. If the account is ever compromised, treat the key as exposed.

If the key is ever lost, phones cannot update: users must uninstall and install a new app. If it is
ever exposed, say so at once in SECURITY.md and the README, stop releasing with it, and publish a
new fingerprint through channels users already trust.

## Cutting a release

Use the `/cut-release` skill, which follows these steps.

1. **Check `main` is ready:** the roadmap phase for this version is merged and CI is green, and
   the [storage and radio audits](audits.md) passed on the candidate, with their evidence recorded.
2. **Prepare it on a branch** `chore/release-X.Y.Z`, in one pull request: set `app = "X.Y.Z"` in
   `gradle/libs.versions.toml`; in `CHANGELOG.md`, rename `## [Unreleased]` to
   `## [X.Y.Z] - YYYY-MM-DD` and add a fresh `## [Unreleased]` above it; write
   `docs/releases/X.Y.Z.md` (a title line, what the release is for, how to install it, and **what it
   does not protect**). The workflow appends the changelog section and the checksums.
3. **Merge it**, then tag the merge commit on `main` and push the tag:

   ```bash
   git switch main && git pull
   git tag -a vX.Y.Z -m "Release X.Y.Z"
   git push origin vX.Y.Z
   ```

   Do not create the release on the GitHub web page: the workflow creates it, and fails if it exists.
4. **Check the release page:** the APK and its `.sha256` are attached, and the APK installs over the
   previous release on a reference phone.

The workflow refuses a tag that does not match the version, a version without release notes or a
changelog section, missing secrets, an APK that is unsigned, debuggable, carries the wrong version,
or asks for a forbidden permission.

If the workflow failed **before publishing**, fix it on `main`, delete the tag and push it again on
the fixed commit. If a release went out broken, do not move or reuse its tag: release `X.Y.Z+1`.
