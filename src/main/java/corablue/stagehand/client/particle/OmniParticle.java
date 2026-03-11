package corablue.stagehand.client.particle;

import corablue.stagehand.particles.ModParticles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.render.Camera;
import org.jetbrains.annotations.Nullable;

public class OmniParticle extends SpriteBillboardParticle {

    // --- Custom 1.21.1 Texture Sheets ---

    public static final ParticleTextureSheet PARTICLE_SHEET_ADDITIVE = new ParticleTextureSheet() {
        @Nullable
        @Override
        public BufferBuilder begin(Tessellator tessellator, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
            RenderSystem.enableBlend();
            // Source Alpha + One = Glowing/Bright Overlap
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            return tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        }

        @Override
        public String toString() {
            return "STAGEHAND_ADDITIVE";
        }
    };

    public static final ParticleTextureSheet PARTICLE_SHEET_SOFT_TRANSLUCENT = new ParticleTextureSheet() {
        @Nullable
        @Override
        public BufferBuilder begin(Tessellator tessellator, TextureManager textureManager) {
            RenderSystem.depthMask(false); // Prevents muddy intersection clipping
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
            RenderSystem.enableBlend();
            // Standard Blending = Allows dark colors, but smooth overlap due to depth mask
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
            return tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        }

        @Override
        public String toString() {
            return "STAGEHAND_SOFT_TRANSLUCENT";
        }
    };

    @Override
    public float getSize(float tickDelta) {
        float exactAge = this.age + tickDelta;
        float fadeStart = this.maxAge * 0.8f;
        float fadeDuration = this.maxAge * 0.2f;

        // If in the final 20% of life, shrink to 0
        if (exactAge > fadeStart) {
            float fadeMultiplier = 1.0f - ((exactAge - fadeStart) / fadeDuration);
            return this.baseScale * Math.max(0.0f, fadeMultiplier);
        }

        return this.baseScale;
    }

    // --- Particle Variables ---

    private final float r1, g1, b1, r2, g2, b2;
    private final float orbX, orbY, orbZ;
    private final boolean rotate;
    private final float rotSpeed;
    private final boolean emissive;
    private final int loopDuration = 16;
    private final float baseScale; // Tracks initial size for the shrink trick
    protected final SpriteProvider spriteProvider;

    protected OmniParticle(ClientWorld world, double x, double y, double z,
                           double velocityX, double velocityY, double velocityZ,
                           OmniParticleEffect params,
                           SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);

        this.velocityX = velocityX; this.velocityY = velocityY; this.velocityZ = velocityZ;

        this.r1 = params.r1(); this.g1 = params.g1(); this.b1 = params.b1();
        this.r2 = params.r2(); this.g2 = params.g2(); this.b2 = params.b2();

        this.orbX = params.orbX(); this.orbY = params.orbY(); this.orbZ = params.orbZ();
        this.rotate = params.rotate();
        this.emissive = params.emissive();

        this.setColor(r1, g1, b1);

        this.scale = params.scale();

        //Weird case, people wanna use fog for large haze-like effects
        if (params.getType() == ModParticles.OMNI_FOG) {
            this.scale *= 6.0f;
        }

        // Save the scale after any modifications so we can scale relative to it later
        this.baseScale = this.scale;

        this.gravityStrength = params.gravity();
        this.maxAge = params.lifetime();
        this.alpha = 1.0f;

        this.spriteProvider = spriteProvider;

        this.setSprite(this.spriteProvider.getSprite(0, loopDuration));

        if (this.rotate) {
            this.angle = this.random.nextFloat() * ((float)Math.PI * 2F);
            this.rotSpeed = (this.random.nextFloat() - 0.5f) * 0.2f;
        } else {
            this.angle = 0;
            this.rotSpeed = 0;
        }
        this.prevAngle = this.angle;
    }

    public double getSquaredDistance(Camera camera) {
        return camera.getPos().squaredDistanceTo(this.x, this.y, this.z);
    }

    @Override
    public int getBrightness(float tintMultiplier) {
        return this.emissive ? 15728880 : super.getBrightness(tintMultiplier);
    }

    @Override
    public ParticleTextureSheet getType() {
        // Automatically route emissive particles to Additive, and normal/dark particles to Soft Translucent
        return this.emissive ? PARTICLE_SHEET_ADDITIVE : PARTICLE_SHEET_SOFT_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();

        // Animation loop
        if (!this.dead) {
            this.setSprite(this.spriteProvider.getSprite(this.age % loopDuration, loopDuration));
        }

        // Base Color Interpolation
        float progress = (float)this.age / this.maxAge;
        float cr = MathHelper.lerp(progress, r1, r2);
        float cg = MathHelper.lerp(progress, g1, g2);
        float cb = MathHelper.lerp(progress, b1, b2);

        // Calculate a correct 1.0 to 0.0 fade multiplier for the final 20% of life
        float fadeMultiplier = 1.0f;
        float fadeStartAge = this.maxAge * 0.8f;
        float fadeDuration = this.maxAge * 0.2f;

        if (this.age > fadeStartAge) {
            fadeMultiplier = 1.0f - ((this.age - fadeStartAge) / fadeDuration);
        }
        fadeMultiplier = Math.max(0.0f, fadeMultiplier);

        // Apply Blending-Specific Fade Logic
        if (this.getType() == PARTICLE_SHEET_ADDITIVE) {
            // Emissive/Additive: Fade RGB to black, keep Alpha safely at 1.0
            this.setColor(cr * fadeMultiplier, cg * fadeMultiplier, cb * fadeMultiplier);
            this.alpha = 1.0f;
        } else {
            // Dark/Translucent: Keep RGB, clamp Alpha strictly at 11% so it doesn't pop
            this.setColor(cr, cg, cb);
            if (fadeMultiplier < 1.0f) {
                this.alpha = Math.max(0.11f, fadeMultiplier);
            } else {
                this.alpha = 1.0f;
            }
        }

        // Rotation
        if (this.rotate) {
            this.prevAngle = this.angle;
            this.angle += this.rotSpeed;
        }

        // Orbital Velocity
        if (orbX != 0 || orbY != 0 || orbZ != 0) {
            Vector3f vel = new Vector3f((float)this.velocityX, (float)this.velocityY, (float)this.velocityZ);
            if (orbX != 0) vel.rotateX(orbX);
            if (orbY != 0) vel.rotateY(orbY);
            if (orbZ != 0) vel.rotateZ(orbZ);

            this.velocityX = vel.x;
            this.velocityY = vel.y;
            this.velocityZ = vel.z;
        }
    }

    public static class Factory implements ParticleFactory<OmniParticleEffect> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(OmniParticleEffect parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new OmniParticle(world, x, y, z, velocityX, velocityY, velocityZ,
                    parameters,
                    this.spriteProvider);
        }
    }
}