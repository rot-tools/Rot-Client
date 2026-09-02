package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

/** Safe client-state boundary for player animal model substitution. */
public final class PlayerAnimalsRuntime {
    private PlayerAnimalsRuntime() {
    }

    public static Appearance appearance(AvatarRenderState state) {
        Minecraft client = Minecraft.getInstance();
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        if (state == null
                || client == null
                || client.player == null
                || config == null
                || !config.playerSizeEnabled
                || !PlayerAnimalPolicy.applies(
                        config.playerAnimalsEnabled,
                        config.playerAnimalsScope,
                        state.id,
                        client.player.getId())) {
            return null;
        }
        String species = PlayerAnimalPolicy.normalizeSpecies(config.playerAnimalsSpecies);
        return new Appearance(
                species,
                Identifier.withDefaultNamespace(
                        PlayerAnimalPolicy.texture(species, config.playerAnimalsBaby)),
                config.playerAnimalsBaby,
                config.playerAnimalsCollarColor);
    }

    public record Appearance(
            String species, Identifier texture, boolean baby, int collarColor) {
        public boolean felineOrWolf() {
            return "Cat".equals(species) || "Wolf".equals(species);
        }

        public Identifier collarTexture() {
            return Identifier.withDefaultNamespace(
                    "Cat".equals(species)
                            ? "textures/entity/cat/cat_collar"
                                    + (baby ? "_baby" : "") + ".png"
                            : "textures/entity/wolf/wolf_collar"
                                    + (baby ? "_baby" : "") + ".png");
        }
    }
}
