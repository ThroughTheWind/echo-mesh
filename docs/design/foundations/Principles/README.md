# Principles

The six rules every decision in Echo traces back to. When two rules conflict, the earlier one wins.

1. **Glance, then act.** The map must answer three questions in under a second at arm's length: what's dangerous near me, where's help, and can my phone hear anyone. Pins are big, families differ by shape *and* colour, the numbers that matter are 18sp or larger, and text targets 7:1 contrast.
2. **Dark first, sunlight ready, discreet on demand.** `dark` is the default: near-black surfaces save OLED battery and don't light up a face at night. `sun` is a high-contrast white theme for direct sunlight. `discreet` is black and grey, dimmed, silent and still. See [Themes](../Themes/README.md).
3. **Built for gloves and one hand.** Every target is at least `touch-min` (64dp, Zapper uses 56), with `space-md` (12dp) between targets. Frequent actions live in the bottom `thumb-zone` (45% of the screen).
4. **Honest by construction.** The UI never claims more safety than the threat model supports. Verified (group) and unverified (public) reports look different everywhere. A public count says "4 nearby phones confirmed", never "verified". Copy that makes a safety claim cites a line in the threat model (SPEC NFR-14).
5. **Capabilities, not radios.** Like Zapper's "brand-agnostic" rule: components show what the mesh can do ("Short range, low power"), never "BLE" or "Wi-Fi Direct". A radio's own name appears only where the user must switch it on in Android settings, and that string comes from the transport adapter.
6. **Open, native and offline.** Material Symbols Rounded (Apache-2.0) and Roboto Flex (OFL-1.1) are bundled in the APK; Zapper's downloadable font goes through Google Play Services, which Echo may not use. No dynamic colour: every phone in a crowd shows the same pins.
