package fi.rotclient.rebuild.illegal;

final class AutoHealingSettings {
    boolean enabled;
    Item item = Item.WAND_OF_ATONEMENT;
    int healthPercent = 20;
    int minimumSeconds = 6;
    int stepDelayTicks = 2;
    int selectionDelayTicks = 2;
    int returnDelayTicks = 2;

    void normalize() {
        if (item == null) item = Item.WAND_OF_ATONEMENT;
        healthPercent = Math.clamp(healthPercent, 1, 100);
        minimumSeconds = Math.clamp(minimumSeconds, 1, 20);
        stepDelayTicks = Math.clamp(stepDelayTicks, 1, 10);
        selectionDelayTicks = Math.clamp(selectionDelayTicks, 1, 10);
        returnDelayTicks = Math.clamp(returnDelayTicks, 1, 10);
    }

    enum Item {
        WAND_OF_ATONEMENT("Wand of Atonement"),
        WAND_OF_RESTORATION("Wand of Restoration"),
        WAND_OF_MENDING("Wand of Mending"),
        WAND_OF_HEALING("Wand of Healing"),
        FLORID_ZOMBIE_SWORD("Florid Zombie Sword"),
        ORNATE_ZOMBIE_SWORD("Ornate Zombie Sword"),
        ZOMBIE_SWORD("Zombie Sword");

        final String title;

        Item(String title) {
            this.title = title;
        }
    }
}
