package corablue.stagehand.client;

import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

public class StageDimensionEffects extends DimensionEffects {
    public static final StageDimensionEffects INSTANCE = new StageDimensionEffects();

    public StageDimensionEffects() {
        // Horizon set low to prevent clipping
        super(-128.0F, false, DimensionEffects.SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
        Vec3d standardColor = color.multiply(sunHeight * 0.94F + 0.06F, sunHeight * 0.94F + 0.06F, sunHeight * 0.91F + 0.09F);

        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.cameraEntity != null) {
            double y = client.cameraEntity.getY();
            // Match the Mixin math for perfect color sync
            if (y < 32.0) {
                double fadeProgress = Math.max(0.0, Math.min(1.0, (32.0 - y) / 128.0));
                return standardColor.lerp(new Vec3d(1.0, 1.0, 1.0), fadeProgress);
            }
        }
        return standardColor;
    }

    @Override
    public boolean useThickFog(int camX, int camY) {
        return  false;
    }
}