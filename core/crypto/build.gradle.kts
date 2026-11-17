plugins {
    alias(libs.plugins.conventions.jvm.library)
}

// Empty until the threat-model ADR picks the library and protocol. The only module allowed to import
// a cryptographic API (checkCryptoBoundaries, .ai/rules/crypto.md).
