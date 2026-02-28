package corablue.stagehand.mixin;

import com.google.common.collect.EvictingQueue;
import com.mojang.blaze3d.systems.RenderSystem;
import corablue.stagehand.client.particle.OmniParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Shadow private Map<ParticleTextureSheet, Queue<Particle>> particles;
    @Shadow private TextureManager textureManager;

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"))
    private void captureSortedParticles(Particle particle, CallbackInfo ci) {
        // Acknowledge our new SORTED_SHEET bittttttch
        if (particle.getType() == OmniParticle.SORTED_SHEET && !this.particles.containsKey(OmniParticle.SORTED_SHEET)) {
            this.particles.put(OmniParticle.SORTED_SHEET, EvictingQueue.create(16384));
        }
    }

    @Inject(method = "renderParticles", at = @At("TAIL"))
    private void renderSortedParticles(LightmapTextureManager lightmapTextureManager, Camera camera, float tickDelta, CallbackInfo ci) {
        Queue<Particle> queue = this.particles.get(OmniParticle.SORTED_SHEET);

        if (queue != null && !queue.isEmpty()) {

            lightmapTextureManager.enable();
            RenderSystem.setShader(GameRenderer::getParticleProgram);
            Tessellator tessellator = Tessellator.getInstance();

            BufferBuilder bufferBuilder = OmniParticle.SORTED_SHEET.begin(tessellator, this.textureManager);

            if (bufferBuilder != null) {
                //Extract particles into a list
                List<OmniParticle> sortedParticles = new ArrayList<>();
                for (Particle p : queue) {
                    if (p instanceof OmniParticle op) {
                        sortedParticles.add(op);
                    }
                }

                //SORT BACK-TO-FRONT (Painter's Algorithm)
                sortedParticles.sort((p1, p2) -> Double.compare(
                        p2.getSquaredDistance(camera),
                        p1.getSquaredDistance(camera)
                ));

                //Draw them in perfect order!
                for (OmniParticle particle : sortedParticles) {
                    particle.buildGeometry(bufferBuilder, camera, tickDelta);
                }

                BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            }

            lightmapTextureManager.disable();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }
}