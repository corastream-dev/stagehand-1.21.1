package corablue.stagehand.client.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

public record OmniParticleEffect(
        ParticleType<OmniParticleEffect> type,
        float red, float green, float blue,
        float scale, float gravity,
        int lifetime // <-- NEW VARIABLE
) implements ParticleEffect {

    public static MapCodec<OmniParticleEffect> createCodec(ParticleType<OmniParticleEffect> particleType) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.FLOAT.fieldOf("red").forGetter(OmniParticleEffect::red),
                Codec.FLOAT.fieldOf("green").forGetter(OmniParticleEffect::green),
                Codec.FLOAT.fieldOf("blue").forGetter(OmniParticleEffect::blue),
                Codec.FLOAT.fieldOf("scale").forGetter(OmniParticleEffect::scale),
                Codec.FLOAT.fieldOf("gravity").forGetter(OmniParticleEffect::gravity),
                Codec.INT.fieldOf("lifetime").forGetter(OmniParticleEffect::lifetime) // <-- ADDED
        ).apply(instance, (r, g, b, s, grav, life) -> new OmniParticleEffect(particleType, r, g, b, s, grav, life)));
    }

    public static PacketCodec<RegistryByteBuf, OmniParticleEffect> createPacketCodec(ParticleType<OmniParticleEffect> particleType) {
        return PacketCodec.tuple(
                PacketCodecs.FLOAT, OmniParticleEffect::red,
                PacketCodecs.FLOAT, OmniParticleEffect::green,
                PacketCodecs.FLOAT, OmniParticleEffect::blue,
                PacketCodecs.FLOAT, OmniParticleEffect::scale,
                PacketCodecs.FLOAT, OmniParticleEffect::gravity,
                PacketCodecs.INTEGER, OmniParticleEffect::lifetime, // <-- ADDED
                (r, g, b, s, grav, life) -> new OmniParticleEffect(particleType, r, g, b, s, grav, life)
        );
    }

    @Override
    public ParticleType<?> getType() { return this.type; }
}