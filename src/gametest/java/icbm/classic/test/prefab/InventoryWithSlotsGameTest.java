package icbm.classic.test.prefab;

import icbm.classic.prefab.inventory.InventorySlot;
import icbm.classic.prefab.inventory.InventoryWithSlots;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Covers {@link InventoryWithSlots} (S5.01): the 1.12 {@code ItemStackHandler} save round trip, the slot
 * filters/insert checks and the per tick slot handler.
 */
public class InventoryWithSlotsGameTest
{
    /**
     * Three slots: a diamond only slot with an insert check that can be switched off, an unconfigured slot and
     * a slot with a per tick handler.
     */
    private static final class TestInventory extends InventoryWithSlots
    {
        private final List<String> changes = new ArrayList<>();
        private boolean allowInsert = true;

        TestInventory()
        {
            super(3);
            withChangeCallback((slot, stack) -> changes.add(slot + "=" + stack.getCount()));
            withSlot(new InventorySlot(0, stack -> stack.getItem() == Items.DIAMOND)
                .withInsertCheck(stack -> allowInsert)
                .withTick(stack -> new ItemStack(Items.DIAMOND, Math.min(stack.getCount() + 1, 64))));
        }
    }

    private static void assertThat(GameTestHelper helper, String name, Object expected, Object actual)
    {
        if (!Objects.equals(expected, actual))
        {
            helper.fail(name + " mismatch, expected <" + expected + "> but got <" + actual + ">");
        }
    }

    private static void assertStack(GameTestHelper helper, String name, ItemStack expected, ItemStack actual)
    {
        if (!ItemStack.isSameItemSameComponents(expected, actual) || expected.getCount() != actual.getCount())
        {
            helper.fail(name + " mismatch, expected <" + expected + "> but got <" + actual + ">");
        }
    }

    @GameTest
    public void inventoryWithSlotsSurvivesSaveRoundTrip(GameTestHelper helper)
    {
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();

        final TestInventory inventory = new TestInventory();
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 5));
        inventory.setItem(2, new ItemStack(Items.STONE, 12));

        //1.12 ItemStackHandler#serializeNBT -> 26.3 ISavable#save into a ValueOutput (ContainerHelper)
        final TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        inventory.save(out);
        final CompoundTag tag = out.buildResult();
        assertThat(helper, "saved items", true, tag.contains(ContainerHelper.TAG_ITEMS));

        final TestInventory loaded = new TestInventory();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));

        assertStack(helper, "slot 0", new ItemStack(Items.DIAMOND, 5), loaded.getItem(0));
        assertStack(helper, "slot 1", ItemStack.EMPTY, loaded.getItem(1));
        assertStack(helper, "slot 2", new ItemStack(Items.STONE, 12), loaded.getItem(2));

        // the filters are code side and have to behave the same after the load
        assertThat(helper, "diamond allowed in slot 0", true, loaded.canPlaceItem(0, new ItemStack(Items.DIAMOND)));
        assertThat(helper, "stone rejected in slot 0", false, loaded.canPlaceItem(0, new ItemStack(Items.STONE)));
        assertThat(helper, "slot without handler rejects everything", false, loaded.canPlaceItem(1, new ItemStack(Items.DIAMOND)));

        helper.succeed();
    }

    @GameTest
    public void inventoryWithSlotsFiltersInsertsAndTicks(GameTestHelper helper)
    {
        final TestInventory inventory = new TestInventory();

        // slot validator + insert check
        assertThat(helper, "validator blocks stone", false, inventory.isItemValid(0, new ItemStack(Items.STONE)));
        assertThat(helper, "validator allows diamond", true, inventory.isItemValid(0, new ItemStack(Items.DIAMOND)));
        inventory.allowInsert = false;
        assertStack(helper, "insert check blocks the insert", new ItemStack(Items.DIAMOND, 4), inventory.insertItem(0, new ItemStack(Items.DIAMOND, 4), false));
        inventory.allowInsert = true;
        assertStack(helper, "first insert has no leftover", ItemStack.EMPTY, inventory.insertItem(0, new ItemStack(Items.DIAMOND, 40), false));
        assertStack(helper, "leftover of a partly fitting insert", new ItemStack(Items.DIAMOND, 16),
            inventory.insertItem(0, new ItemStack(Items.DIAMOND, 40), false));
        assertStack(helper, "inserted stack capped at the stack size", new ItemStack(Items.DIAMOND, 64), inventory.getItem(0));

        // removing works through the vanilla container api
        assertStack(helper, "extracted stack", new ItemStack(Items.DIAMOND, 10), inventory.extractItem(0, 10, false));
        assertStack(helper, "rest after extraction", new ItemStack(Items.DIAMOND, 54), inventory.getItem(0));

        // content changes are reported with their slot, the container level change with -1
        inventory.changes.clear();
        inventory.setItem(1, new ItemStack(Items.STONE, 2));
        inventory.setChanged();
        assertThat(helper, "change callback log", "[1=2, -1=0]", inventory.changes.toString());

        // the slot tick handler only runs on the server (1.12 ItemStackHandler#update)
        inventory.update(0, false);
        assertStack(helper, "client tick leaves the slot alone", new ItemStack(Items.DIAMOND, 54), inventory.getItem(0));
        inventory.update(0, true);
        assertStack(helper, "server tick grows the slot", new ItemStack(Items.DIAMOND, 55), inventory.getItem(0));

        helper.succeed();
    }
}
