package corablue.stagehand.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.render.Camera;

public class OmniParticle extends SpriteBillboardParticle {

    private final float r1, g1, b1, r2, g2, b2;
    private final float orbX, orbY, orbZ;
    private final boolean rotate;
    private final float rotSpeed;
    private final boolean emissive;

    protected final SpriteProvider spriteProvider;

    // NEW: Define how many ticks it takes to complete one full animation loop.
    // 20 ticks = 1 second. If your JSON has 4 frames, it plays at 4 frames per second.
    // If you want it faster, lower this number (e.g., 10 for a half-second loop).
    private final int loopDuration = 16;

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
        this.gravityStrength = params.gravity();
        this.maxAge = params.lifetime();
        this.alpha = 1.0f;

        this.spriteProvider = spriteProvider;

        // FIXED: Set the initial frame using our loop logic instead of maxAge
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
        // Calculates how far away this specific particle is from the player's eyes
        return camera.getPos().squaredDistanceTo(this.x, this.y, this.z);
    }

    public static final ParticleTextureSheet SORTED_SHEET = new ParticleTextureSheet() {
        @SuppressWarnings("deprecation")
        @Override
        public BufferBuilder begin(Tessellator tessellator, TextureManager textureManager) {
            RenderSystem.depthMask(true); // True fixes the clouds rendering over them!
            RenderSystem.setShaderTexture(0, SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE);
            RenderSystem.enableBlend();

            // Standard transparency! No washed-out additive glow.
            RenderSystem.defaultBlendFunc();

            return tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        }

        @Override
        public String toString() {
            return "SORTED_SHEET";
        }
    };

    @Override
    public int getBrightness(float tintMultiplier) {
        // 15728880 is the magic number for full block light and full sky light (15 << 20 | 15 << 4)
        return this.emissive ? 15728880 : super.getBrightness(tintMultiplier);
    }

    @Override
    public ParticleTextureSheet getType() {
        return this.emissive ? SORTED_SHEET : ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();

        // FIXED: Loop the animation!
        // this.age % loopDuration resets to 0 every time it hits the loop duration.
        if (!this.dead) {
            this.setSprite(this.spriteProvider.getSprite(this.age % loopDuration, loopDuration));
        }

        // 1. Color Interpolation
        float progress = (float)this.age / this.maxAge;
        float cr = MathHelper.lerp(progress, r1, r2);
        float cg = MathHelper.lerp(progress, g1, g2);
        float cb = MathHelper.lerp(progress, b1, b2);
        this.setColor(cr, cg, cb);

        // 2. Alpha Fadeout (last 20%)
        if (this.age > this.maxAge - (this.maxAge * 0.2f)) {
            this.alpha = 1.0f - ((float)(this.age - (this.maxAge * 0.2f)) / (this.maxAge * 0.2f));
        }

        // 3. Lifetime Rotation
        if (this.rotate) {
            this.prevAngle = this.angle;
            this.angle += this.rotSpeed;
        }

        // 4. Orbital Velocity (Rotate Velocity Vector)
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