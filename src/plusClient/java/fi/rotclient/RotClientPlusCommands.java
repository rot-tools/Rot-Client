package fi.rotclient;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Commands contributed only by the Plus client edition. */
final class RotClientPlusCommands {
    private RotClientPlusCommands() {
    }

    static void contribute(LiteralArgumentBuilder<FabricClientCommandSource> root,
                           String legacyAlias) {
        root.then(literal("autoclicker")
                .then(literal("add")
                        .then(literal("left").executes(context -> autoClicker(
                                context.getSource(), legacyAlias, true, "add")))
                        .then(literal("right").executes(context -> autoClicker(
                                context.getSource(), legacyAlias, false, "add"))))
                .then(literal("remove")
                        .then(literal("left").executes(context -> autoClicker(
                                context.getSource(), legacyAlias, true, "remove")))
                        .then(literal("right").executes(context -> autoClicker(
                                context.getSource(), legacyAlias, false, "remove"))))
                .then(literal("list").executes(context -> autoClicker(
                        context.getSource(), legacyAlias, true, "list"))));
        root.then(literal("superboom")
                .then(literal("add").executes(context -> run(
                        context.getSource(), legacyAlias, "add")))
                .then(literal("remove").executes(context -> run(
                        context.getSource(), legacyAlias, "remove")))
                .then(literal("list").executes(context -> run(
                        context.getSource(), legacyAlias, "list"))));
    }

    static void help(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal(
                "Rot Client+ commands: /rot autoclicker add|remove left|right, "
                        + "/rot autoclicker list, /rot superboom add|remove|list"));
    }

    private static int autoClicker(FabricClientCommandSource source,
                                   String legacyAlias, boolean left, String action) {
        notice(source, legacyAlias);
        var feedback = (java.util.function.Consumer<String>) text ->
                source.sendFeedback(Component.literal(text));
        return switch (action) {
            case "add" -> QolClientFlavorSupport.hooks().autoClickerAdd(left, feedback);
            case "remove" -> QolClientFlavorSupport.hooks().autoClickerRemove(left, feedback);
            default -> QolClientFlavorSupport.hooks().autoClickerList(feedback);
        };
    }

    private static int run(FabricClientCommandSource source,
                           String legacyAlias, String action) {
        notice(source, legacyAlias);
        return switch (action) {
            case "add" -> superboomAdd(source);
            case "remove" -> superboomRemove(source);
            default -> superboomList(source);
        };
    }

    private static void notice(FabricClientCommandSource source, String legacyAlias) {
        if (legacyAlias != null) {
            source.sendFeedback(Component.literal(
                    RotClientCommandNotices.deprecationNotice(legacyAlias)));
        }
    }

    private static int superboomAdd(FabricClientCommandSource source) {
        String blockId = lookedBlockId();
        if (blockId.isBlank()) {
            source.sendError(Component.literal("Look at a block first. Usage: /rot superboom add"));
            return 0;
        }
        DungeonAthenSettings athen = RotClientClient.qolConfigPublic().extras().athen();
        athen.superboomExtraBlocks = DungeonAthenPortPolicy.addExtraBlock(
                athen.superboomExtraBlocks, blockId);
        TrackerStore.save(RotClientClient.trackerConfig());
        source.sendFeedback(Component.literal("Superboom extra block added: "
                + DungeonLeftoverPolicy.path(blockId)));
        return 1;
    }

    private static int superboomRemove(FabricClientCommandSource source) {
        String blockId = lookedBlockId();
        if (blockId.isBlank()) {
            source.sendError(Component.literal("Look at a block first. Usage: /rot superboom remove"));
            return 0;
        }
        DungeonAthenSettings athen = RotClientClient.qolConfigPublic().extras().athen();
        athen.superboomExtraBlocks = DungeonAthenPortPolicy.removeExtraBlock(
                athen.superboomExtraBlocks, blockId);
        TrackerStore.save(RotClientClient.trackerConfig());
        source.sendFeedback(Component.literal("Superboom extra block removed: "
                + DungeonLeftoverPolicy.path(blockId)));
        return 1;
    }

    private static int superboomList(FabricClientCommandSource source) {
        String csv = RotClientClient.qolConfigPublic().extras().athen().superboomExtraBlocks;
        if (csv == null || csv.isBlank()) {
            source.sendFeedback(Component.literal(
                    "No extra Superboom blocks. Defaults still include cracked stone bricks and crypt walls."));
            return 1;
        }
        source.sendFeedback(Component.literal("Superboom extra blocks: " + csv));
        return 1;
    }

    private static String lookedBlockId() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || !(client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return "";
        }
        return DungeonRuntime.lookedBlockId(client, hit.getBlockPos());
    }
}
