package corablue.stagehand.world.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ProxyStateManager extends PersistentState {
    // Stores [Source -> Target] coordinates
    private final Map<BlockPos, BlockPos> links = new HashMap<>();

    public void addLink(BlockPos source, BlockPos target) {
        links.put(source, target);
        this.markDirty(); // Save
    }

    public void removeLink(BlockPos pos) {
        boolean changed = false;
        // If the block broken was a source, remove the link
        if (links.remove(pos) != null) changed = true;

        // If the block broken was a target, break any links pointing to it
        if (links.entrySet().removeIf(entry -> entry.getValue().equals(pos))) changed = true;

        if (changed) this.markDirty();
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
            state.links.put(BlockPos.fromLong(pair.getLong("Source")), BlockPos.fromLong(pair.getLong("Target")));
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