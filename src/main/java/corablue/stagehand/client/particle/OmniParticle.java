package corablue.stagehand.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

public class OmniParticle extends SpriteBillboardParticle {

    private final float r1, g1, b1, r2, g2, b2;
    private final float orbX, orbY, orbZ;
    private final boolean rotate;
    private final float rotSpeed;

    protected OmniParticle(ClientWorld world, double x, double y, double z,
                           double velocityX, double velocityY, double velocityZ,
                           OmniParticleEffect params,
                           SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);

        this.velocityX = velocityX; this.velocityY = velocityY; this.velocityZ = velocityZ;

        // Store color start/end
        this.r1 = params.r1(); this.g1 = params.g1(); this.b1 = params.b1();
        this.r2 = params.r2(); this.g2 = params.g2(); this.b2 = params.b2();

        this.orbX = params.orbX(); this.orbY = params.orbY(); this.orbZ = params.orbZ();
        this.rotate = params.rotate();

        // Initialize at Start Color
        this.setColor(r1, g1, b1);

        this.scale = params.scale();
        this.gravityStrength = params.gravity();
        this.maxAge = params.lifetime();
        this.alpha = 1.0f;
        this.setSprite(spriteProvider);

        // Rotation Init
        if (this.rotate) {
            this.angle = this.random.nextFloat() * ((float)Math.PI * 2F);
            // Random spin speed between -0.1 and 0.1 rad/tick
            this.rotSpeed = (this.random.nextFloat() - 0.5f) * 0.2f;
        } else {
            this.angle = 0;
            this.rotSpeed = 0;
        }
        this.prevAngle = this.angle;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();

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
            // Apply rotations
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
                    parameters, // Pass the whole effect object
                    this.spriteProvider);
        }
    }
}