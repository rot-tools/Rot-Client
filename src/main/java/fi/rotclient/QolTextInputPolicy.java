package fi.rotclient;

/** Bounds single-line settings input without applying the name-hider limit to every field. */
public final class QolTextInputPolicy {
    private QolTextInputPolicy() {}

    public static boolean accepts(String settingId, String current, int codePoint) {
        if (!Character.isValidCodePoint(codePoint) || codePoint < 32
                || codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE) return false;
        int limit = "qol.name_hider.custom_name".equals(settingId) ? NameHiderPolicy.MAX_CUSTOM_LENGTH : 2048;
        return current.length() + Character.charCount(codePoint) <= limit;
    }
}
