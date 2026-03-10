package corablue.stagehand.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public class DirectionalOmniParticle extends OmniParticle {

    protected DirectionalOmniParticle(ClientWorld world, double x, double y, double z,
                                      double velocityX, double velocityY, double velocityZ,
                                      OmniParticleEffect params,
                                      SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ, params, spriteProvider);
    }

    @Override
    public void buildGeometry(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        Vec3d camPos = camera.getPos();
        float renderX = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - camPos.getX());
        float renderY = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - camPos.getY());
        float renderZ = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - camPos.getZ());
        Vector3f up = new Vector3f((float)this.velocityX, (float)this.velocityY, (float)this.velocityZ);

        if (up.lengthSquared() < 0.0001f) {
            up.set(0, -1, 0);
        } else {
            up.normalize();
        }

        Vector3f toCamera = new Vector3f(-renderX, -renderY, -renderZ);
        toCamera.normalize();
        Vector3f right = new Vector3f(toCamera).cross(up);

        if (right.lengthSquared() < 0.0001f) {
            right.set(1, 0, 0);
        } else {
            right.normalize();
        }

        //Dynamic Stretching
        float visualScale = this.getSize(tickDelta);
        float speed = (float) new Vector3f((float)this.velocityX, (float)this.velocityY, (float)this.velocityZ).length();
        float stretchFactor = 1.0f + (speed * 1.5f);
        float lengthScale = visualScale * stretchFactor;

        //AAAHHHH
        Vector3f[] corners = new Vector3f[]{
                new Vector3f(renderX - right.x * visualScale - up.x * lengthScale,
                        renderY - right.y * visualScale - up.y * lengthScale,
                        renderZ - right.z * visualScale - up.z * lengthScale),

                new Vector3f(renderX - right.x * visualScale + up.x * lengthScale,
                        renderY - right.y * visualScale + up.y * lengthScale,
                        renderZ - right.z * visualScale + up.z * lengthScale),

                new Vector3f(renderX + right.x * visualScale + up.x * lengthScale,
                        renderY + right.y * visualScale + up.y * lengthScale,
                        renderZ + right.z * visualScale + up.z * lengthScale),

                new Vector3f(renderX + right.x * visualScale - up.x * lengthScale,
                        renderY + right.y * visualScale - up.y * lengthScale,
                        renderZ + right.z * visualScale - up.z * lengthScale)
        };

        float minU = this.getMinU(); float maxU = this.getMaxU();
        float minV = this.getMinV(); float maxV = this.getMaxV();
        int light = this.getBrightness(tickDelta);

        vertexConsumer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).texture(maxU, maxV).color(this.red, this.green, this.blue, this.alpha).light(light);
        vertexConsumer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).texture(maxU, minV).color(this.red, this.green, this.blue, this.alpha).light(light);
        vertexConsumer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).texture(minU, minV).color(this.red, this.green, this.blue, this.alpha).light(light);
        vertexConsumer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).texture(minU, maxV).color(this.red, this.green, this.blue, this.alpha).light(light);
    }

    public static class Factory implements ParticleFactory<OmniParticleEffect> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) { this.spriteProvider = spriteProvider; }

        @Override
        public Particle createParticle(OmniParticleEffect parameters, ClientWorld world,
                                       double x, double y, double z,
                                       double velocityX, double velocityY, double velocityZ) {
            return new DirectionalOmniParticle(world, x, y, z, velocityX, velocityY, velocityZ,
                    parameters,
                    this.spriteProvider);
        }
    }
}