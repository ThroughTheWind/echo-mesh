# Changelog

All notable changes are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- The project skeleton: a multi-module Kotlin and Compose build, the `MeshTransport` contract with
  an in-memory transport for simulated phones, and build gates that refuse the `INTERNET`
  permission, proprietary or tracking dependencies, and imports across the transport and crypto
  boundaries.
