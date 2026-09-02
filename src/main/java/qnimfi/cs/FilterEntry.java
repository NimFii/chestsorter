package qnimfi.cs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public record FilterEntry(Item item, int maxCount, FilterType type) {

    public static final Codec<FilterEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(FilterEntry::item),
            Codec.INT.fieldOf("max_count").forGetter(FilterEntry::maxCount),
            Codec.STRING.optionalFieldOf("type", "only").xmap(
                    s -> {
                        try { return FilterType.valueOf(s.toUpperCase()); }
                        catch (Exception e) { return FilterType.ONLY; }
                    },
                    FilterType::name
            ).forGetter(FilterEntry::type)
    ).apply(instance, FilterEntry::new));
}