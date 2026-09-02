package fi.rotclient;

enum MiningSessionCategory {
    /** Selected target obtained from confirmed mining evidence. */
    TARGET_MINED,

    /** Confirmed mined resource outside the selected target. */
    OTHER_MINED,

    /** Item confirmed as a chest reward. */
    CHEST_LOOT,

    /** Non-item currency such as powder or essence. */
    CURRENCY
}
