package icbm.classic.api.events;

import icbm.classic.api.actions.IAction;
import icbm.classic.api.explosion.IBlast;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base class for any event fired by the EMP system.
 * <p>
 * 1.12 extended the Forge {@code Event} class with {@code @Cancelable} sub classes. 26.3 uses a Fabric
 * {@code Event} per fired variant; the cancelable variants use a callback returning {@code boolean}
 * (any listener returning false cancels the EMP effect, matching the 1.12 behavior).
 *
 *
 * Created by Dark(DarkGuardsman, Robin) on 3/12/2018.
 *
 * @deprecated will be replaced by an event providing {@link icbm.classic.api.actions.IAction}
 */
@Deprecated
public abstract class EmpEvent extends BlastEvent<IBlast>
{
    public EmpEvent(IAction blast)
    {
        super(blast);
    }

    /**
     * Called before an entity is hit with an EMP effect. Allows canceling the effect for any reason.
     * <p>
     * Canceling the effect will remove any side effects that are normally applied. This includes
     * mutating some entities, apply some effects, and adding EMP effects to items.
     */
    public static class EntityPre extends EmpEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onEntityPre(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public final Entity target;

        public EntityPre(IAction emp, Entity target)
        {
            super(emp);
            this.target = target;
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the EMP and target
             * @return false to cancel the EMP effect
             */
            boolean onEntityPre(EntityPre event);
        }
    }

    /**
     * Called after EMP effects have been applied to the entity. This includes several different
     * effects and EMP effects on items.
     */
    public static class EntityPost extends EmpEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        listener.onEntityPost(event);
                    }
                });

        public final Entity target;

        public EntityPost(IAction emp, Entity target)
        {
            super(emp);
            this.target = target;
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the EMP and target
             */
            void onEntityPost(EntityPost event);
        }
    }

    /**
     * Called before a block is hit with an EMP effect. Allows canceling the effect for any reason.
     * <p>
     * Canceling the effect will remove any side effects that are normally applied. This includes
     * mutating some entities, apply some effects, and adding EMP effects to items.
     */
    public static class BlockPre extends EmpEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onBlockPre(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public final Level world;
        public final BlockPos blockPos;
        public final BlockState state;

        public BlockPre(IAction emp, Level world, BlockPos pos, BlockState state)
        {
            super(emp);
            this.world = world;
            this.blockPos = pos;
            this.state = state;
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the EMP, block position and state
             * @return false to cancel the EMP effect
             */
            boolean onBlockPre(BlockPre event);
        }
    }

    /**
     * Called after EMP effects have been applied to the block.
     */
    public static class BlockPost extends EmpEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        listener.onBlockPost(event);
                    }
                });

        public final Level world;
        public final BlockPos blockPos;
        public final BlockState state;

        public BlockPost(IAction emp, Level world, BlockPos pos, BlockState state)
        {
            super(emp);
            this.world = world;
            this.blockPos = pos;
            this.state = state;
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the EMP, block position and state
             */
            void onBlockPost(BlockPost event);
        }
    }
}
