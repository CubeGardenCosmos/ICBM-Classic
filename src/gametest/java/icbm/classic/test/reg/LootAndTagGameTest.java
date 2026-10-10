package icbm.classic.test.reg;

import icbm.classic.ICBMConstants;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Covers the S2.07 data: the generated loot tables and tags are loaded by the server and do what the 1.12
 * sources did.
 *
 * <ul>
 *     <li>mining tags: the concrete/spikes/glass button and plate are pickaxe blocks, the reinforced glass
 *     ({@code Material.GLASS}) and the flat explosive blocks ({@code Material.TNT}) are not, and no content
 *     block asks for an iron tool (1.12 never called {@code setHarvestLevel}),</li>
 *     <li>{@code c:} item tags: the 1.12 OreDictionary names are back as convention tags,</li>
 *     <li>drops: a block drops itself (1.12 had no drop overrides, and {@code canSilkHarvest} defaults to
 *     true, so the reinforced glass drops without silk touch too).</li>
 * </ul>
 */
public class LootAndTagGameTest
{
    @GameTest(maxTicks = 100)
    public void lootTablesAndTagsAreLoaded(GameTestHelper helper)
    {
        final Registry<Block> blocks = helper.getLevel().getServer().registryAccess().lookupOrThrow(Registries.BLOCK);
        final Registry<Item> items = helper.getLevel().getServer().registryAccess().lookupOrThrow(Registries.ITEM);

        for (Block pickaxe : new Block[] {ModBlocks.CONCRETE, ModBlocks.COMPACT_CONCRETE, ModBlocks.REINFORCED_CONCRETE,
                ModBlocks.SPIKES, ModBlocks.POISON_SPIKES, ModBlocks.FIRE_SPIKES, ModBlocks.GLASS_BUTTON,
                ModBlocks.GLASS_PRESSURE_PLATE})
        {
            helper.assertTrue(blocks.wrapAsHolder(pickaxe).is(BlockTags.MINEABLE_WITH_PICKAXE),
                    id(pickaxe) + " should be mineable with a pickaxe");
            helper.assertTrue(!blocks.wrapAsHolder(pickaxe).is(BlockTags.NEEDS_IRON_TOOL),
                    id(pickaxe) + " should not need an iron tool, 1.12 set no harvest level");
        }
        for (Block noTool : new Block[] {ModBlocks.REINFORCED_GLASS, ModBlocks.EXPLOSIVE_BLOCKS.get(ICBMConstants.id("nuclear"))})
        {
            helper.assertTrue(!blocks.wrapAsHolder(noTool).is(BlockTags.MINEABLE_WITH_PICKAXE),
                    id(noTool) + " is glass/tnt like and should not be a pickaxe block");
        }

        helper.assertTrue(items.wrapAsHolder(ModItems.STEEL_INGOT).is(commonItemTag("ingots/steel")), "missing c:ingots/steel");
        helper.assertTrue(items.wrapAsHolder(ModItems.COPPER_INGOT).is(commonItemTag("ingots/copper")), "missing c:ingots/copper");
        helper.assertTrue(items.wrapAsHolder(ModItems.STEEL_PLATE).is(commonItemTag("plates/steel")), "missing c:plates/steel");
        helper.assertTrue(items.wrapAsHolder(ModItems.IRON_PLATE).is(commonItemTag("plates/iron")), "missing c:plates/iron");
        helper.assertTrue(items.wrapAsHolder(ModItems.SULFUR_DUST).is(commonItemTag("dusts/sulfur")), "missing c:dusts/sulfur");
        helper.assertTrue(items.wrapAsHolder(ModItems.SALTPETER).is(commonItemTag("dusts/saltpeter")), "missing c:dusts/saltpeter");
        helper.assertTrue(!items.wrapAsHolder(ModItems.SALTPETER).is(commonItemTag("dusts/sulfur")),
                "saltpeter is not sulfur dust");

        // drops: the reinforced glass (Material.GLASS) and a flat explosive block (Material.TNT) drop themselves
        // by hand, while concrete (Material.ROCK) needs a pickaxe, in 1.12 and in 26.3 alike. The item entities
        // appear after the destroyed block ticked, so the assertions wait for them.
        final BlockPos handBrokenGlass = new BlockPos(1, 2, 1);
        final BlockPos handBrokenExplosive = new BlockPos(4, 2, 1);
        final BlockPos pickaxeConcrete = new BlockPos(7, 2, 1);
        final BlockPos bareHandConcrete = new BlockPos(10, 2, 1);
        final Block explosiveBlock = ModBlocks.EXPLOSIVE_BLOCKS.get(ICBMConstants.id("condensed"));
        // GameTestHelper#destroyBlock breaks without dropping and Level#destroyBlock ignores the tool
        // requirement, so the blocks are mined through the player game mode, the path a real player uses.
        final ServerPlayer miner = helper.makeMockServerPlayerInLevel();
        miner.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        mineBlock(helper, miner, handBrokenGlass, ModBlocks.REINFORCED_GLASS, ItemStack.EMPTY);
        mineBlock(helper, miner, handBrokenExplosive, explosiveBlock, ItemStack.EMPTY);
        mineBlock(helper, miner, bareHandConcrete, ModBlocks.CONCRETE, ItemStack.EMPTY);
        mineBlock(helper, miner, pickaxeConcrete, ModBlocks.CONCRETE, new ItemStack(Items.IRON_PICKAXE));

        helper.succeedWhen(() ->
        {
            helper.assertItemEntityPresent(ModBlocks.REINFORCED_GLASS.asItem(), handBrokenGlass, 1.5);
            helper.assertItemEntityPresent(explosiveBlock.asItem(), handBrokenExplosive, 1.5);
            helper.assertItemEntityPresent(ModBlocks.CONCRETE.asItem(), pickaxeConcrete, 1.5);
            helper.assertItemEntityNotPresent(ModBlocks.CONCRETE.asItem(), bareHandConcrete, 1.5);
            helper.assertBlockPresent(Blocks.AIR, bareHandConcrete);
            helper.assertItemEntityNotPresent(Items.SAND, handBrokenGlass, 1.5);
        });
    }

    /**
     * Places the block on stone and mines it as {@code miner} holding {@code tool} ({@link ItemStack#EMPTY}
     * is a bare hand break). The drops are checked by the caller once they spawned.
     */
    private static void mineBlock(GameTestHelper helper, ServerPlayer miner, BlockPos pos, Block block, ItemStack tool)
    {
        helper.setBlock(pos.below(), Blocks.STONE);
        helper.setBlock(pos, block);
        miner.setItemInHand(InteractionHand.MAIN_HAND, tool);
        miner.gameMode.destroyBlock(helper.absolutePos(pos));
    }

    private static String id(Block block)
    {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static TagKey<Item> commonItemTag(String path)
    {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
}
