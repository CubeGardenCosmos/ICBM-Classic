package icbm.classic.test.blast;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.IAction;
import icbm.classic.api.actions.cause.IActionSource;
import icbm.classic.api.explosion.IBlast;
import icbm.classic.api.explosion.IBlastInit;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.actions.causeby.CauseByTimer;
import icbm.classic.content.blast.ender.EnderBlastCustomization;
import icbm.classic.content.missile.logic.source.ActionSource;
import icbm.classic.lib.data.ExplosiveSettings;
import com.mojang.serialization.DynamicOps;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Covers the ender half of step S3.07:
 *
 * <ul>
 *     <li>the ender blast teleports the entities in range to the target carried by
 *     {@link ExplosiveSettings#enderTarget()} / {@link EnderBlastCustomization},</li>
 *     <li>the {@code ender_target} field of the explosive settings component survives a codec round trip
 *     and an absent field still loads (the S2.01 record had no target),</li>
 *     <li>the customization keeps the 1.12 "pos" + "dim" save keys.</li>
 * </ul>
 *
 * The blast is created through {@link ICBMClassicAPI#EXPLOSIVE_REGISTRY} (like
 * {@code BasicBlastsGameTest}) with the size forced to 3 to keep the test fast. The ender blast itself
 * ignores that size, its radius is the 1.12 fixed 20 ({@code BlastEnder#getBlastRadius}).
 */
public class EnderBlastGameTest
{
    /** Center of the blast, relative to the test structure */
    private static final BlockPos CENTER = new BlockPos(4, 3, 4);
    /**
     * Ender target, relative to the test structure. Kept inside the 8x8x8 structure (coords 0..7) so the
     * teleport stays in a loaded chunk and 4+ blocks away from the center so the blast does not teleport the
     * entity a second time.
     */
    private static final BlockPos TARGET = new BlockPos(1, 3, 1);
    /** Size every blast is forced to, keeps the affected area inside the test structure */
    private static final int TEST_SIZE = 3;

    @GameTest(maxTicks = 60)
    public void enderBlastTeleportsEntitiesToCustomizationTarget(GameTestHelper helper)
    {
        // floor under the target so the teleported zombie lands instead of falling through the structure
        helper.setBlock(TARGET.below(), Blocks.STONE);

        final Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(CENTER.getX() + 0.5D, CENTER.getY() + 1.0D, CENTER.getZ() + 0.5D));

        // target comes from the explosive settings component, EnderBlastCustomization is its view (the 1.12
        // ExplosiveInit#encodeEnderCoordSet stored the same pair on the explosive capability of the item)
        final GlobalPos target = new GlobalPos(helper.getLevel().dimension(), helper.absolutePos(TARGET));
        final EnderBlastCustomization customization = EnderBlastCustomization.fromSettings(new ExplosiveSettings(target));
        if (customization == null)
        {
            helper.fail("the explosive settings did not produce an ender customization");
            return;
        }

        final IExplosiveData data = ICBMClassicAPI.EXPLOSIVE_REGISTRY.getExplosiveData(ICBMConstants.id("ender"), false);
        final BlockPos pos = helper.absolutePos(CENTER);
        final Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        final IActionSource source = new ActionSource(helper.getLevel(), center, new CauseByTimer("test", 0));

        final IAction action = data.create(helper.getLevel(), center.x, center.y, center.z, source, null);
        if (action instanceof IBlastInit init)
        {
            init.setBlastSize(TEST_SIZE);
        }
        // 1.12 applied the customization of the item/block to the built blast; the block (S3.09) and the
        // missile (S5) do the same with the customization read from the explosive settings component
        customization.apply(data, (IBlast) action);
        action.doAction();

        // the target is inside the structure, checks the horizontal position and that the entity landed
        final Vec3 targetPosition = new Vec3(target.pos().getX() + 0.5D, target.pos().getY(), target.pos().getZ() + 0.5D);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        Math.abs(zombie.getX() - targetPosition.x) < 1.0D && Math.abs(zombie.getZ() - targetPosition.z) < 1.0D
                                && zombie.getY() >= targetPosition.y - 0.5D && zombie.getY() <= targetPosition.y + 1.0D,
                        "the ender blast did not teleport the zombie to " + targetPosition + ", zombie at " + zombie.position()))
                .thenSucceed();
    }

    @GameTest(maxTicks = 20)
    public void explosiveSettingsCodecCarriesEnderTarget(GameTestHelper helper)
    {
        final DynamicOps<Tag> ops = NbtOps.INSTANCE;
        final GlobalPos target = new GlobalPos(helper.getLevel().dimension(), helper.absolutePos(TARGET));

        final Tag encoded = ExplosiveSettings.CODEC.encodeStart(ops, new ExplosiveSettings(target)).getOrThrow();
        final ExplosiveSettings decoded = ExplosiveSettings.CODEC.parse(ops, encoded).getOrThrow();
        helper.assertTrue(Objects.equals(target, decoded.enderTarget().orElse(null)),
                "ender_target did not survive the explosive settings codec, encoded as " + encoded);

        // backwards compatible: a save/component without ender_target still decodes, with an empty target
        final ExplosiveSettings legacy = ExplosiveSettings.CODEC.parse(ops, new CompoundTag()).getOrThrow();
        helper.assertTrue(legacy.enderTarget().isEmpty(),
                "explosive settings without an ender_target field must load as empty, got " + legacy);

        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void enderCustomizationSavesTargetUnder12Keys(GameTestHelper helper)
    {
        final HolderLookup.Provider provider = helper.getLevel().registryAccess();
        final EnderBlastCustomization saved = new EnderBlastCustomization(
                new GlobalPos(helper.getLevel().dimension(), helper.absolutePos(TARGET)));

        final TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        saved.save(out);
        final CompoundTag tag = out.buildResult();

        // 1.12 saved the position with nodeVec3d("pos") and the dimension with nodeInteger("dim")
        helper.assertTrue(tag.contains("pos"), "the save dropped the 1.12 \"pos\" key: " + tag);
        helper.assertTrue(tag.contains("dim"), "the save dropped the 1.12 \"dim\" key: " + tag);

        final EnderBlastCustomization loaded = new EnderBlastCustomization();
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));
        helper.assertTrue(Objects.equals(saved.toGlobalPos(), loaded.toGlobalPos()),
                "the ender target did not survive a save/load round trip, saved as " + tag);

        helper.succeed();
    }
}
