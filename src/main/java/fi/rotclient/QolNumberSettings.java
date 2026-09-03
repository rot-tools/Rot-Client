package fi.rotclient;

/**
 * Ranges, steps, and slider-vs-stepper policy for QoL NUMBER settings.
 * Auto Clicker CPS stays a numeric stepper; everything else uses a slider.
 */
public final class QolNumberSettings {
    public record Spec(double min, double max, double step, boolean slider) {
        public Spec {
            if (!Double.isFinite(min)) {
                min = 0.0D;
            }
            if (!Double.isFinite(max) || max < min) {
                max = min;
            }
            if (!Double.isFinite(step) || step <= 0.0D) {
                step = 1.0D;
            }
        }

        public double clamp(double value) {
            if (!Double.isFinite(value)) {
                return min;
            }
            if (value < min) {
                return min;
            }
            if (value > max) {
                return max;
            }
            return value;
        }

        public double fraction(double value) {
            if (max <= min) {
                return 0.0D;
            }
            return Math.max(0.0D, Math.min(1.0D, (clamp(value) - min) / (max - min)));
        }

        public double fromFraction(double fraction) {
            double t = Double.isFinite(fraction) ? Math.max(0.0D, Math.min(1.0D, fraction)) : 0.0D;
            return snap(min + t * (max - min));
        }

        public double snap(double value) {
            double clamped = clamp(value);
            if (step <= 0.0D || max <= min) {
                return clamped;
            }
            double snapped = min + Math.round((clamped - min) / step) * step;
            return clamp(snapped);
        }
    }

    private QolNumberSettings() {
    }

    public static boolean usesSlider(String settingId) {
        Spec spec = spec(settingId);
        return spec != null && spec.slider();
    }

