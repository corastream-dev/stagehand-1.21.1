package corablue.stagehand.network;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ParticleEmitterUpdatePacket(
        BlockPos pos,
        Identifier particleType, // <-- NEW!
        float r1, float g1, float b1,
        float r2, float g2, float b2,
        float scale, float gravity, int amount, int lifetime,
        float oX, float oY, float oZ,
        float aX, float aY, float aZ,
        float minVX, float maxVX, float minVY, float maxVY, float minVZ, float maxVZ
) {}