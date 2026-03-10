package corablue.stagehand.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProxyStateManager extends PersistentState {
    // Stores [Source -> Target] coordinates
    private final Map<BlockPos, BlockPos> links = new HashMap<>();
    private final Map<BlockPos, Set<BlockPos>> targetsToSources = new HashMap<>();

    public void addLink(BlockPos source, BlockPos target) {
        links.put(source, target);
        targetsToSources.computeIfAbsent(target, k -> new HashSet<>()).add(source);
        this.markDirty(); // Save
    }

    public void removeLink(BlockPos pos) {
        boolean changed = false;

        // 1. If the block broken was a SOURCE, remove its link
        BlockPos target = links.remove(pos);
        if (target != null) {
            changed = true;
            Set<BlockPos> sources = targetsToSources.get(target);
            if (sources != null) {
                sources.remove(pos);
                if (sources.isEmpty()) {
                    targetsToSources.remove(target); // Clean up empty sets
                }
            }
        }

        // 2. If the block broken was a TARGET, remove all sources pointing to it instantly
        Set<BlockPos> pointingSources = targetsToSources.remove(pos);
        if (pointingSources != null) {
            for (BlockPos source : pointingSources) {
                links.remove(source);
            }
            changed = true;
        }

        if (changed) this.markDirty();
    }

    public Set<BlockPos> getSourcesForTarget(BlockPos target) {
        return targetsToSources.get(target);
    }

    @Nullable
    public BlockPos getTarget(BlockPos source) {
        return links.get(source);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (Map.Entry<BlockPos, BlockPos> entry : links.entrySet()) {
            NbtCompound pair = new NbtCompound();
            pair.putLong("Source", entry.getKey().asLong());
            pair.putLong("Target", entry.getValue().asLong());
            list.add(pair);
        }
        nbt.put("ProxyLinks", list);
        return nbt;
    }

    public static ProxyStateManager createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ProxyStateManager state = new ProxyStateManager();
        NbtList list = nbt.getList("ProxyLinks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound pair = list.getCompound(i);
            BlockPos source = BlockPos.fromLong(pair.getLong("Source"));
            BlockPos target = BlockPos.fromLong(pair.getLong("Target"));

            state.links.put(source, target);
            state.targetsToSources.computeIfAbsent(target, k -> new HashSet<>()).add(source);
        }
        return state;
    }

    public static ProxyStateManager getServerState(ServerWorld world) {
        PersistentState.Type<ProxyStateManager> type = new PersistentState.Type<>(
                ProxyStateManager::new,
                ProxyStateManager::createFromNbt,
                null
        );
        // This creates a "stagehand_proxy_links.dat" file in the world folder
        return world.getPersistentStateManager().getOrCreate(type, "stagehand_proxy_links");
    }
}