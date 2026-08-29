package qnimfi.cs.menu;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import qnimfi.cs.ChestSorter;

public class ModMenuTypes {

    public static final ExtendedMenuType<ChestLinkerConfigMenu, ChestLinkerMenuData> CHEST_LINKER_CONFIG =
            Registry.register(
                    BuiltInRegistries.MENU,
                    Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, "chest_linker_config"),
                    new ExtendedMenuType<>(
                            ChestLinkerConfigMenu::new,
                            ChestLinkerMenuData.STREAM_CODEC
                    )
            );

    public static void initialize() {
        // referencing this class triggers the static registration above
    }
}