package fi.rotclient.rebuild;

public enum Category {
    COMBAT("Combat"), SLAYER("Slayer"), EVENTS("Events"), FISHING("Fishing"),
    FORAGING("Foraging"), GARDEN("Garden"), GUI("GUI"), DUNGEONS("Dungeons"),
    KUUDRA("Kuudra"), MINING("Mining"), UTILITIES("Utilities"), HUD_DISPLAY("HUD & Display"),
    RENDER("Render"), INTERFACE("Interface");

    private final String title;

    Category(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }
}
