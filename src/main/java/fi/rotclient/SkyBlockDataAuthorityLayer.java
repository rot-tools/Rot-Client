package fi.rotclient;

/**
 * Binding authority layers for SkyBlock static metadata. See
 * {@code docs/ARCHITECTURE.md} section "SkyBlock Data Authority and Provenance".
 */
enum SkyBlockDataAuthorityLayer {
    OFFICIAL_HYPIXEL,
    COMMUNITY_ENRICHMENT,
    LIVE_TRACE,
    ROT_CANONICAL,
    ROT_DOMAIN_RULE
}
