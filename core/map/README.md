# :core:map

Offline map loading and rendering (Android library), wrapping the open-source renderer an ADR will
choose. That ADR must show the renderer makes no network call of its own, and it decides how the one
user-started map download reaches the app without the `INTERNET` permission leaking into every build
(ADR-0003).
