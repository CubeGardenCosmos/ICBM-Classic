package icbm.classic.api.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;

/**
 * Called when a missile tracker loads or unloads a chunk.
 * <p>
 * 1.12 carried the content class {@code LoadedChunkPair} plus a {@code ForgeChunkManager.Ticket}.
 * Neither exists in 26.3: the {@code api} package must not reference content (step card S1.10) and the
 * Forge chunk manager was removed, so the event now carries only the level id, the chunk and the time
 * the chunk should stay loaded. The tracker (S4.03) is responsible for its own chunk ticket and for
 * reading {@link #timeLeft} back after the event.
 * <p>
 * 1.12 posted these on the Forge event bus with {@code @Cancelable}. 26.3 uses a Fabric {@code Event}
 * per variant whose callback returns {@code boolean} (false cancels the load/unload).
 */
public class MissileChunkEvent
{
    /** Level the chunk belongs to */
    public final Identifier dimension;
    /** Chunk being loaded or unloaded */
    public final ChunkPos chunkPos;
    /** Ticks left until the unload should happen, listeners may change this */
    public int timeLeft;

    public MissileChunkEvent(Identifier dimension, ChunkPos chunkPos, int timeLeft)
    {
        this.dimension = dimension;
        this.chunkPos = chunkPos;
        this.timeLeft = timeLeft;
    }

    /**
     * Called when a missile is about to load a chunk.
     * Use this to change the amount of time the chunk
     * should be loaded for, or cancel this event
     * to not load the chunk.
     */
    public static class Load extends MissileChunkEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onLoad(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public Load(Identifier dimension, ChunkPos chunkPos, int timeLeft)
        {
            super(dimension, chunkPos, timeLeft);
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the level, chunk and load time
             * @return false to not load the chunk
             */
            boolean onLoad(Load event);
        }
    }

    /**
     * Called when a chunk loaded by a missile is about
     * to be unloaded. Cancel this event to not unload the
     * chunk. Make sure to change the time the chunk should
     * be loaded for or another attempt to unload the chunk
     * will be made in the next world tick.
     */
    public static class Unload extends MissileChunkEvent
    {
        public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class,
                listeners -> event -> {
                    for (Callback listener : listeners) {
                        if (!listener.onUnload(event)) {
                            return false;
                        }
                    }
                    return true;
                });

        public Unload(Identifier dimension, ChunkPos chunkPos, int timeLeft)
        {
            super(dimension, chunkPos, timeLeft);
        }

        @FunctionalInterface
        public interface Callback
        {
            /**
             * @param event carrying the level, chunk and remaining load time
             * @return false to not unload the chunk
             */
            boolean onUnload(Unload event);
        }
    }
}