    public static Spec spec(String settingId) {
        if (settingId == null || settingId.isBlank()) {
            return null;
        }
        return switch (settingId) {
            case "qol.auto_clicker.cps",
                 "qol.auto_clicker.left_cps",
                 "qol.auto_clicker.right_cps" ->
                    new Spec(
                            AutoClickerPolicy.MIN_CPS,
                            AutoClickerPolicy.MAX_CPS,
                            0.5D,
                            false);
            case "qol.player_size.x", "qol.player_size.y", "qol.player_size.z" ->
                    new Spec(
                            PlayerSizePolicy.MIN_SCALE,
                            PlayerSizePolicy.MAX_SCALE,
                            0.05D,
                            true);
            case "qol.hide_players.distance" ->
                    new Spec(
                            HidePlayersPolicy.MIN_DISTANCE,
                            HidePlayersPolicy.MAX_DISTANCE,
                            1.0D,
                            true);
            case "qol.no_cursor_reset.unhook_timeout" ->
                    new Spec(
                            NoCursorResetPolicy.MIN_TIMEOUT_MS,
                            NoCursorResetPolicy.MAX_TIMEOUT_MS,
                            10.0D,
                            true);
            case "qol.chat_commands.previous_server_time" ->
                    new Spec(
                            SkyblockFlavorPolicy.MIN_PREVIOUS_SERVER_SECONDS,
                            SkyblockFlavorPolicy.MAX_PREVIOUS_SERVER_SECONDS,
                            10.0D,
                            true);
            case "qol.render_optimizer.nether_fog_scale" ->
                    new Spec(
                            SkyblockFlavorPolicy.MIN_NETHER_FOG_SCALE,
                            SkyblockFlavorPolicy.MAX_NETHER_FOG_SCALE,
                            0.05D,
                            true);
            case "qol.render_optimizer.armor_self",
                 "qol.render_optimizer.armor_others" ->
                    new Spec(0.0D, 100.0D, 1.0D, true);
            case "qol.slot_binds.line_width" ->
                    new Spec(
                            SlotBindsPolicy.MIN_LINE_WIDTH,
                            SlotBindsPolicy.MAX_LINE_WIDTH,
                            0.1D,
                            true);
            case "qol.inventory_walk.ping" ->
                    new Spec(
                            InventoryWalkPolicy.MIN_PING_MS,
                            InventoryWalkPolicy.MAX_PING_MS,
                            10.0D,
                            true);
            case "qol.wardrobe_keybinds.ping" ->
                    new Spec(
                            WardrobeKeybindPolicy.MIN_PING_MS,
                            WardrobeKeybindPolicy.MAX_PING_MS,
                            10.0D,
                            true);
            case "qol.wardrobe_keybinds.swap_a", "qol.wardrobe_keybinds.swap_b" ->
                    new Spec(1.0D, 9.0D, 1.0D, true);
            case "qol.wardrobe_keybinds.click_delay",
                 "qol.wardrobe_keybinds.close_delay" ->
                    new Spec(
                            WardrobeKeybindPolicy.MIN_DELAY_TICKS,
                            WardrobeKeybindPolicy.MAX_DELAY_TICKS,
                            1.0D,
                            true);
            case "qol.wardrobe_keybinds.delay_variance" ->
                    new Spec(0.0D, WardrobeKeybindPolicy.MAX_VARIANCE, 1.0D, true);
            case "qol.trajectories.range" ->
                    new Spec(
                            TrajectoryPredictor.MIN_RANGE,
                            TrajectoryPredictor.MAX_RANGE,
                            1.0D,
                            true);
            case "qol.trajectories.width" -> new Spec(0.1D, 5.0D, 0.1D, true);
            case "qol.trajectories.box_size" -> new Spec(0.5D, 3.0D, 0.1D, true);
            case "qol.trajectories.plane_size" -> new Spec(0.5D, 8.0D, 0.1D, true);
            case "qol.world_scanner.esp_range" ->
                    new Spec(
                            WorldScannerPolicy.MIN_ESP_RANGE,
                            WorldScannerPolicy.MAX_ESP_RANGE,
                            1.0D,
                            true);
            case "qol.auto_conversation.delay",
                 "qol.fishing_helper.pull_delay",
                 "qol.fishing_helper.pull_variance",
                 "qol.fishing_helper.recast_delay",
                 "qol.fishing_helper.recast_variance",
                 "qol.fishing_creatures.auto_delay" ->
                    new Spec(
                            AutoConversationPolicy.MIN_DELAY_TICKS,
                            AutoConversationPolicy.MAX_DELAY_TICKS,
                            1.0D,
                            true);
            case "qol.fishing_creatures.timer_length" ->
                    new Spec(30.0D, FishingCreaturesPolicy.MAX_TIMER_SECONDS, 1.0D, true);
            case "qol.item_rarity.fill_alpha", "qol.item_rarity.outline_alpha" ->
                    new Spec(0.0D, 1.0D, 0.01D, true);
            case "qol.custom_tooltip.horizontal_speed",
                 "qol.custom_tooltip.vertical_speed" ->
                    new Spec(1.0D, 32.0D, 1.0D, true);
            case "qol.custom_tooltip.border_width" -> new Spec(1.0D, 4.0D, 1.0D, true);
            case "qol.price_tooltips.burgers" -> new Spec(0.0D, 5.0D, 1.0D, true);
            case "qol.viewmodel.swing_speed" ->
                    new Spec(ViewmodelPolicy.SPEED_MIN, ViewmodelPolicy.SPEED_MAX, 1.0D, true);
            case "qol.viewmodel.offset_x", "qol.viewmodel.offset_y", "qol.viewmodel.offset_z" ->
                    new Spec(ViewmodelPolicy.OFFSET_MIN, ViewmodelPolicy.OFFSET_MAX, 0.05D, true);
            case "qol.viewmodel.scale_x", "qol.viewmodel.scale_y", "qol.viewmodel.scale_z" ->
                    new Spec(ViewmodelPolicy.SCALE_MIN, ViewmodelPolicy.SCALE_MAX, 0.05D, true);
            case "qol.viewmodel.rot_x", "qol.viewmodel.rot_y", "qol.viewmodel.rot_z" ->
                    new Spec(ViewmodelPolicy.ROT_MIN, ViewmodelPolicy.ROT_MAX, 1.0D, true);
            case "qol.viewmodel.swing_x", "qol.viewmodel.swing_y", "qol.viewmodel.swing_z" ->
                    new Spec(ViewmodelPolicy.SWING_MIN, ViewmodelPolicy.SWING_MAX, 0.05D, true);
            case "qol.item_scale.scale" ->
                    new Spec(ItemScalePolicy.MIN, ItemScalePolicy.MAX, 0.05D, true);
            case "qol.auto_experiments.click_delay" ->
                    new Spec(
                            AutoExperimentsPolicy.MIN_CLICK_DELAY,
                            AutoExperimentsPolicy.MAX_CLICK_DELAY,
                            1.0D,
                            true);
            case "qol.auto_experiments.delay_variety" ->
                    new Spec(
                            AutoExperimentsPolicy.MIN_DELAY_VARIETY,
                            AutoExperimentsPolicy.MAX_DELAY_VARIETY,
                            1.0D,
                            true);
            case "qol.auto_experiments.serum_count" ->
                    new Spec(
                            AutoExperimentsPolicy.MIN_SERUM,
                            AutoExperimentsPolicy.MAX_SERUM,
                            1.0D,
                            true);
            case "qol.cheater_wardrobe.click_delay",
                 "qol.cheater_wardrobe.close_delay" ->
                    new Spec(
                            WardrobeKeybindPolicy.MIN_DELAY_TICKS,
                            WardrobeKeybindPolicy.MAX_DELAY_TICKS,
                            1.0D,
                            true);
            case "qol.cheater_wardrobe.delay_variance" ->
                    new Spec(0.0D, WardrobeKeybindPolicy.MAX_VARIANCE, 1.0D, true);
            case "qol.auto_gfs.timer_increments" ->
                    new Spec(
                            AutoGfsPolicy.MIN_TIMER_SECONDS,
                            AutoGfsPolicy.MAX_TIMER_SECONDS,
                            1.0D,
                            true);
            case "qol.auto_sell.delay" ->
                    new Spec(AutoSellPolicy.MIN_DELAY, AutoSellPolicy.MAX_DELAY, 1.0D, true);
            case "qol.auto_sell.randomization" ->
                    new Spec(
                            AutoSellPolicy.MIN_RANDOMIZATION,
                            AutoSellPolicy.MAX_RANDOMIZATION,
                            1.0D,
                            true);
            case "qol.slayer_highlights.boss_width",
                 "qol.slayer_highlights.miniboss_width",
                 "qol.slayer_highlights.demon_width",
                 "qol.slayer_highlights.target_line_width" ->
                    new Spec(0.5D, 6.0D, 0.5D, true);
            case "qol.slayer_highlights.target_line_distance" ->
                    new Spec(4.0D, 64.0D, 1.0D, true);
            case "qol.slayer_active_boss_transparency.strength" ->
                    new Spec(
                            SlayerTransparencyPolicy.MIN_STRENGTH,
                            SlayerTransparencyPolicy.MAX_STRENGTH,
                            1.0D,
                            true);
            case "qol.slayer_irrelevant_mobs.strength" ->
                    new Spec(
                            SlayerIrrelevantMobsPolicy.MIN_STRENGTH,
                            SlayerIrrelevantMobsPolicy.MAX_STRENGTH,
                            1.0D,
                            true);
            case "qol.slayer_miniboss_alert.distance" ->
                    new Spec(1.0D, 64.0D, 1.0D, true);
            case "qol.slayer_cocoon_alert.pitch" ->
                    new Spec(0.0D, 2.0D, 0.05D, true);
            case "qol.slayer_cocoon_alert.volume" ->
                    new Spec(0.0D, 1.0D, 0.05D, true);
            case "qol.slayer_progress.warning_percent" ->
                    new Spec(50.0D, 90.0D, 1.0D, true);
            case "qol.slayer_drops.price_title_minimum" ->
                    new Spec(100_000.0D, 100_000_000.0D, 100_000.0D, true);
            case "qol.slayer_drops.ground_label_minimum" ->
                    new Spec(
                            SlayerGroundDropPolicy.MIN_VALUE,
                            SlayerGroundDropPolicy.MAX_VALUE,
                            100_000.0D,
                            true);
            case "qol.slayer_drops.profit_items_shown" ->
                    new Spec(1.0D, 10.0D, 1.0D, true);
            case "qol.slayer_dagger_swap.delay" ->
                    new Spec(
                            SlayerMechanicsPolicy.MIN_DAGGER_DELAY_TICKS,
                            SlayerMechanicsPolicy.MAX_DAGGER_DELAY_TICKS,
                            1.0D,
                            true);
            case "qol.slayer_dagger_swap.variance" ->
                    new Spec(
                            SlayerMechanicsPolicy.MIN_DAGGER_VARIANCE_TICKS,
                            SlayerMechanicsPolicy.MAX_DAGGER_VARIANCE_TICKS,
                            1.0D,
                            true);
            case "qol.slayer_auto_soulcry.min_delay",
                 "qol.slayer_auto_soulcry.max_delay" ->
                    new Spec(0.0D, 5.0D, 1.0D, true);
            case "qol.slayer_voidgloom.line_width",
                 "qol.slayer_voidgloom.boss_line_width",
                 "qol.slayer_tarantula.boss_line_width",
                 "qol.slayer_vampire_markers.boss_line_width" ->
                    new Spec(
                            SlayerFightPolicy.MIN_LINE_WIDTH,
                            SlayerFightPolicy.MAX_LINE_WIDTH,
                            1.0D,
                            true);
            case "qol.slayer_vampire_markers.twinclaws_delay" ->
                    new Spec(
                            SlayerFightPolicy.MIN_TWINCLAWS_DELAY_MS,
                            SlayerFightPolicy.MAX_TWINCLAWS_DELAY_MS,
                            1.0D,
                            true);
            case "qol.slayer_auto_start.delay" ->
                    new Spec(0.0D, 40.0D, 1.0D, true);
            case "qol.dungeon_terminals.delay" ->
                    new Spec(0.0D, 20.0D, 1.0D, true);
            case "qol.dungeon_terminals.protect_ms" ->
                    new Spec(
                            DungeonF7Policy.MIN_TERM_PROTECT_MS,
                            DungeonF7Policy.MAX_TERM_PROTECT_MS,
                            10.0D,
                            true);
            case "qol.dungeon_f7.relic_look_time" ->
                    new Spec(
                            DungeonF7Policy.MIN_RELIC_LOOK_MS,
                            DungeonF7Policy.MAX_RELIC_LOOK_MS,
                            1.0D,
                            true);
            case "qol.dungeon_f7.auto_i4_rotation" ->
                    new Spec(
                            DungeonF7Policy.MIN_I4_ROTATION_MS,
                            DungeonF7Policy.MAX_I4_ROTATION_MS,
                            1.0D,
                            true);
            case "qol.dungeon_esp.opacity" ->
                    new Spec(0.0D, 100.0D, 1.0D, true);
            case "qol.dungeon_announce.score_threshold" ->
                    new Spec(100.0D, 305.0D, 1.0D, true);
            case "qol.dungeon_termsim.ping" ->
                    new Spec(0.0D, 500.0D, 50.0D, true);
            case "qol.dungeon_esp.trigger_delay" ->
                    new Spec(0.0D, 1000.0D, 50.0D, true);
            case "qol.dungeon_hud.cheater_darken_factor" ->
                    new Spec(0.0D, 1.0D, 0.05D, true);
            case "qol.dungeon_f7.superboom_delay" ->
                    new Spec(1.0D, 10.0D, 1.0D, true);
            case "qol.auto_dojo.control_predict" ->
                    new Spec(1.0D, 20.0D, 1.0D, true);
            case "qol.auto_dojo.mastery_delay" ->
                    new Spec(0.0D, 2000.0D, 50.0D, true);
            case "qol.dungeon_requeue.delay" ->
                    new Spec(0.0D, 200.0D, 1.0D, true);
            case "qol.dungeon_menus.party_cata" ->
                    new Spec(0.0D, 60.0D, 1.0D, true);
            case "qol.slayer_big_drops.scale" ->
                    new Spec(1.0D, 8.0D, 0.25D, true);
            case "qol.slayer_big_drops.range" ->
                    new Spec(0.5D, 5.0D, 0.1D, true);
            case "qol.slayer_big_drops.unscale_seconds" ->
                    new Spec(1.0D, 60.0D, 1.0D, true);
            case "qol.storage_overlay.columns" -> new Spec(1.0D, 5.0D, 1.0D, true);
            case "qol.storage_overlay.height" -> new Spec(180.0D, 720.0D, 18.0D, true);
            case "qol.storage_overlay.scroll_speed" -> new Spec(1.0D, 40.0D, 1.0D, true);
            case "qol.storage_overlay.padding", "qol.storage_overlay.margin" ->
                    new Spec(0.0D, 40.0D, 1.0D, true);
            case "qol.freecam.speed" ->
                    new Spec(
                            FreecamPolicy.MIN_SPEED,
                            FreecamPolicy.MAX_SPEED,
                            0.1D,
                            true);
            case "qol.hud_layout.scale",
                 "rotclient.hud_style.scale" -> new Spec(0.6D, 2.5D, 0.05D, true);
            case "qol.custom_cursor.size" -> new Spec(0.6D, 2.4D, 0.05D, true);
            case "qol.stall_market.sell_threshold" ->
                    new Spec(0.0D, 2_000_000_000D, 10_000D, true);
            case "qol.foraging_helpers.sea_lumies_min" -> new Spec(1.0D, 4.0D, 1.0D, true);
            case "qol.foraging_cheats.min_cluster" -> new Spec(1.0D, 35.0D, 1.0D, true);
            case "qol.foraging_cheats.click_delay" -> new Spec(1.0D, 20.0D, 1.0D, true);
            case "qol.camera.distance" ->
                    new Spec(
                            TempleDungeonPolicy.MIN_CAMERA_DISTANCE,
                            TempleDungeonPolicy.MAX_CAMERA_DISTANCE,
                            0.5D,
                            true);
            case "qol.command_keybinds.ratelimit_count" ->
                    new Spec(
                            RingPolicy.MIN_RATELIMIT_COUNT,
                            RingPolicy.MAX_RATELIMIT_COUNT,
                            1.0D,
                            true);
            case "qol.command_keybinds.ratelimit_ticks" ->
                    new Spec(
                            RingPolicy.MIN_RATELIMIT_TICKS,
                            RingPolicy.MAX_RATELIMIT_TICKS,
                            1.0D,
                            true);
            case "qol.command_keybinds.length_limit" ->
                    new Spec(
                            RingPolicy.MIN_LENGTH_LIMIT,
                            RingPolicy.MAX_LENGTH_LIMIT,
                            1.0D,
                            true);
            default -> dynamicWorldScannerSpec(settingId);
        };
    }

    private static Spec dynamicWorldScannerSpec(String settingId) {
        if (!settingId.startsWith("qol.world_scanner.target.")) {
            return null;
        }
        if (settingId.endsWith(".opacity")) {
            return new Spec(0.0D, 1.0D, 0.01D, true);
        }
        if (settingId.endsWith(".name_scale")) {
            return new Spec(0.5D, 2.0D, 0.05D, true);
        }
        return null;
    }
}
