package corablue.stagehand.particles;

import com.mojang.serialization.MapCodec;
import corablue.stagehand.client.particle.DirectionalOmniParticle;
import corablue.stagehand.client.particle.OmniParticleEffect;
import corablue.stagehand.client.particle.OmniParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ModParticles {

    // TWO hidden lists to keep things organized!
    private static final List<ParticleType<OmniParticleEffect>> BILLBOARD_PARTICLES = new ArrayList<>();
    private static final List<ParticleType<OmniParticleEffect>> DIRECTIONAL_PARTICLES = new ArrayList<>();

    // 1. Declare your custom particles here!
    public static final ParticleType<OmniParticleEffect> OMNI_SPARK = registerBillboard("omni_spark", false);
    public static final ParticleType<OmniParticleEffect> OMNI_FOG = registerBillboard("omni_fog", false);
    public static final ParticleType<OmniParticleEffect> OMNI_DOT = registerBillboard("omni_dot", false);
    public static final ParticleType<OmniParticleEffect> OMNI_HEART = registerBillboard("omni_heart", false);
    public static final ParticleType<OmniParticleEffect> OMNI_SWIRL = registerBillboard("omni_swirl", false);
    public static final ParticleType<OmniParticleEffect> OMNI_BUBBLE = registerBillboard("omni_bubble", false);

    // Use the new Directional method for rain, lasers, or falling debris!
    public static final ParticleType<OmniParticleEffect> OMNI_RAIN = registerDirectional("omni_rain", false);
    public static final ParticleType<OmniParticleEffect> OMNI_DOT_DIR = registerDirectional("omni_dot_dir", false);
    public static final ParticleType<OmniParticleEffect> OMNI_BOLT = registerDirectional("omni_bolt", false);
    public static final ParticleType<OmniParticleEffect> OMNI_FLAME = registerDirectional("omni_flame", false);
    public static final ParticleType<OmniParticleEffect> OMNI_GHOST = registerDirectional("omni_ghost", false);

    // --- Helper Methods ---
    private static ParticleType<OmniParticleEffect> registerBillboard(String name, boolean alwaysShow) {
        ParticleType<OmniParticleEffect> type = createType(name, alwaysShow);
        BILLBOARD_PARTICLES.add(type);
        return type;
    }

    private static ParticleType<OmniParticleEffect> registerDirectional(String name, boolean alwaysShow) {
        ParticleType<OmniParticleEffect> type = createType(name, alwaysShow);
        DIRECTIONAL_PARTICLES.add(type);
        return type;
    }

    // Shared logic so we don't repeat the Codec mapping code
    private static ParticleType<OmniParticleEffect> createType(String name, boolean alwaysShow) {
        return Registry.register(
                Registries.PARTICLE_TYPE,
                Identifier.of("stagehand", name),
                new ParticleType<OmniParticleEffect>(alwaysShow) {
                    @Override public MapCodec<OmniParticleEffect> getCodec() { return OmniParticleEffect.createCodec(this); }
                    @Override public PacketCodec<RegistryByteBuf, OmniParticleEffect> getPacketCodec() { return OmniParticleEffect.createPacketCodec(this); }
                }
        );
    }

    public static void registerParticles() {
        System.out.println("Registering Custom Particles for Stagehand...");
    }

    // Client Initialization loop
    @Environment(EnvType.CLIENT)
    public static void registerClientFactories() {
        // Register the standard fluffy ones
        for (ParticleType<OmniParticleEffect> particle : BILLBOARD_PARTICLES) {
            ParticleFactoryRegistry.getInstance().register(particle, OmniParticle.Factory::new);
        }
        // Register the directional moving ones
        for (ParticleType<OmniParticleEffect> particle : DIRECTIONAL_PARTICLES) {
            ParticleFactoryRegistry.getInstance().register(particle, DirectionalOmniParticle.Factory::new);
        }
    }
}