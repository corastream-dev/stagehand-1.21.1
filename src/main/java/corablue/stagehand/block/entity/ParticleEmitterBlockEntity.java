package corablue.stagehand.block.entity;

import corablue.stagehand.block.custom.ParticleEmitterBlock;
import corablue.stagehand.client.particle.OmniParticleEffect;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ParticleEmitterBlockEntity extends BlockEntity {

    // --- Settings / Default Values ---
    private UUID owner = null;
    private boolean isActive = true;
    private Identifier particleType = Identifier.of("stagehand", "omni_spark");

    // Colors
    private float c1R = 1.0f; private float c1G = 1.0f; private float c1B = 1.0f;
    private float c2R = 1.0f; private float c2G = 1.0f; private float c2B = 1.0f;
    private boolean useLifetimeColor = false; // Toggle: Random vs Lifetime

    // Spawning Area & Offsets
    private float offsetX = 0.0f; private float offsetY = 0.5f; private float offsetZ = 0.0f;
    private float areaX = 0.5f; private float areaY = 0.5f; private float areaZ = 0.5f;

    // Behavior
    private double amountPerTick = 1;
    private double spawnAccumulator = 0;
    private int lifetime = 40;
    private float scale = 1.0f;
    private float gravity = 0.0f;
    private boolean rotate = false;
    private boolean emissive = false;

    // Velocity Ranges
    private float minVelX = -0.05f; private float maxVelX = 0.05f;
    private float minVelY = 0.02f;  private float maxVelY = 0.08f;
    private float minVelZ = -0.05f; private float maxVelZ = 0.05f;

    // Orbital Velocity (Angular velocity of vector)
    private float orbX = 0.0f; private float orbY = 0.0f; private float orbZ = 0.0f;

    public ParticleEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARTICLE_EMITTER_BE, pos, state);
    }

    // --- Getters ---
    public Identifier getParticleType() { return this.particleType; }
    public float getC1R() { return this.c1R; } public float getC1G() { return this.c1G; } public float getC1B() { return this.c1B; }
    public float getC2R() { return this.c2R; } public float getC2G() { return this.c2G; } public float getC2B() { return this.c2B; }
    public boolean getUseLifetimeColor() { return this.useLifetimeColor; }

    public float getOffsetX() { return this.offsetX; } public float getOffsetY() { return this.offsetY; } public float getOffsetZ() { return this.offsetZ; }
    public float getAreaX() { return this.areaX; } public float getAreaY() { return this.areaY; } public float getAreaZ() { return this.areaZ; }
    public float getMinVelX() { return this.minVelX; } public float getMaxVelX() { return this.maxVelX; }
    public float getMinVelY() { return this.minVelY; } public float getMaxVelY() { return this.maxVelY; }
    public float getMinVelZ() { return this.minVelZ; } public float getMaxVelZ() { return this.maxVelZ; }

    public float getOrbX() { return this.orbX; } public float getOrbY() { return this.orbY; } public float getOrbZ() { return this.orbZ; }

    public float getScale() { return this.scale; }
    public float getGravity() { return this.gravity; }
    public double getAmountPerTick() { return this.amountPerTick; }
    public int getLifetime() { return this.lifetime; }
    public boolean getRotate() { return this.rotate; }
    public boolean getEmissive() { return this.emissive; }

    // --- Security ---
    public void setOwner(UUID uuid) {
        this.owner = uuid;
        this.markDirtyAndSync();
    }
    public boolean isOwner(PlayerEntity player) {
        return this.owner != null && this.owner.equals(player.getUuid());
    }

    // --- Client Tick ---
    public static void clientTick(World world, BlockPos pos, BlockState state, ParticleEmitterBlockEntity be) {

        // ==========================================
        // 1. DECORATIVE HOLOGRAM (Smooth + Sine Wave)
        // ==========================================
        ParticleType<?> rawDotType = Registries.PARTICLE_TYPE.get(Identifier.of("stagehand", "omni_dot"));
        if (rawDotType != null) {
            @SuppressWarnings("unchecked")
            ParticleType<OmniParticleEffect> omniDotType = (ParticleType<OmniParticleEffect>) rawDotType;

            double radius = 0.25;
            double orbitSpeed = 0.1;

            // This is the distance "out" from the center of the block
            double baseForwardOffset = 0.3;

            // Sine Wave variables
            double sineSpeed = 0.1;
            double sineAmplitude = 0.1;

            long time = world.getTime();

            // 1. Get the Facing Direction
            Direction facing = state.get(ParticleEmitterBlock.FACING);

            for (int i = 0; i < 3; i++) {
                double offset = i * (2 * Math.PI / 3);

                // --- CALCULATE LOCAL OFFSETS ---
                double curAngle = (time * orbitSpeed) + offset;
                double localX = Math.cos(curAngle) * radius;
                double localY = baseForwardOffset + (Math.sin(time * sineSpeed) * sineAmplitude);
                double localZ = Math.sin(curAngle) * radius;

                double nextAngle = ((time + 1) * orbitSpeed) + offset;
                double nextLocalX = Math.cos(nextAngle) * radius;
                double nextLocalY = baseForwardOffset + (Math.sin((time + 1) * sineSpeed) * sineAmplitude);
                double nextLocalZ = Math.sin(nextAngle) * radius;

                // --- ROTATE TO WORLD SPACE ---
                double[] curPos = rotateVector(localX, localY, localZ, facing);
                double curWorldX = pos.getX() + 0.5 + curPos[0];
                double curWorldY = pos.getY() + 0.5 + curPos[1];
                double curWorldZ = pos.getZ() + 0.5 + curPos[2];

                double[] nextPos = rotateVector(nextLocalX, nextLocalY, nextLocalZ, facing);
                double nextWorldX = pos.getX() + 0.5 + nextPos[0];
                double nextWorldY = pos.getY() + 0.5 + nextPos[1];
                double nextWorldZ = pos.getZ() + 0.5 + nextPos[2];

                // --- CALCULATE VELOCITY ---
                double velX = nextWorldX - curWorldX;
                double velY = nextWorldY - curWorldY;
                double velZ = nextWorldZ - curWorldZ;

                // FIXED: Use the new constructor with all arguments
                // r1, g1, b1 (Cyan), r2, g2, b2 (Cyan), scale, gravity, lifetime, orbX, orbY, orbZ, rotate
                OmniParticleEffect hologramEffect = new OmniParticleEffect(
                        omniDotType,
                        0.0f, 1.0f, 1.0f, // Start Color
                        0.0f, 1.0f, 1.0f, // End Color
                        0.25f, 0.0f, 2,   // Scale, Gravity, Lifetime
                        0.0f, 0.0f, 0.0f, // Orbital Velocity (None)
                        false, true             // Rotate, Emissive
                );

                world.addParticle(hologramEffect, curWorldX, curWorldY, curWorldZ, velX, velY, velZ);
            }
        }

        // ==========================================
        // 2. FUNCTIONAL EMITTER
        // ==========================================

        if (!be.isActive || state.get(ParticleEmitterBlock.POWERED)) return;

        ParticleType<?> rawType = Registries.PARTICLE_TYPE.get(be.particleType);
        if (rawType == null) return;

        @SuppressWarnings("unchecked")
        ParticleType<OmniParticleEffect> omniType = (ParticleType<OmniParticleEffect>) rawType;

        be.spawnAccumulator += be.amountPerTick;
        while (be.spawnAccumulator >= 1.0) {

            float r1, g1, b1, r2, g2, b2;

            if (be.useLifetimeColor) {
                // Lifetime Mode: Pass Start and End colors directly
                r1 = be.c1R; g1 = be.c1G; b1 = be.c1B;
                r2 = be.c2R; g2 = be.c2G; b2 = be.c2B;
            } else {
                // Random Mode: Pick ONE color and use it for both Start and End
                float lerpFactor = world.random.nextFloat();
                float finalR = MathHelper.lerp(lerpFactor, be.c1R, be.c2R);
                float finalG = MathHelper.lerp(lerpFactor, be.c1G, be.c2G);
                float finalB = MathHelper.lerp(lerpFactor, be.c1B, be.c2B);
                r1 = finalR; g1 = finalG; b1 = finalB;
                r2 = finalR; g2 = finalG; b2 = finalB;
            }

            OmniParticleEffect effect = new OmniParticleEffect(
                    omniType,
                    r1, g1, b1,
                    r2, g2, b2,
                    be.scale,
                    be.gravity,
                    be.lifetime,
                    be.orbX, be.orbY, be.orbZ,
                    be.rotate,
                    be.emissive
            );

            double spawnX = pos.getX() + 0.5 + be.offsetX + (world.random.nextGaussian() * be.areaX);
            double spawnY = pos.getY() + 0.5 + be.offsetY + (world.random.nextGaussian() * be.areaY);
            double spawnZ = pos.getZ() + 0.5 + be.offsetZ + (world.random.nextGaussian() * be.areaZ);

            double velX = be.minVelX + world.random.nextDouble() * (be.maxVelX - be.minVelX);
            double velY = be.minVelY + world.random.nextDouble() * (be.maxVelY - be.minVelY);
            double velZ = be.minVelZ + world.random.nextDouble() * (be.maxVelZ - be.minVelZ);

            world.addParticle(effect, true, spawnX, spawnY, spawnZ, velX, velY, velZ);

            be.spawnAccumulator -= 1.0;
        }
    }

    // --- Update Settings ---
    public void updateSettings(
            Identifier type,
            float r1, float g1, float b1, float r2, float g2, float b2, boolean useLifetimeColor,
            float scale, float gravity, double amount, int lifetime,
            float oX, float oY, float oZ,
            float aX, float aY, float aZ,
            float minVX, float maxVX, float minVY, float maxVY, float minVZ, float maxVZ,
            float orbX, float orbY, float orbZ, boolean rotate, boolean emissive
    ) {
        this.particleType = type;
        this.c1R = r1; this.c1G = g1; this.c1B = b1;
        this.c2R = r2; this.c2G = g2; this.c2B = b2;
        this.useLifetimeColor = useLifetimeColor;

        this.scale = scale; this.gravity = gravity;
        this.amountPerTick = amount; this.lifetime = lifetime;

        this.offsetX = oX; this.offsetY = oY; this.offsetZ = oZ;
        this.areaX = aX; this.areaY = aY; this.areaZ = aZ;

        this.minVelX = minVX; this.maxVelX = maxVX;
        this.minVelY = minVY; this.maxVelY = maxVY;
        this.minVelZ = minVZ; this.maxVelZ = maxVZ;

        this.orbX = orbX; this.orbY = orbY; this.orbZ = orbZ;
        this.rotate = rotate;
        this.emissive = emissive;

        this.markDirtyAndSync();
    }

    private static double[] rotateVector(double x, double y, double z, Direction facing) {
        // (Same as before)
        switch (facing) {
            case UP:    return new double[]{ x,  y,  z};
            case DOWN:  return new double[]{ x, -y, -z};
            case NORTH: return new double[]{ x,  z, -y};
            case SOUTH: return new double[]{-x,  z,  y};
            case EAST:  return new double[]{ y,  z, -x};
            case WEST:  return new double[]{-y,  z,  x};
            default:    return new double[]{ x,  y,  z};
        }
    }

    // --- NBT Saving & Loading ---
    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (this.owner != null) nbt.putUuid("Owner", this.owner);
        nbt.putBoolean("IsActive", isActive);
        nbt.putString("ParticleType", particleType.toString());

        nbt.putFloat("C1R", c1R); nbt.putFloat("C1G", c1G); nbt.putFloat("C1B", c1B);
        nbt.putFloat("C2R", c2R); nbt.putFloat("C2G", c2G); nbt.putFloat("C2B", c2B);
        nbt.putBoolean("UseLifetimeColor", useLifetimeColor);

        nbt.putFloat("OffsetX", offsetX); nbt.putFloat("OffsetY", offsetY); nbt.putFloat("OffsetZ", offsetZ);
        nbt.putFloat("AreaX", areaX); nbt.putFloat("AreaY", areaY); nbt.putFloat("AreaZ", areaZ);

        nbt.putDouble("Amount", amountPerTick);
        nbt.putInt("Lifetime", lifetime);
        nbt.putFloat("Scale", scale);
        nbt.putFloat("Gravity", gravity);

        nbt.putFloat("MinVelX", minVelX); nbt.putFloat("MaxVelX", maxVelX);
        nbt.putFloat("MinVelY", minVelY); nbt.putFloat("MaxVelY", maxVelY);
        nbt.putFloat("MinVelZ", minVelZ); nbt.putFloat("MaxVelZ", maxVelZ);

        nbt.putFloat("OrbX", orbX); nbt.putFloat("OrbY", orbY); nbt.putFloat("OrbZ", orbZ);
        nbt.putBoolean("Rotate", rotate);
        nbt.putBoolean("Emissive", emissive);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("Owner")) this.owner = nbt.getUuid("Owner");
        if (nbt.contains("IsActive")) this.isActive = nbt.getBoolean("IsActive");
        if (nbt.contains("ParticleType")) this.particleType = Identifier.of(nbt.getString("ParticleType"));

        if (nbt.contains("C1R")) this.c1R = nbt.getFloat("C1R");
        if (nbt.contains("C1G")) this.c1G = nbt.getFloat("C1G");
        if (nbt.contains("C1B")) this.c1B = nbt.getFloat("C1B");

        if (nbt.contains("C2R")) this.c2R = nbt.getFloat("C2R");
        if (nbt.contains("C2G")) this.c2G = nbt.getFloat("C2G");
        if (nbt.contains("C2B")) this.c2B = nbt.getFloat("C2B");
        if (nbt.contains("UseLifetimeColor")) this.useLifetimeColor = nbt.getBoolean("UseLifetimeColor");

        if (nbt.contains("OffsetX")) this.offsetX = nbt.getFloat("OffsetX");
        if (nbt.contains("OffsetY")) this.offsetY = nbt.getFloat("OffsetY");
        if (nbt.contains("OffsetZ")) this.offsetZ = nbt.getFloat("OffsetZ");

        if (nbt.contains("AreaX")) this.areaX = nbt.getFloat("AreaX");
        if (nbt.contains("AreaY")) this.areaY = nbt.getFloat("AreaY");
        if (nbt.contains("AreaZ")) this.areaZ = nbt.getFloat("AreaZ");

        if (nbt.contains("Amount")) this.amountPerTick = nbt.getDouble("Amount");
        if (nbt.contains("Lifetime")) this.lifetime = nbt.getInt("Lifetime");
        if (nbt.contains("Scale")) this.scale = nbt.getFloat("Scale");
        if (nbt.contains("Gravity")) this.gravity = nbt.getFloat("Gravity");

        if (nbt.contains("MinVelX")) this.minVelX = nbt.getFloat("MinVelX");
        if (nbt.contains("MaxVelX")) this.maxVelX = nbt.getFloat("MaxVelX");
        if (nbt.contains("MinVelY")) this.minVelY = nbt.getFloat("MinVelY");
        if (nbt.contains("MaxVelY")) this.maxVelY = nbt.getFloat("MaxVelY");
        if (nbt.contains("MinVelZ")) this.minVelZ = nbt.getFloat("MinVelZ");
        if (nbt.contains("MaxVelZ")) this.maxVelZ = nbt.getFloat("MaxVelZ");

        if (nbt.contains("OrbX")) this.orbX = nbt.getFloat("OrbX");
        if (nbt.contains("OrbY")) this.orbY = nbt.getFloat("OrbY");
        if (nbt.contains("OrbZ")) this.orbZ = nbt.getFloat("OrbZ");
        if (nbt.contains("Rotate")) this.rotate = nbt.getBoolean("Rotate");
        if (nbt.contains("Emissive")) this.emissive = nbt.getBoolean("Emissive");
    }

    // --- Network Syncing ---
    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    public void markDirtyAndSync() {
        this.markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }
}