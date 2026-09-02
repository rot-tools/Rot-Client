# SkyBlock Data — JAR Audit

**Artifact:** `RotClient-2.0.0+mc26.2.jar`
**Path:** `build/libs/RotClient-2.0.0+mc26.2.jar`
**Bytes:** 921198
**SHA-256:** `FD7319B58C6487389AA2C8CADAE48007DE485A3030AD394479CBCF6F07DCD3F6`
**Class major version:** 69 (Java 25)
**Mod version:** `2.0.0+mc26.2`
**Build:** `gradlew clean test build` (green)

## Bundled SkyBlock data entries

- `assets/rotclient/data/`
- `assets/rotclient/data/bazaar-product-index.v1.json`
- `assets/rotclient/data/canonical-items.v1.json`
- `assets/rotclient/data/conflicts.v1.json`
- `assets/rotclient/data/domain-rules.v1.json`
- `assets/rotclient/data/manifest.v1.json`
- `assets/rotclient/data/mining-resources.v2.json`
- `assets/rotclient/data/provenance.v1.json`

## Forbidden content checks

- No user config, Current Session, or Session History payloads
- No diagnostic log or diagnostic data payloads
- No screenshots or backup archives
- No credential payloads
- No research dumps or raw Hypixel item cache
- No source files
- No absolute local machine or personal paths
- No protected private-use-character entry names

Runtime diagnostic classes and public-data cache implementation classes are
expected application code and are not runtime data payloads.

## Notes

This is the playable JAR, not the sources JAR. Static metadata snapshots are
present under `assets/rotclient/data/`. Dynamic Bazaar prices and local backup
metadata are not baked into the JAR.
