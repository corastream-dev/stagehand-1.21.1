package corablue.stagehand.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

public class OmniParticle extends SpriteBillboardParticle {

    // Notice we added SpriteProvider here
    protected OmniParticle(ClientWorld world, double x, double y, double z,
                           double velocityX, double velocityY, double velocityZ,
                           float red, float green, float blue, float scale, float gravity, int lifetime,
                           SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);

        this.velocityX = velocityX; this.velocityY = velocityY; this.velocityZ = velocityZ;
        this.setColor(red, green, blue);
        this.scale = scale;
        this.gravityStrength = gravity;

        // --- ADD THESE TWO LINES ---
        // Give it a random starting rotation (0 to 360 degrees in radians)
        this.angle = this.random.nextFloat() * ((float)Math.PI * 2F);
        this.prevAngle = this.angle;
        // ---------------------------

        this.maxAge = lifetime;
        this.alpha = 1.0f;
        this.setSprite(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age > this.maxAge - (this.maxAge * 0.2f)) {
            this.alpha = 1.0f - ((float)(this.age - (this.maxAge * 0.2f)) / (this.maxAge * 0.2f));
        }
    }

    // --- THE FACTORY ---
    // Minecraft uses this to connect your custom data to the visual particle
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
                    parameters.red(), parameters.green(), parameters.blue(),
                    parameters.scale(), parameters.gravity(), parameters.lifetime(), // <-- Pass lifetime here
                    this.spriteProvider);
        }
    }
}