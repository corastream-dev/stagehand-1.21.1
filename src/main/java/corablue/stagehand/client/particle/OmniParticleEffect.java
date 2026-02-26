package corablue.stagehand.client.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

public record OmniParticleEffect(
        ParticleType<OmniParticleEffect> type,
        float r1, float g1, float b1,
        float r2, float g2, float b2,
        float scale, float gravity,
        int lifetime,
        float orbX, float orbY, float orbZ,
        boolean rotate,
        boolean emissive
) implements ParticleEffect {

    // --- JSON CODEC (For /particle command) ---
    public static MapCodec<OmniParticleEffect> createCodec(ParticleType<OmniParticleEffect> particleType) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.FLOAT.fieldOf("r1").forGetter(OmniParticleEffect::r1),
                Codec.FLOAT.fieldOf("g1").forGetter(OmniParticleEffect::g1),
                Codec.FLOAT.fieldOf("b1").forGetter(OmniParticleEffect::b1),
                Codec.FLOAT.fieldOf("r2").forGetter(OmniParticleEffect::r2),
                Codec.FLOAT.fieldOf("g2").forGetter(OmniParticleEffect::g2),
                Codec.FLOAT.fieldOf("b2").forGetter(OmniParticleEffect::b2),
                Codec.FLOAT.fieldOf("scale").forGetter(OmniParticleEffect::scale),
                Codec.FLOAT.fieldOf("gravity").forGetter(OmniParticleEffect::gravity),
                Codec.INT.fieldOf("lifetime").forGetter(OmniParticleEffect::lifetime),
                Codec.FLOAT.fieldOf("orbX").forGetter(OmniParticleEffect::orbX),
                Codec.FLOAT.fieldOf("orbY").forGetter(OmniParticleEffect::orbY),
                Codec.FLOAT.fieldOf("orbZ").forGetter(OmniParticleEffect::orbZ),
                Codec.BOOL.fieldOf("rotate").forGetter(OmniParticleEffect::rotate),
                Codec.BOOL.fieldOf("emissive").forGetter(OmniParticleEffect::emissive)
        ).apply(instance, (r1, g1, b1, r2, g2, b2, s, grav, life, ox, oy, oz, rot, emissive) ->
                new OmniParticleEffect(particleType, r1, g1, b1, r2, g2, b2, s, grav, life, ox, oy, oz, rot, emissive)));
    }

    // --- PACKET CODEC (Network Sync) ---
    // Fixed: Implemented manually to bypass tuple limit
    public static PacketCodec<RegistryByteBuf, OmniParticleEffect> createPacketCodec(ParticleType<OmniParticleEffect> particleType) {
        return PacketCodec.of(
                (value, buf) -> {
                    buf.writeFloat(value.r1); buf.writeFloat(value.g1); buf.writeFloat(value.b1);
                    buf.writeFloat(value.r2); buf.writeFloat(value.g2); buf.writeFloat(value.b2);
                    buf.writeFloat(value.scale);
                    buf.writeFloat(value.gravity);
                    buf.writeInt(value.lifetime);
                    buf.writeFloat(value.orbX); buf.writeFloat(value.orbY); buf.writeFloat(value.orbZ);
                    buf.writeBoolean(value.rotate);
                    buf.writeBoolean(value.emissive);
                },
                (buf) -> new OmniParticleEffect(
                        particleType,
                        buf.readFloat(), buf.readFloat(), buf.readFloat(),
                        buf.readFloat(), buf.readFloat(), buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readInt(),
                        buf.readFloat(), buf.readFloat(), buf.readFloat(),
                        buf.readBoolean(),
                        buf.readBoolean()
                )
        );
    }

    @Override
    public ParticleType<?> getType() { return this.type; }
}