package icbm.classic.test.block;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.data.BlockActivateFunction;
import icbm.classic.content.blocks.explosive.ExplosiveBlockEntity;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.reg.ModBlocks;
import icbm.classic.content.reg.ModDataComponents;
import icbm.classic.content.reg.ModEntities;
import icbm.classic.lib.data.ExplosiveSettings;
import icbm.classic.lib.data.GpsData;
import icbm.classic.test.util.SaveRoundTrip;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * Covers the flat explosive blocks and their fuse (S3.09, {@code ExplosiveBlock}/{@code ExplosiveBlockEntity}):
 *
 * <ul>
 *     <li>a redstone signal ignites {@code icbmclassic:condensed_explosive}: the block is removed, a
 *     {@code primed_explosive} entity appears and the blast runs when the fuse of the explosive content
 *     registry runs out (1.12 {@code BlockExplosive#neighborChanged} → {@code TileEntityExplosive#trigger} →
 *     fuse → blast),</li>
 *     <li>{@code icbmclassic:nuclear_explosive} is primed with the configured fuse but the test discards the
 *     entity so the nuclear blast never runs,</li>
 *     <li>placing an explosive item copies the {@code icbmclassic:explosive_settings} component into the block
 *     entity and the settings survive a save/load round trip under the 1.12 key,</li>
 *     <li>mining an explosive block drops the block item itself (1.12 {@code ItemBlockExplosive}, loot table
 *     of S2.07),</li>
 *     <li>the ender explosive activation listener (S3.07 registration) stores the target of a held GPS item,
 *     the 26.3 counterpart of the 1.12 {@code ItemBlockExplosive}/coordinate card path.</li>
 * </ul>
 */
public class ExplosiveBlockGameTest
{
    /** Center of the test structure, 1.12 style test coordinates */
    private static final BlockPos CENTER = new BlockPos(4, 3, 4);

    private static Block explosive(String id)
    {
        final Block block = ModBlocks.EXPLOSIVE_BLOCKS.get(ICBMConstants.id(id));
        if (block == null)
        {
            throw new IllegalStateException("Missing flat explosive block icbmclassic:" + id);
        }
        return block;
    }

    /** Primes the explosive at {@link #CENTER} through its neighbour, 1.12 {@code neighborChanged}. */
    private static void igniteWithRedstone(GameTestHelper helper, Block explosive)
    {
        // the primed explosive falls until it hits a block, a floor keeps the blast inside the test area
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                helper.setBlock(CENTER.offset(x, -1, z), Blocks.STONE);
            }
        }
        helper.setBlock(CENTER, explosive.defaultBlockState());
        // 1.12: world.isBlockPowered(pos) reads the redstone power of the neighbors
        helper.setBlock(CENTER.east(), Blocks.REDSTONE_BLOCK);
    }

    /** The primed explosive in the test structure, null while none was spawned. */
    @Nullable
    private static EntityExplosive findPrimed(GameTestHelper helper)
    {
        final List<EntityExplosive> entities = helper.getEntities(ModEntities.PRIMED_EXPLOSIVE);
        return entities.isEmpty() ? null : entities.get(0);
    }

    /**
     * 1.12 {@code BlockExplosive#neighborChanged} + {@code TileEntityExplosive#trigger}: the powered block
     * spawns the primed explosive, removes itself and the primed explosive runs the blast once the fuse of
     * {@code ConfigBlast.FUSE_TIMES.EXPLOSIVES.CONDENSED} (100 ticks) ran out.
     * <p>
     * The condensed blast is configured with a 6 block radius ({@code ConfigBlast.condensed.energyScale}),
     * which is wider than the 8x8x8 test structure, so this test reserves extra padding around its structure
     * to keep the explosion clear of the neighboring tests.
     */
    @GameTest(maxTicks = 400, padding = 16)
    public void redstoneIgnitesCondensedExplosive(GameTestHelper helper)
    {
        igniteWithRedstone(helper, explosive("condensed"));

        helper.startSequence()
                // the tile triggers right away: the block is gone and the primed explosive takes its place
                .thenWaitUntil(() -> helper.assertTrue(findPrimed(helper) != null,
                        "the redstone signal should have primed the condensed explosive block"))
                .thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, CENTER))
                // the fuse of the explosive content registry runs out and the primed explosive runs its blast
                .thenWaitUntil(() -> helper.assertEntityNotPresent(ModEntities.PRIMED_EXPLOSIVE))
                .thenExecute(() ->
                {
                    int removed = 0;
                    for (int x = -1; x <= 1; x++)
                    {
                        for (int z = -1; z <= 1; z++)
                        {
                            if (helper.getBlockState(CENTER.offset(x, -1, z)).isAir())
                            {
                                removed++;
                            }
                        }
                    }
                    helper.assertTrue(removed > 0,
                            "the primed explosive should have run the condensed blast when its fuse ran out, "
                                    + "but none of the stone blocks under the explosive was destroyed");
                })
                .thenSucceed();
    }

    /**
     * The nuclear explosive is primed with the fuse of the explosive content registry, but the test discards
     * the primed entity before the fuse runs out ({@code ConfigBlast.FUSE_TIMES.EXPLOSIVES.NUCLEAR} = 100
     * ticks) so a nuclear blast never runs inside the test level.
     */
    @GameTest(maxTicks = 60)
    public void nuclearExplosiveIsPrimedWithTheRegistryFuse(GameTestHelper helper)
    {
        igniteWithRedstone(helper, explosive("nuclear"));

        helper.startSequence()
                // the fuse is initialized by the primed entity's first tick (1.12
                // EntityExplosive#onUpdate: if (fuse == -1) fuse = registry.getFuseTime(...))
                .thenWaitUntil(() -> helper.assertTrue(findPrimed(helper) != null && findPrimed(helper).getFuse() > 0,
                        "the redstone signal should have primed the nuclear explosive block and started its fuse"))
                .thenExecute(() ->
                {
                    final EntityExplosive primed = findPrimed(helper);
                    final int expectedFuse = ICBMClassicAPI.EX_BLOCK_REGISTRY.getFuseTime(helper.getLevel(),
                            primed.getX(), primed.getY(), primed.getZ(), ICBMConstants.id("nuclear"));
                    final int fuse = primed.getFuse();

                    helper.assertTrue(ICBMConstants.id("nuclear").equals(primed.getExplosiveId()),
                            "the primed explosive should carry the nuclear explosive but carries " + primed.getExplosiveId());
                    helper.assertTrue(fuse > 0 && fuse <= expectedFuse && fuse >= expectedFuse - 10,
                            "the primed explosive should start on the fuse of the explosive content registry ("
                                    + expectedFuse + ") but its fuse is " + fuse);

                    // must not let a nuclear blast run in the test level
                    primed.discard();
                    helper.assertEntityNotPresent(ModEntities.PRIMED_EXPLOSIVE);
                })
                .thenSucceed();
    }

    /**
     * 1.12 {@code onBlockPlacedBy} replaced the block entity explosive capability with the placed stack and
     * {@code writeToNBT} saved it under {@code explosive_stack}. 26.3 copies the
     * {@code icbmclassic:explosive_settings} component ({@code BlockItem#place} →
     * {@code BlockEntity#applyComponentsFromItemStack}) and saves the settings under the same key.
     */
    @GameTest(maxTicks = 40)
    public void placedSettingsSurviveSaveAndLoad(GameTestHelper helper)
    {
        helper.setBlock(CENTER, explosive("ender").defaultBlockState());
        final ExplosiveBlockEntity placed = helper.getBlockEntity(CENTER, ExplosiveBlockEntity.class);
        helper.assertTrue(placed != null, "the explosive block should create an explosive block entity");

        final GlobalPos target = new GlobalPos(helper.getLevel().dimension(), helper.absolutePos(CENTER.offset(2, 0, 2)));
        final ItemStack placedItem = new ItemStack(explosive("ender"));
        placedItem.set(ModDataComponents.EXPLOSIVE_SETTINGS, new ExplosiveSettings(target));
        //1.12: explosive.capabilityExplosive = new CapabilityExplosiveStack(itemStack)
        placed.applyComponentsFromItemStack(placedItem);

        helper.assertTrue(target.equals(placed.getSettings().enderTarget().orElse(null)),
                "placing the item should copy the explosive settings into the block entity but the settings are "
                        + placed.getSettings());

        //1.12 TileEntityExplosive#writeToNBT: nbt.setTag("explosive_stack", ...)
        final CompoundTag tag = placed.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(tag.contains(ExplosiveBlockEntity.NBT_EXPLOSIVE_STACK),
                "the settings have to be saved under the 1.12 key " + ExplosiveBlockEntity.NBT_EXPLOSIVE_STACK
                        + ", the saved tag is " + tag);

        SaveRoundTrip.assertRoundTrip(helper, placed, null, (original, loaded) ->
                ((ExplosiveBlockEntity) loaded).getSettings().equals(((ExplosiveBlockEntity) original).getSettings()));
        helper.succeed();
    }

    /**
     * Mining the block with a survival player drops the block item, 1.12 {@code ItemBlockExplosive} and the
     * loot table generated by S2.07 ({@code ICBMBlockLootProvider#dropSelf}).
     */
    @GameTest(maxTicks = 100)
    public void miningTheExplosiveBlockDropsItself(GameTestHelper helper)
    {
        final Block condensed = explosive("condensed");
        // GameTestHelper#destroyBlock breaks without dropping, so the block is mined through the player game
        // mode, the path a real player uses
        helper.setBlock(CENTER.below(), Blocks.STONE);
        helper.setBlock(CENTER, condensed);
        final ServerPlayer miner = helper.makeMockServerPlayerInLevel();
        miner.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        miner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        miner.gameMode.destroyBlock(helper.absolutePos(CENTER));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertItemEntityPresent(condensed.asItem(), CENTER, 4.0D))
                .thenSucceed();
    }

    /**
     * Right clicking an ender explosive block with a GPS item stores the target in the block settings, the
     * 26.3 counterpart of the 1.12 {@code ExplosiveInit#enderBlockCoordSet} registration (which 1.12 never
     * invoked). The item is set up by hand because the GPS items of the mod land in S5.08.
     */
    @GameTest(maxTicks = 40)
    public void enderExplosiveActivationStoresTheGpsTarget(GameTestHelper helper)
    {
        helper.setBlock(CENTER, explosive("ender").defaultBlockState());
        final ExplosiveBlockEntity explosive = helper.getBlockEntity(CENTER, ExplosiveBlockEntity.class);
        helper.assertTrue(explosive != null, "the explosive block should create an explosive block entity");

        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final GlobalPos target = new GlobalPos(helper.getLevel().dimension(), helper.absolutePos(CENTER.offset(2, 1, 2)));
        final ItemStack gpsItem = new ItemStack(Items.PAPER);
        gpsItem.set(ModDataComponents.GPS_DATA, new GpsData(target));
        player.setItemInHand(InteractionHand.MAIN_HAND, gpsItem);

        final BlockActivateFunction activation = ICBMClassicAPI.EX_BLOCK_REGISTRY.getActivationListener(ICBMConstants.id("ender"));
        helper.assertTrue(activation != null, "the ender explosive should have an activation listener");

        helper.assertTrue(activation.onActivate(helper.getLevel(), helper.absolutePos(CENTER), player,
                        InteractionHand.MAIN_HAND, Direction.UP, 0.5F, 0.5F, 0.5F),
                "activating the ender explosive with a GPS item should consume the interaction");
        helper.assertTrue(target.equals(explosive.getSettings().enderTarget().orElse(null)),
                "the activation should store the GPS target in the explosive settings but the settings are "
                        + explosive.getSettings());
        helper.succeed();
    }
}
