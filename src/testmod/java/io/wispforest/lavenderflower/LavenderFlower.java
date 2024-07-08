package io.wispforest.lavenderflower;

import io.wispforest.lavender.book.LavenderBookItem;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.GameData;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(LavenderFlower.MOD_ID)
public class LavenderFlower {

    public static final String MOD_ID = "lavender_flower";

    public LavenderFlower(IEventBus modBus) {
        modBus.addListener(RegisterEvent.class, event -> {
            event.register(RegistryKeys.ITEM, id("the_book"), () -> LavenderBookItem.forBook(id("the_book"), new Item.Settings()));
        });

    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
