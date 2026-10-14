package icbm.classic.content.blast;

import icbm.classic.ICBMConstants;
import icbm.classic.api.explosion.IBlastTickable;
import icbm.classic.content.blast.threaded.BlastThreaded;
import icbm.classic.content.entity.flyingblock.EntityFlyingBlock;
import icbm.classic.content.entity.flyingblock.FlyingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Anti gravity blast, floats the blocks of the sphere around the center as flying blocks and lifts the
 * entities above them for {@link #RUNNING_TICKS} ticks.
 * <p>
 * Port notes (porting-guide.md §2, docs/architecture/overview.md §6):
 * <ul>
 *     <li>1.12 held a stale {@code ThreadSmallExplosion} field that {@code setupBlast()} started next to a
 *     {@code doRun} override; the port keeps the {@code doRun} form only and the setup override is gone.
 *     {@link BlastThreaded} starts the worker from {@code doExplode} and the worker reads the
 *     {@code LevelSnapshot} instead of the level.</li>
 *     <li>1.12 had the antigravity sound playback commented out next to the thread start; that sound is not in
 *     the ported sound registry, so it is still not played.</li>
 *     <li>The results are consumed by this blast one block per tick (1.12 {@code thread.isComplete} +
 *     {@code Blast#getThreadResults()}), so {@link #onWorkerResultsReady(List)} only stores them and the base
 *     class must not apply them through the block edit queue.</li>
 *     <li>{@code entity.yawChange/pitchChange} → {@code setYawChange}/{@code setPitchChange},
 *     {@code entity.setGravity(0)} → {@code setProjectileGravity(0)} and {@code entity.motionX/Y/Z} writes →
 *     {@code Entity#setDeltaMovement(double, double, double)}.</li>
 *     <li>{@code world().rand} → {@code Level#getRandom()}, {@code entity.posY} → {@code Entity#getY()},
 *     {@code entity.motionY} → {@code Entity#getDeltaMovement()} and
 *     {@code world().getEntitiesWithinAABB(...)} → {@code Level#getEntitiesOfClass(...)}.</li>
 * </ul>
 */
public class BlastAntiGravitational extends BlastThreaded implements IBlastTickable {

    public static final int RUNNING_TICKS = ICBMConstants.TICKS_MIN * 2;
    protected Set<EntityFlyingBlock> flyingBlocks = new HashSet<EntityFlyingBlock>();
    List<BlockPos> results;

    int searchIndex = 0;

    @Override
    public boolean doRun(int loops, Consumer<BlockPos> edits) {
        BlastHelpers.forEachPosInRadius(this.getBlastRadius(), (x, y, z) -> {
            edits.accept(new BlockPos(xi() + x, yi() + y, zi() + z));
        });
        return false;
    }

    @Override
    public boolean doExplode(int callCount)
    {
        //1.12 started the thread in setupBlast, the ported base starts the worker on the first tick
        //(the call is a no-op once the worker is running)
        super.doExplode(callCount);

        if (world() != null && !this.world().isClientSide()) //1.12: !this.world().isRemote
        {
            if (isThreadCompleted()) //1.12: this.thread.isComplete
            {
                if (results == null) {
                    results = new ArrayList<>(getThreadResults());
                    Collections.shuffle(results);
                    results.sort(Comparator.comparingInt(r -> -r.getY()));
                    getThreadResults().clear(); //1.12: this.threadResults.clear()
                }

                if (searchIndex >= results.size()) {
                    searchIndex = 0;
                }

                // Search until we find a single block
                for (; searchIndex < results.size(); searchIndex++) {
                    final BlockPos targetPosition = results.get(searchIndex);

                    if (FlyingBlock.spawnFlyingBlock(world(), targetPosition, (entity) -> {
                        entity.setYawChange(50 * world().getRandom().nextFloat()); //1.12: entity.yawChange
                        entity.setPitchChange(100 * world().getRandom().nextFloat()); //1.12: entity.pitchChange
                        //1.12: entity.motionY += Math.max(1 * world().rand.nextFloat(), 1)
                        entity.setDeltaMovement(entity.getDeltaMovement().add(0, Math.max(1 * world().getRandom().nextFloat(), 1), 0));

                        double deltaX = targetPosition.getX() - this.location.getX();
                        double deltaZ = targetPosition.getZ() - this.location.getZ();
                        double mag = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                        deltaX /= mag;
                        deltaZ /= mag;

                        //1.12: entity.motionX += deltaX * (1 - world().rand.nextFloat())
                        //      entity.motionZ += deltaZ * (1 - world().rand.nextFloat())
                        entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * (1 - world().getRandom().nextFloat()), 0, deltaZ * (1 - world().getRandom().nextFloat())));

                        entity.setProjectileGravity(0); //1.12: entity.setGravity(0)
                        entity.setInAirKillTime(RUNNING_TICKS * 2);
                    }, entityFlyingBlock -> {
                        flyingBlocks.add(entityFlyingBlock);
                    })) {
                        break;
                    }
                }
            }
        }

        int radius = (int) this.getBlastRadius();
        final int affectHeight = Math.max(radius, 100); //TODO config affect height
        //1.12 built the box with location.y() + radius as the max X value, kept as is
        AABB bounds = new AABB(location.x() - radius, location.y() - radius, location.z() - radius, location.y() + radius, location.y() + affectHeight, location.z() + radius);
        //1.12: world().getEntitiesWithinAABB(Entity.class, bounds)
        List<Entity> allEntities = world().getEntitiesOfClass(Entity.class, bounds);

        for (Entity entity : allEntities) {
            if (entity.getY() < affectHeight + location.y()) { //1.12: entity.posY
                if (entity.getDeltaMovement().y < 0.4) { //1.12: entity.motionY
                    entity.push(0, 0.15, 0); //1.12: entity.motionY += 0.15
                }
            }
        }

        return this.callCount > RUNNING_TICKS;
    }

    /**
     * This blast consumes the worker result one block per tick in {@link #doExplode(int)}, so the base class
     * must not apply the positions through the block edit queue (that would remove every collected block at once
     * and mark the blast finished).
     *
     * @param edits sorted positions the worker collected
     */
    @Override
    protected void onWorkerResultsReady(List<BlockPos> edits)
    {
        this.threadResults = edits;
        this.threadComplete = true;
    }

    @Override
    protected void onBlastCompleted() {
        flyingBlocks.forEach(EntityFlyingBlock::restoreGravity);
        super.onBlastCompleted(); //BlastThreaded#onBlastCompleted -> endBlast()
    }
}
