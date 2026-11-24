// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import java.util.regex.Pattern;

public record CommandShortcut(String command, String label, String key) {
    static final int MAX_COMMAND_LENGTH = 256;
    static final int MAX_LABEL_LENGTH = 40;
    private static final Pattern VALID_COMMAND = Pattern.compile("/[a-zA-Z0-9_:.?-]+(?: .+)?");

    record Notice(String title, String subtitle) { }

    static String cleanCommand(String input) {
        if (input == null || input.codePoints().anyMatch(Character::isISOControl)) return "";
        String command = input.strip();
        if (!command.startsWith("/")) command = "/" + command;
        return command.length() <= MAX_COMMAND_LENGTH && VALID_COMMAND.matcher(command).matches() ? command : "";
    }

    static String cleanLabel(String value) {
        if (value == null) return "";
        String label = value.replaceAll("[\\p{Cntrl}§]", "").strip();
        return label.substring(0, label.offsetByCodePoints(0, Math.min(label.codePointCount(0, label.length()), MAX_LABEL_LENGTH)));
    }

    static String cleanKey(String value) {
        return value != null && value.matches("(?:key\\.keyboard\\.[a-z0-9_.-]+|scancode\\.[0-9]+)")
                && !value.equals("key.keyboard.unknown")
                && !value.equals("key.keyboard.right.shift") ? value : "";
    }

    boolean isTravelCommand() {
        String normalized = WarpCatalog.normalized(command);
        return WarpCatalog.find(normalized) != null || normalized.startsWith("/warp ")
                || normalized.startsWith("/visit ") || normalized.startsWith("/guest ");
    }

    Notice notice() {
        String normalized = WarpCatalog.normalized(command);
        WarpCatalog.Destination destination = WarpCatalog.find(normalized);
        if (destination != null) return new Notice(label.isEmpty() ? destination.name() : label, "Warp requested");

        if (normalized.startsWith("/warp ")) {
            String target = command.substring(command.indexOf(' ') + 1).strip();
            return new Notice(label.isEmpty() ? "/warp " + cleanLabel(target) : label, "Warp requested");
        }
        if (normalized.startsWith("/visit ") || normalized.startsWith("/guest ")) {
            String player = cleanLabel(command.substring(command.indexOf(' ') + 1));
            return new Notice(label.isEmpty() ? "Visit " + player : label, "Visit requested");
        }

        String root = normalized.split(" ", 2)[0];
        String title = switch (root) {
            case "/wardrobe", "/wd" -> "Wardrobe";
            case "/warp" -> "Fast Travel";
            case "/ah", "/auctionhouse" -> "Auction House";
            case "/bz", "/bazaar" -> "Bazaar";
            case "/ec", "/enderchest" -> "Ender Chest";
            case "/sbmenu", "/skyblockmenu" -> "SkyBlock Menu";
            default -> root.isEmpty() ? "Command" : root;
        };
        return new Notice(label.isEmpty() ? title : label, "Command sent");
    }
}
