# :adapters:loopback

An in-memory `MeshTransport` for tests: `LoopbackNetwork` creates simulated phones and links them by
hand, so mesh, gossip and hostile-peer tests run on the JVM in milliseconds. Its tests extend
`MeshTransportContract`, which every real transport must pass too. Debug builds may register it; the
release build must not link it (CI checks this).
