package fi.rotclient;

/** Read-only capture of the current Bazaar price cache. */
interface MiningSessionPriceProvider {
    MiningSessionPriceBook capture(long nowMillis);
}
