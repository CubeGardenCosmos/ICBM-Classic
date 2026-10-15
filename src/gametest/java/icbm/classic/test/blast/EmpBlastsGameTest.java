package icbm.classic.test.blast;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.actions.status.IActionStatus;
import icbm.classic.api.caps.IEMPReceiver;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.config.util.BlockReplacementData;
import icbm.classic.config.util.RegistryLookup;
import icbm.classic.content.actions.causeby.CauseByTimer;
import icbm.classic.content.actions.emp.ActionEmpArea;
import icbm.classic.content.actions.emp.EmpHandler;
import icbm.classic.content.entity.EntityExplosive;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.lib.actions.status.ActionResponses;
import icbm.classic.lib.lookup.IcbmLookups;
import icbm.classic.lib.transform.vector.Pos;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Runs the EMP action ported by S3.07 ({@code icbmclassic:emp} → {@code ActionDataEmpArea} →
 * {@link ActionEmpArea}) through the same registry path the game uses.
 * <ul>
 *     <li>creeper → the 1.12 {@code CapabilityEmpCreeper} effect (lightning strike → powered creeper),</li>
 *     <li>dropped item → the 1.12 {@code CapabilityEmpEntityItem} → {@code CapabilityEmpInventory#empItemStack}
 *     path reaches an item EMP receiver and writes the changed stack back,</li>
 *     <li>item inside a container → the block entity inventory path
 *     ({@code CapabilityEmpInventory.TileInv}) does the same for a chest,</li>
 *     <li>block replacement → the config driven {@link EmpHandler} replacements are applied.</li>
 * </ul>
 * 1.12 shipped no item with an EMP capability of its own (item energy draining through
 * {@code EnergySystem} is S6.05), so the item half registers a test owned receiver on
 * {@link IcbmLookups#EMP_RECEIVER_ITEM} for {@link #EMP_ITEM}; everything else uses the registered content.
 * The size is forced to 3 to keep the affected area inside the default 8x8x8 test structure.
 */
public class EmpBlastsGameTest
{
    /** Center of the emp, relative to the default 8x8x8 structure */
    private static final BlockPos CENTER = new BlockPos(4, 3, 4);
    /** Size the emp is forced to, keeps the affected area inside the test structure */
    private static final int TEST_SIZE = 3;
    /** Item the test registers an EMP receiver for, used as the dropped item and as the chest content */
    private static final Item EMP_ITEM = Items.STRUCTURE_VOID;

    /**
     * Test owned EMP receiver for {@link #EMP_ITEM}.
     * <p>
     * {@code CapabilityEmpInventory#empItemStack} passes the stack <b>copy</b> to the receiver, the EMP then
     * compares it against the stored stack and writes it back when it changed (1.12 did the same). Shrinking
     * the stack therefore proves both halves of the path: the receiver was reached and its change was kept.
     */
    private static final class TestItemEmpReceiver implements IEMPReceiver
    {
        static int calls;
        static int doActionCalls;

        private final ItemStack stack;

        TestItemEmpReceiver(ItemStack stack)
        {
            this.stack = stack;
        }

        static void reset()
        {
            calls = 0;
            doActionCalls = 0;
        }

        @Override
        public float applyEmpAction(Level level, double x, double y, double z, IAction emp_blast, float power, boolean doAction)
        {
            calls++;
            if (doAction)
            {
                //1.12 always passed true here, even when simulating
                doActionCalls++;
            }
            stack.shrink(1);
            return power;
        }
    }

    static
    {
        //Lookups are normally filled during mod init; the test owns this provider so a dropped/contained item
        //stack is EMP-able without waiting for the S5/S6 content that registers the real item receivers.
        IcbmLookups.EMP_RECEIVER_ITEM.registerForItems(
                (stack, context) -> new TestItemEmpReceiver(stack),
                EMP_ITEM);
    }

    /**
     * Triggers the emp explosive through the registry, mirroring how the game creates the action
     * (see {@code BasicBlastsGameTest}).
     *
     * @param helper test helper
     * @return status returned by the emp action
     */
    private static IActionStatus triggerEmp(GameTestHelper helper)
    {
        final IExplosiveData data = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(ICBMConstants.id("emp"), false);
        final BlockPos pos = helper.absolutePos(CENTER);
        final Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        final IActionSource source = new ActionSource(helper.getLevel(), center, new CauseByTimer("test", 0));

        final IAction action = data.create(helper.getLevel(), center.x, center.y, center.z, source, null);
        if (action instanceof ActionEmpArea empArea)
        {
            empArea.setSize(TEST_SIZE);
        }
        return action.doAction();
    }

    /** Drops an item at the emp center so the entity pass of {@code ActionEmpArea} sees it */
    private static ItemEntity dropItem(GameTestHelper helper, int count)
    {
        final BlockPos pos = helper.absolutePos(CENTER);
        final ItemEntity itemEntity = new ItemEntity(helper.getLevel(),
                pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                new ItemStack(EMP_ITEM, count));
        helper.getLevel().addFreshEntity(itemEntity);
        return itemEntity;
    }

    /** Restores the emp block replacements to the shipped default (empty list) */
    private static void resetEmpReplacements()
    {
        ConfigBlast.emp.replacements.blockStates = new String[]{};
        EmpHandler.loadFromConfig();
        EmpHandler.empBlockSwaps.clearDefaults();
    }

    /**
     * 1.12 {@code CapabilityEmpCreeper#applyEmpAction} struck the creeper with an effect only lightning bolt
     * ({@code EntityCreeper#onStruckByLightning}), which sets the powered flag. 26.3 reaches the same receiver
     * through {@link IcbmLookups#EMP_RECEIVER_ENTITY} and applies {@code Creeper#thunderHit}, whose observable
     * result is {@code Creeper#isPowered()}.
     */
    @GameTest(maxTicks = 100)
    public void empSuperChargesCreeper(GameTestHelper helper)
    {
        helper.setBlock(CENTER.below(), Blocks.STONE);
        final Creeper creeper = helper.spawn(EntityTypes.CREEPER, new Vec3(4.5D, CENTER.getY() + 1.0D, 4.5D));
        try
        {
            helper.assertFalse(creeper.isPowered(), "the creeper should start unpowered");
            final IActionStatus status = triggerEmp(helper);
            helper.assertTrue(status == ActionResponses.COMPLETED, "emp did not report COMPLETED, was " + status);
            helper.assertTrue(creeper.isPowered(),
                    "emp did not super charge the creeper, ALLOW_LIGHTING_CREEPER=" + ConfigBlast.emp.ALLOW_LIGHTING_CREEPER
                            + " level=" + helper.getLevel().getClass().getSimpleName());
            helper.succeed();
        }
        finally
        {
            creeper.discard();
        }
    }

    /**
     * Dropped item with an EMP receiver: 1.12 wrapped the dropped stack in {@code CapabilityEmpEntityItem},
     * which ran {@code CapabilityEmpInventory#empItemStack} on a copy of the stack and stored the copy back
     * when it changed.
     */
    @GameTest(maxTicks = 100)
    public void empProcessesDroppedItem(GameTestHelper helper)
    {
        TestItemEmpReceiver.reset();
        final ItemEntity itemEntity = dropItem(helper, 4);
        try
        {
            triggerEmp(helper);
            helper.assertTrue(TestItemEmpReceiver.calls == 1,
                    "emp did not reach the item EMP receiver of the dropped item, calls: " + TestItemEmpReceiver.calls);
            helper.assertTrue(TestItemEmpReceiver.doActionCalls == 1,
                    "emp simulated the item effect instead of applying it");
            helper.assertTrue(itemEntity.getItem().getCount() == 3,
                    "emp did not write the changed stack back into the dropped item, stack: " + itemEntity.getItem());
            helper.succeed();
        }
        finally
        {
            itemEntity.discard();
        }
    }

    /**
     * Container block entity: 1.12 asked the tile entity for the item handler capability and wrapped it in
     * {@code CapabilityEmpInventory.TileInv}. 26.3 uses the vanilla {@link Container} of the block entity and
     * runs the same per slot item call.
     */
    @GameTest(maxTicks = 100)
    public void empProcessesItemInsideContainer(GameTestHelper helper)
    {
        TestItemEmpReceiver.reset();
        helper.setBlock(CENTER.below(), Blocks.STONE);
        helper.setBlock(CENTER, Blocks.CHEST);

        final Container chest = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(CENTER));
        helper.assertTrue(chest != null, "the test chest did not create a block entity");
        chest.setItem(0, new ItemStack(EMP_ITEM, 5));

        triggerEmp(helper);

        helper.assertTrue(TestItemEmpReceiver.calls == 1,
                "emp did not reach the item EMP receiver inside the container, calls: " + TestItemEmpReceiver.calls);
        helper.assertTrue(chest.getItem(0).getCount() == 4,
                "emp did not write the changed stack back into the container, stack: " + chest.getItem(0));
        helper.succeed();
    }

    /**
     * 1.12 {@code EntityExplosive} carried {@code CapabilityEmpKill} as its EMP receiver, so an EMP killed the
     * primed explosive (and the minecart explosives) before they could run their own explosive. 26.3 registers
     * the entity itself on {@link IcbmLookups#EMP_RECEIVER_ENTITY} (see {@code CapabilityEMP#register()}) and
     * the receiver calls {@code Entity#discard()} (1.12 {@code setDead}).
     */
    @GameTest(maxTicks = 100)
    public void empKillsPrimedExplosive(GameTestHelper helper)
    {
        resetEmpReplacements();
        helper.setBlock(CENTER.below(), Blocks.STONE);

        final BlockPos pos = helper.absolutePos(CENTER);
        final EntityExplosive explosive = new EntityExplosive(helper.getLevel(),
                new Pos(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D),
                Direction.UP, ICBMConstants.id("condensed"));
        //long fuse: the entity must be killed before it runs its own blast
        explosive.setFuse(400);
        helper.getLevel().addFreshEntity(explosive);

        triggerEmp(helper);

        helper.assertTrue(explosive.isRemoved(), "emp did not kill the primed explosive");
        helper.assertTrue(explosive.getFuse() > 1,
                "the primed explosive ran its own blast instead of being killed, fuse: " + explosive.getFuse());
        helper.assertBlockPresent(Blocks.STONE, CENTER.below());
        helper.succeed();
    }

    /**
     * Config driven replacements: 1.12 {@code ActionEmpArea#empTile} looked the block state up in
     * {@code EmpHandler#empBlockSwaps} and replaced it before applying any EMP effect.
     * <p>
     * The row is added through {@link icbm.classic.config.util.BlockStateConfigList#setDefault} instead of a
     * config string: the 1.12 text parser cannot read the {@code source=target} form it documents, because
     * {@code ResourceConfigList#KEY_VALUE_REGEX} lets the domain group swallow the {@code =} of a
     * namespaced target ({@code ^([^\s@]*):([^\s=]*)(?:=(\S*))?} matches
     * {@code minecraft:stone=minecraft:gold_block} as the domain {@code minecraft:stone=minecraft}). That
     * defect is carried over unchanged from 1.12 (both regexes are identical, and
     * {@code ConfigEmp.BlockReplacements#blockStates} ships empty in both), so the string form is left as is
     * and the test drives the same lookup with a default row.
     */
    @GameTest(maxTicks = 100)
    public void empReplacesConfiguredBlocks(GameTestHelper helper)
    {
        helper.setBlock(CENTER, Blocks.STONE);
        EmpHandler.empBlockSwaps.setDefault(RegistryLookup.keyOf(Blocks.STONE),
                new BlockReplacementData().setChance(1f).setBlockState(Blocks.GOLD_BLOCK.defaultBlockState()), 0);
        try
        {
            triggerEmp(helper);
            helper.assertBlockPresent(Blocks.GOLD_BLOCK, CENTER);
            helper.succeed();
        }
        finally
        {
            resetEmpReplacements();
        }
    }
}
