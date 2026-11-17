# :adapters:wifidirect

A `MeshTransport` over Wi-Fi Direct (Android library). Empty for now. Moving data over a Wi-Fi Direct
group uses IP sockets, and Android requires the `INTERNET` permission to open any socket, so this
adapter cannot ship until an ADR allows that permission and says how traffic is kept off every other
network (ADR-0003, `checkManifestPermissions`). Rules:
[`.ai/rules/transports.md`](../../.ai/rules/transports.md).
