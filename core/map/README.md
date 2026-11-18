# :core:map

The offline map (Android library), from [ADR-0006](../../docs/decisions/ADR-0006-offline-map.md): VTM
drawing Mapsforge `.map` files the user downloads in their browser and imports through the file
picker, or receives from another phone. `MapLibrary` checks and keeps the files, `OfflineMap` draws
one, `OwnPosition` gives the user's own position (never stored), and `MapSharing` holds the browser,
picker and share-sheet intents. No network code: `vtm-http` and OkHttp are forbidden dependencies.
