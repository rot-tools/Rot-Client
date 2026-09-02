package fi.rotclient.mixin;

import fi.rotclient.FishingSuiteRuntime;
import fi.rotclient.ForagingRuntime;
import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Local explosion particle suppression for Render Optimizer.
 */
@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void rotclient$suppressParticles(
            ParticleOptions options,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            CallbackInfoReturnable<Particle> cir) {
        FishingSuiteRuntime.observeParticle(options, x, y, z, xSpeed, ySpeed, zSpeed);
        if (ForagingRuntime.shouldHideParticle(options, x, y, z)
                || QolVisualRuntime.shouldSuppressParticle(options, x, y, z)) {
            cir.setReturnValue(null);
        }
    }
}
