package qnimfi.cs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public record FilterEntry(Item item, int maxCount) {

    public static final Codec<FilterEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(FilterEntry::item),
            Codec.INT.fieldOf("max_count").forGetter(FilterEntry::maxCount)
    ).apply(instance, FilterEntry::new));
}