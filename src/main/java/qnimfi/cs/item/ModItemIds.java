package qnimfi.cs.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import qnimfi.cs.ChestSorter;

public class ModItemIds {

    public static ResourceKey<Item> create(String name) {
        return ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, name)
        );
    }

    public static final ResourceKey<Item> CHEST_LINKER = create("chest_linker");
}