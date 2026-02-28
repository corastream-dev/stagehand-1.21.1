package corablue.stagehand.block.entity;

import corablue.stagehand.Stagehand;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FatigueCoreBlockEntity extends BlockEntity {
    public static final Set<FatigueCoreBlockEntity> ACTIVE_CORES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<UUID, Long> LAST_MESSAGE_TIMES = new HashMap<>();

    private UUID owner = null;
    private int range = 8;
    private boolean affectOwner = false;

    private final Set<UUID> optedOutPlayers = new HashSet<>();
    private final Set<UUID> currentlyAffected = new HashSet<>();

    //Unsure about this? Might remove
    @Override
    public void markRemoved() {
        ACTIVE_CORES.remove(this);
        super.markRemoved();
    }

    public FatigueCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FATIGUE_CORE_BE, pos, state);
        ACTIVE_CORES.add(this);
    }

    public static void tick(World world, BlockPos pos, BlockState state, FatigueCoreBlockEntity be) {
        // Ticking every 20 ticks (1 second)
        if (world.getTime() % 20 != 0) return;

        Box searchBox = new Box(pos).expand(be.range);
        List<ServerPlayerEntity> playersInRange = world.getEntitiesByClass(
                ServerPlayerEntity.class, searchBox, player -> player.isAlive() && !player.isSpectator()
        );

        Set<UUID> playersHandledThisTick = new HashSet<>();

        for (ServerPlayerEntity player : playersInRange) {
            UUID uuid = player.getUuid();

            if ((!be.affectOwner && be.isOwner(player)) || player.isCreative() || be.optedOutPlayers.contains(uuid)) {
                continue;
            }

            playersHandledThisTick.add(uuid);

            // 1. Apply Mining Fatigue III
            // Duration is 31 ticks to ensure overlap between 20-tick pulses
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 31, Stagehand.CONFIG.MiningFatigueLevel(), true, false, true));
            be.currentlyAffected.add(uuid);

            // 2. Handle 10-Minute Message
            long currentTime = world.getTime();
            long lastTime = LAST_MESSAGE_TIMES.getOrDefault(uuid, -24000L);

            if (currentTime - lastTime >= 12000 && Stagehand.CONFIG.AllowFatigueZoneOptout()) {
                player.sendMessage(Text.translatable("command.stagehand.fatigue_info"), false);
                LAST_MESSAGE_TIMES.put(uuid, currentTime);
            }
        }

        // 3. Handle players leaving the zone
        Iterator<UUID> it = be.currentlyAffected.iterator();
        while (it.hasNext()) {
            UUID affectedUuid = it.next();
            if (!playersHandledThisTick.contains(affectedUuid)) {
                it.remove();

                ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(affectedUuid);
                if (player != null) {
                    boolean stillAffectedElsewhere = false;
                    for (FatigueCoreBlockEntity otherCore : ACTIVE_CORES) {
                        if (otherCore != be && otherCore.currentlyAffected.contains(affectedUuid)) {
                            stillAffectedElsewhere = true;
                            break;
                        }
                    }

                    // Remove the effect immediately when they leave all zones
                    if (!stillAffectedElsewhere) {
                        player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
                        LAST_MESSAGE_TIMES.remove(affectedUuid);
                    }
                }
            }
        }
    }

    public void releaseAllPlayers() {
        if (this.world == null || this.world.isClient) return;

        for (UUID affectedUuid : this.currentlyAffected) {
            ServerPlayerEntity player = (ServerPlayerEntity) this.world.getPlayerByUuid(affectedUuid);
            if (player != null) {
                boolean stillAffectedElsewhere = false;
                for (FatigueCoreBlockEntity otherCore : ACTIVE_CORES) {
                    if (otherCore != this && otherCore.currentlyAffected.contains(affectedUuid)) {
                        stillAffectedElsewhere = true;
                        break;
                    }
                }

                if (!stillAffectedElsewhere) {
                    player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
                    LAST_MESSAGE_TIMES.remove(affectedUuid);
                }
            }
        }
        this.currentlyAffected.clear();
    }

    public boolean isPlayerInRange(PlayerEntity player) {
        return player.getWorld() == this.getWorld()
                && player.squaredDistanceTo(this.pos.toCenterPos()) <= (this.range * this.range);
    }

    public void optOutPlayer(UUID uuid) {
        this.optedOutPlayers.add(uuid);
        this.markDirty();
    }

    public void optInPlayer(UUID uuid) {
        this.optedOutPlayers.remove(uuid);
        this.markDirty();
    }

    public void setOwner(UUID uuid) { this.owner = uuid; markDirty(); }
    public boolean isOwner(PlayerEntity player) { return this.owner != null && this.owner.equals(player.getUuid()); }
    public int getRange() { return this.range; }
    public boolean doesAffectOwner() { return this.affectOwner; }

    public void setRange(int range) { this.range = range; markDirtyAndSync(); }
    public void setAffectOwner(boolean affectOwner) { this.affectOwner = affectOwner; markDirtyAndSync(); }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (owner != null) nbt.putUuid("Owner", owner);
        nbt.putInt("Range", range);
        nbt.putBoolean("AffectOwner", affectOwner);

        NbtList optOutList = new NbtList();
        for (UUID uuid : optedOutPlayers) {
            optOutList.add(NbtString.of(uuid.toString()));
        }
        nbt.put("OptedOut", optOutList);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("Owner")) this.owner = nbt.getUuid("Owner");
        this.range = nbt.getInt("Range");
        this.affectOwner = nbt.getBoolean("AffectOwner");

        this.optedOutPlayers.clear();
        if (nbt.contains("OptedOut")) {
            NbtList list = nbt.getList("OptedOut", NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                this.optedOutPlayers.add(UUID.fromString(list.getString(i)));
            }
        }
    }

    @Nullable @Override public Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
    @Override public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) { return createNbt(registryLookup); }
    private void markDirtyAndSync() {
        this.markDirty();
        if (this.world != null && !this.world.isClient) {
            this.world.updateListeners(this.pos, this.getCachedState(), this.getCachedState(), 3);
        }
    }
}