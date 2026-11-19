# :adapters:ble

A `MeshTransport` over Bluetooth LE (Android library, WI-011). Rules:
[`.ai/rules/transports.md`](../../.ai/rules/transports.md).

- **Discovery:** legacy advertising of the app's service UUID, with a random token and the PSM of
  the phone's L2CAP server as service data (6 bytes, `Advertisement`).
- **Data:** one insecure L2CAP channel per neighbour, frames length-prefixed (`Framing`). No GATT
  server and no pairing: the BLE spike (WI-007) found L2CAP three times faster and steadier.
- **Nothing stable on the air** (threat model §5.1): token, PSM and advertising set change together
  at least every 10 minutes, and every link is dropped at rotation. The device name is never
  advertised; `AdvertisementPrivacyTest` checks the source.
- `BleTransport` holds the logic and runs on the JVM against a fake radio (`FakeBleRadio`, which
  also runs `MeshTransportContract`); `AndroidBleRadio` is the thin Android half.
