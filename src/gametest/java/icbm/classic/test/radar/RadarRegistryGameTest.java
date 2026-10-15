package icbm.classic.test.radar;

import icbm.classic.lib.radar.RadarRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;

import java.util.List;
import java.util.Objects;

/**
 * Tests the radar registry (S1.06): entities added to a level's radar map are found by a range query,
 * entities outside the queried range are not, removing an entity drops it, and the {@code entityFilter}
 * hook can veto tracking.
 *
 * <p>1.12 only fed missiles into the map via {@code CapabilityMissile}; the default 26.3 filter accepts
 * everything until S4 restores the missile-only rule, so a vanilla {@code ARMOR_STAND} is used here.</p>
 */
public class RadarRegistryGameTest
{
    /** Small query radius so the far contact falls outside the chunk-bounds approximation too */
    private static final double RANGE = 8;

    private static void assertThat(GameTestHelper helper, String name, Object expected, Object actual)
    {
        if (!Objects.equals(expected, actual))
        {
            helper.fail(name + " mismatch, expected <" + expected + "> but got <" + actual + ">");
        }
    }

    /** Creates an untracked stand entity at the given world position (not added to the level) */
    private static Entity createStand(GameTestHelper helper, double x, double y, double z)
    {
        final Entity entity = EntityTypes.ARMOR_STAND.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (entity == null)
        {
            helper.fail("failed to create armor stand entity");
        }
        entity.setPos(x, y, z);
        return entity;
    }

    /** Creates a stand entity and spawns it into the test level (also feeds it through ENTITY_LOAD) */
    private static Entity spawnStand(GameTestHelper helper, double x, double y, double z)
    {
        final Entity entity = createStand(helper, x, y, z);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static boolean found(GameTestHelper helper, double x, double y, double z, Entity entity)
    {
        final List<Entity> found = RadarRegistry.getAllLivingObjectsWithin(helper.getLevel(), x, y, z, RANGE);
        return found.contains(entity);
    }

    @GameTest
    public void entityWithinRangeIsFound(GameTestHelper helper)
    {

        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final Entity entity = spawnStand(helper, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        // idempotent: ENTITY_LOAD may already have tracked it, either way it must be queryable
        RadarRegistry.add(entity);
        assertThat(helper, "entity found in range", true, found(helper, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, entity));

        RadarRegistry.remove(entity);
        entity.discard();
        helper.succeed();
    }

    @GameTest
    public void entityOutsideRangeIsNotFound(GameTestHelper helper)
    {

        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final double farX = pos.getX() + 128;
        final Entity entity = createStand(helper, farX + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        assertThat(helper, "far entity added", true, RadarRegistry.add(entity));
        assertThat(helper, "far entity not in range", false, found(helper, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, entity));

        RadarRegistry.remove(entity);
        entity.discard();
        helper.succeed();
    }

    @GameTest
    public void removeDropsEntity(GameTestHelper helper)
    {

        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final Entity entity = spawnStand(helper, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        RadarRegistry.add(entity);
        assertThat(helper, "entity found before remove", true, found(helper, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, entity));

        assertThat(helper, "entity removed", true, RadarRegistry.remove(entity));
        assertThat(helper, "entity gone after remove", false, found(helper, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, entity));

        entity.discard();
        helper.succeed();
    }

    @GameTest
    public void entityFilterBlocksAdd(GameTestHelper helper)
    {

        final BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        final Entity entity = createStand(helper, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        final var previous = RadarRegistry.entityFilter;
        RadarRegistry.entityFilter = ignored -> false;
        try
        {
            assertThat(helper, "filtered entity rejected", false, RadarRegistry.add(entity));
            assertThat(helper, "filtered entity not found", false, found(helper, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, entity));
        }
        finally
        {
            RadarRegistry.entityFilter = previous;
        }

        entity.discard();
        helper.succeed();
    }
}
