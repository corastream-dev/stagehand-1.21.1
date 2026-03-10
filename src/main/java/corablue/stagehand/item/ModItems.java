package corablue.stagehand.item;

import corablue.stagehand.Stagehand;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item BLOCK_LINKER = registerItem("block_linker",
            new corablue.stagehand.item.custom.BlockLinkerItem(new Item.Settings().maxCount(1)));

    private static Item registerItem(String name, Item item){
        return Registry.register(Registries.ITEM, Identifier.of(Stagehand.MOD_ID, name), item);
    }

    public static void registerModItems(){
        Stagehand.LOGGER.info("Registering mod items for" + Stagehand.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
        });
    }


}
