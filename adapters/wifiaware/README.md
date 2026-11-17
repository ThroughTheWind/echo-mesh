# :adapters:wifiaware

A `MeshTransport` over Wi-Fi Aware (NAN) (Android library). Empty for now. Aware discovery messages
need no socket, but a data path does, and a socket needs the `INTERNET` permission, so the same
ADR-0003 condition as `:adapters:wifidirect` applies. Not every phone has Aware hardware: the adapter
must report itself unavailable rather than fail. Rules:
[`.ai/rules/transports.md`](../../.ai/rules/transports.md).
