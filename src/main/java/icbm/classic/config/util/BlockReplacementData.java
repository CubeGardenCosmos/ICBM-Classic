package icbm.classic.config.util;

import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Handles block placement with a chance
 * <p>
 * 26.3 port note: NBT payloads are applied through {@link TagValueOutput}/{@link TagValueInput} and the
 * block entity's public {@code saveCustomOnly}/{@code loadCustomOnly} methods, which map to the 1.12
 * behaviour of writing to/reading from a 1.12 style compound tag without touching the internal tags.
 */
@Data
@Accessors(chain = true)
public class BlockReplacementData {

    /** Block state to place */
    private net.minecraft.world.level.block.state.BlockState blockState;

    /** NBT data to load into the block */
    private CompoundTag blockNBT = null;

    /** Chance of placement, 1 is always and 0 is never */
    private float chance = -1;

    public void apply(Level level, BlockPos pos) {
        if (shouldPlace(level)) {
            placeBlock(level, pos);
            applyNBT(level, pos);
        }
    }

    private boolean shouldPlace(Level level) {
        return getChance() < 0 || level.getRandom().nextFloat() <= getChance();
    }

    private void placeBlock(Level level, BlockPos pos) {
        if (getBlockState() != null) {
            level.setBlock(pos, getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }

    private void applyNBT(Level level, BlockPos pos) {
        if (blockNBT != null && !blockNBT.isEmpty()) {
            final BlockEntity tileEntity = level.getBlockEntity(pos);
            if (tileEntity != null) {
                final TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                tileEntity.saveCustomOnly(out);
                final CompoundTag existingSave = out.buildResult();

                // Remove internal tags to prevent issues
                existingSave.remove("id");
                existingSave.remove("x");
                existingSave.remove("y");
                existingSave.remove("z");

                existingSave.merge(blockNBT);

                final ValueInput in = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), existingSave);
                tileEntity.loadCustomOnly(in);
            }
        }
    }
}
