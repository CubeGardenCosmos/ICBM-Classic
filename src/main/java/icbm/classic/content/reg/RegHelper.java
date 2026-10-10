package icbm.classic.content.reg;

import icbm.classic.ICBMConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Registration helpers for the flat 26.3 registries.
 * <p>
 * 1.12 used {@code RegistryEvent.Register} plus {@code setRegistryName}/{@code setUnlocalizedName}. 26.3
 * requires the id to be baked into the properties before the object is constructed
 * ({@code Properties#setId(ResourceKey)}), so every helper below creates the id first and registers the
 * result into the matching {@link BuiltInRegistries} registry (porting-guide.md §2.2).
 * <p>
 * Every registration is done from {@code onInitialize}; there is no load order to fight because the
 * helper forces the {@code Mod*} classes into the documented order (overview.md §5).
 */
public final class RegHelper
{
    private RegHelper() {}

    public static ResourceKey<Block> blockKey(String id)
    {
        return ResourceKey.create(Registries.BLOCK, ICBMConstants.id(id));
    }

    public static ResourceKey<Item> itemKey(String id)
    {
        return ResourceKey.create(Registries.ITEM, ICBMConstants.id(id));
    }

    /** Registers a block; the properties are given the id before the block is constructed. */
    public static <B extends Block> B block(String id, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties props)
    {
        final ResourceKey<Block> key = blockKey(id);
        return net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
    }

    /** Registers the block item for a block, reusing the block id as the item id. */
    public static BlockItem blockItem(Block block)
    {
        final ResourceKey<Item> key = itemKey(BuiltInRegistries.BLOCK.getKey(block).getPath());
        return net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, key,
                new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    /** Registers an item; the properties are given the id before the item is constructed. */
    public static <I extends Item> I item(String id, Function<Item.Properties, I> factory, Item.Properties props)
    {
        final ResourceKey<Item> key = itemKey(id);
        return net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
    }

    /**
     * Registers a sound event.
     * <p>
     * 1.12 created {@code new SoundEvent(id).setRegistryName(id)} inside a {@code RegistryEvent}; 26.3
     * builds the event from the id and registers it in one call. The 1.12 sound range was "variable"
     * (the range came from the play call), so {@code createVariableRangeEvent} is used.
     */
    public static SoundEvent sound(String id)
    {
        final ResourceKey<SoundEvent> key = ResourceKey.create(Registries.SOUND_EVENT, ICBMConstants.id(id));
        return net.minecraft.core.Registry.register(BuiltInRegistries.SOUND_EVENT, key,
                SoundEvent.createVariableRangeEvent(key.identifier()));
    }

    /**
     * Looks up an item by its flat id, returns null when it is not registered.
     * <p>
     * The {@code containsKey} check is required: {@link BuiltInRegistries#ITEM} is a
     * {@code DefaultedRegistry}, so {@code getValue} answers with the default (air) for unknown ids
     * instead of null.
     */
    public static Item item(String id)
    {
        final net.minecraft.resources.Identifier key = ICBMConstants.id(id);
        return BuiltInRegistries.ITEM.containsKey(key) ? BuiltInRegistries.ITEM.getValue(key) : null;
    }
}
