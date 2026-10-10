package icbm.classic.test.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Shared round trip helper for block entity save tests. Rebuilds a block entity of the same type from
 * the data minecraft itself persisted, then lets the caller compare the two.
 * <p>
 * Every content class implementing a {@link icbm.classic.lib.saving.SaveHandler} is expected to have at
 * least one "save -> load -> fields equal" round trip test using this helper.
 */
public final class SaveRoundTrip
{
    private SaveRoundTrip()
    {
    }

    /**
     * @param helper  test helper used to report failure
     * @param be      block entity to save, mutated first
     * @param mutate  applied to the block entity before it is saved, may be null
     * @param equals  compares the mutated original with the rebuilt copy
     */
    public static void assertRoundTrip(GameTestHelper helper, BlockEntity be, Consumer<BlockEntity> mutate, BiPredicate<BlockEntity, BlockEntity> equals)
    {
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();

        // change the data so the save has something to carry over
        if (mutate != null)
        {
            mutate.accept(be);
        }

        final CompoundTag tag = be.saveWithoutMetadata(provider);

        final BlockEntity loaded = be.getType().create(be.getBlockPos(), be.getBlockState());
        if (loaded == null)
        {
            helper.fail("Failed to create block entity of type " + be.getType());
            return;
        }
        loaded.setLevel(helper.getLevel());
        loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));

        if (equals == null || !equals.test(be, loaded))
        {
            helper.fail("Block entity " + be.getType() + " did not survive a save/load round trip, saved as " + tag);
        }
    }
}
