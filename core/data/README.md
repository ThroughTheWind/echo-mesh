# :core:data

The encrypted local store and settings (spec FR-12, threat model §5.6), on the plain JVM. `SealedFiles`
keeps small files sealed by a `SealingKey` from `:core:crypto` (in the app, the Android Keystore key of
`:core:crypto:keystore`); `SettingsStore` keeps the settings in one of them. Reports, votes and
positions never come here: they live in memory only (FR-12.2). Nothing here may write location
history or a plaintext log.
