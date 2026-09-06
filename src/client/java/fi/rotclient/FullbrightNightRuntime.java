package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3f;

/**
 * Client bridge for Fullbright and Always Night. Mixins stay thin; dusk vs
 * snap lives here plus {@link FullbrightNightPolicy}.
 */
public final class FullbrightNightRuntime {
    private static final FullbrightNightPolicy.SkyPose GENERIC_DAY =
            FullbrightNightPolicy.SkyPose.daytimeStart();

    private static ClientLevel lastLevel;
    private static boolean duskRunning;
    private static boolean latched;
    private static long duskStartMs;
    private static FullbrightNightPolicy.SkyPose startPose;
    private static long poseMs = Long.MIN_VALUE;
    private static FullbrightNightPolicy.SkyPose cachedPose;
    private static float cachedBlend = 1.0F;

    private FullbrightNightRuntime() {
    }

    static void onAlwaysNightChanged(boolean wasEnabled, boolean enabled, boolean worldLoaded) {
        poseMs = Long.MIN_VALUE;
        cachedPose = null;
        if (!enabled) {
            duskRunning = false;
            latched = false;
            startPose = null;
            return;
        }
        if (wasEnabled) {
            return;
        }
        if (worldLoaded) {
            duskRunning = true;
            latched = false;
            startPose = GENERIC_DAY;
            duskStartMs = System.currentTimeMillis();
            lastLevel = Minecraft.getInstance() == null ? null : Minecraft.getInstance().level;
            return;
        }
        duskRunning = false;
        latched = true;
        startPose = null;
    }

    public static void applyNightSky(SkyRenderState state) {
        if (state == null || state.skybox != DimensionType.Skybox.OVERWORLD) {
            return;
        }
        FullbrightNightPolicy.SkyPose pose = currentPose(state);
        if (pose == null) {
            return;
        }
        state.sunAngle = pose.sunAngle();
        state.moonAngle = pose.moonAngle();
        state.starAngle = pose.starAngle();
        state.starBrightness = pose.starBrightness();
        state.skyColor = pose.skyColor();
        state.sunriseAndSunsetColor = pose.sunriseColor();
        state.moonPhase = MoonPhase.FULL_MOON;
    }

    public static void applyNightLightmap(LightmapRenderState state) {
        if (state == null) {
            return;
        }
        if (!FullbrightNightPolicy.applyNightLightmap(
                RotClientClient.isFullbrightEnabled(),
                RotClientClient.isAlwaysNightEnabled())) {
            return;
        }
        FullbrightNightPolicy.SkyPose pose = currentPose(null);
        if (pose == null) {
            return;
        }
        float t = cachedBlend;
        state.needsUpdate = true;
        state.skyFactor = pose.skyFactor();
        state.skyLightColor = new Vector3f(
                FullbrightNightPolicy.lerp(1.0F, FullbrightNightPolicy.NIGHT_SKY_LIGHT_R, t),
                FullbrightNightPolicy.lerp(1.0F, FullbrightNightPolicy.NIGHT_SKY_LIGHT_G, t),
                FullbrightNightPolicy.lerp(1.0F, FullbrightNightPolicy.NIGHT_SKY_LIGHT_B, t));
        state.ambientColor = new Vector3f(
                FullbrightNightPolicy.lerp(1.0F, FullbrightNightPolicy.NIGHT_AMBIENT_R, t),
                FullbrightNightPolicy.lerp(1.0F, FullbrightNightPolicy.NIGHT_AMBIENT_G, t),
                FullbrightNightPolicy.lerp(1.0F, FullbrightNightPolicy.NIGHT_AMBIENT_B, t));
    }

    private static FullbrightNightPolicy.SkyPose currentPose(SkyRenderState liveSky) {
        if (!FullbrightNightPolicy.applyNightSky(RotClientClient.isAlwaysNightEnabled())) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client == null ? null : client.level;
        long now = System.currentTimeMillis();
        if (cachedPose != null && poseMs == now && lastLevel == level) {
            if (liveSky != null && duskRunning && startPose == GENERIC_DAY) {
                startPose = capture(liveSky);
                cachedPose = FullbrightNightPolicy.lerpPose(
                        startPose,
                        FullbrightNightPolicy.SkyPose.parkedNight(liveSky.rainBrightness),
                        cachedBlend);
            }
            return cachedPose;
        }
        boolean sameWorld = lastLevel != null && lastLevel == level;
        FullbrightNightPolicy.NightApply apply = FullbrightNightPolicy.decideNightApply(
                RotClientClient.isAlwaysNightEnabled(),
                level != null,
                sameWorld,
                duskRunning,
                latched);
        if (apply == FullbrightNightPolicy.NightApply.OFF) {
            cachedPose = null;
            return null;
        }
        float rain = liveSky == null ? 1.0F : liveSky.rainBrightness;
        FullbrightNightPolicy.SkyPose target = FullbrightNightPolicy.SkyPose.parkedNight(rain);
        if (apply == FullbrightNightPolicy.NightApply.SNAP
                || apply == FullbrightNightPolicy.NightApply.LATCHED) {
            duskRunning = false;
            latched = true;
            startPose = null;
            lastLevel = level;
            cachedBlend = 1.0F;
            cachedPose = target;
            poseMs = now;
            return target;
        }
        if (startPose == GENERIC_DAY && liveSky != null) {
            startPose = capture(liveSky);
        } else if (startPose == null) {
            startPose = GENERIC_DAY;
        }
        float linear = FullbrightNightPolicy.duskProgress(
                now, duskStartMs, FullbrightNightPolicy.DUSK_DURATION_MS);
        cachedBlend = FullbrightNightPolicy.smoothstep(linear);
        if (linear >= 1.0F) {
            duskRunning = false;
            latched = true;
            startPose = null;
            lastLevel = level;
            cachedBlend = 1.0F;
            cachedPose = target;
            poseMs = now;
            return target;
        }
        lastLevel = level;
        cachedPose = FullbrightNightPolicy.lerpPose(startPose, target, cachedBlend);
        poseMs = now;
        return cachedPose;
    }

    private static FullbrightNightPolicy.SkyPose capture(SkyRenderState state) {
        return new FullbrightNightPolicy.SkyPose(
                state.sunAngle,
                state.moonAngle,
                state.starAngle,
                state.starBrightness,
                state.skyColor,
                state.sunriseAndSunsetColor,
                1.0F);
    }
}
