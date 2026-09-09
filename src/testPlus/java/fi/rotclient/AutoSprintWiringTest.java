package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Source wiring checks for Auto Sprint + Camera QoL. */
final class AutoSprintWiringTest {
    @Test
    void localPlayerMixinUsesSprintOverrideTarget() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/LocalPlayerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("drop(Z)Z"));
        assertTrue(source.contains("ItemProtectRuntime.shouldBlockDrop"));
        assertTrue(source.contains(
                "Lnet/minecraft/world/entity/player/Input;sprint()Z"));
        assertTrue(source.contains("AutoSprintPolicy.resolveSprintInput"));
        assertTrue(source.contains("isAutoSprintEnabled"));
        assertFalse(source.contains("isConnectedToHypixel"));
        assertFalse(source.contains("setSprinting"));
        assertFalse(source.contains("BYPASS_HYPIXEL"));
    }

    @Test
    void policyHasNoHypixelGate() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/fi/rotclient/AutoSprintPolicy.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("originalSprintInput || moduleEnabled"));
        assertFalse(source.contains("onHypixel"));
        assertFalse(source.contains("BYPASS_HYPIXEL"));
        assertFalse(source.contains("isEffectivelyEnabled"));
    }

    @Test
    void stubModuleFilesAreNotPresent() {
        assertFalse(Files.exists(Path.of(
                "src/client/java/fi/rotclient/AutoSprintMod.java")));
        assertFalse(Files.exists(Path.of(
                "src/client/java/fi/rotclient/AutoSprintPolicy.java")));
        assertFalse(Files.exists(Path.of(
                "src/client/java/fi/rotclient/Hypixeldetector.java")));
        assertFalse(Files.exists(Path.of(
                "src/main/java/fi/rotclient/modules")));
        assertFalse(Files.exists(Path.of(
                "src/client/java/fi/rotclient/modules")));
    }

    @Test
    void mixinJsonRegistersLocalPlayerMixin() throws Exception {
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("LocalPlayerMixin"));
        String plusJson = Files.readString(Path.of(
                "src/plusClient/resources/rotclient.plus.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(plusJson.contains("OptionsCameraMixin"));
    }

    @Test
    void qolUiExposesAutoSprintToggleWithoutHypixelBlock() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/MiningUiScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("QolUtilityDashboard"));
        assertTrue(source.contains("qolDashboard"));
        assertTrue(!source.contains("Unavailable on Hypixel"));
        assertTrue(!source.contains("AUTO SPRINT  ·  BLOCKED"));

        String catalog = Files.readString(Path.of(
                "src/main/java/fi/rotclient/QolUtilityCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("Auto Sprint"));
        assertTrue(catalog.contains("qol.auto_sprint"));
        String plusCatalog = Files.readString(Path.of(
                "src/plus/java/fi/rotclient/QolPlusCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(plusCatalog.contains("Camera"));
    }

    @Test
    void optionsCameraMixinSkipsFrontPerspective() throws Exception {
        String source = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/mixin/OptionsCameraMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("setCameraType"));
        assertTrue(source.contains("CameraPolicy.resolve"));
        assertTrue(source.contains("isCameraEnabled"));
        assertTrue(source.contains("skipFrontPerspective"));
        assertTrue(source.contains("FIRST_PERSON"));
        assertTrue(source.contains("THIRD_PERSON_BACK"));
        assertTrue(source.contains("THIRD_PERSON_FRONT"));
    }
}
