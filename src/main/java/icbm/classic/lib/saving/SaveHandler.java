package icbm.classic.lib.saving;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.LinkedList;
import java.util.List;

/**
 * Handles logic for saving/loading without needing to duplicate logic or checks
 * <p>
 * Each content entry should have a single save handler. However, some cases might want a different save
 * handler for game save, packets, spawn data, or other useful cases.
 * <p>
 * To use the handler create a new instance. Then call {@link #addRoot(String)} to create new save groups.
 * Groups are a great way to organize the save for players/admins editing saves. Such as grouping `flags` under
 * a `flag` root or grouping all player settings under `settings`
 * <p>
 * To add things to save group chain nodes and end with a call to {@link SaveRoot#base()} to return to this level.
 * <p>
 * Using {@link #mainRoot()} can allow saving directly to the base save. This is useful for basic content or saving
 * data that doesn't need to be grouped. Works the same as a save group but will direct all nodes to the base save.
 * <p>
 * Example:
 *
 * <pre>
 *     {@code
 *
 *      public static final SaveHandler&lt;Furnace&gt; SAVE_LOGIC = new SaveHandler()
 *      .mainRoot()
 *          .nodeInteger("fuel", (furnace) -&gt; furnace.fuel, (furnace, integer) -&gt; furnace.fuel = integer)
 *     .base()
 *     .addRoot("inventory")
 *          .nodeItemStack("items", (furnace) -&gt; furnace.inventory)
 *          .nodeSavable("settings", (furnace) -&gt; furnace.settings)
 *      .base();
 *
 *     public void load(ValueInput in) {
 *         SAVE_LOGIC.load(this, in);
 *     }
 *
 *     public void save(ValueOutput out) {
 *         SAVE_LOGIC.save(this, out);
 *     }
 *     }
 * </pre>
 *
 * @param <E> type of object to save
 */
public class SaveHandler<E>
{
    private static final String ROOT_KEY = "root";

    private final List<SaveRoot<E>> roots = new LinkedList<SaveRoot<E>>();
    private final SaveRoot<E> mainRoot = new SaveRoot<E>(ROOT_KEY, this, null);

    /**
     * Called to save data as a {@link CompoundTag}. Used where the save has to travel as raw NBT, such
     * as item components, saved data or network payloads.
     *
     * @param objectToSave to pull data from
     * @param provider     registry access used by codecs of complex values
     * @return save object
     */
    public CompoundTag saveToTag(E objectToSave, HolderLookup.Provider provider)
    {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, provider);
        save(objectToSave, output);
        return output.buildResult();
    }

    /**
     * Called to load save data stored as a {@link CompoundTag}, see {@link #saveToTag(Object, HolderLookup.Provider)}.
     *
     * @param objectToLoad to push data back into
     * @param tag          save from minecraft
     * @param provider     registry access used by codecs of complex values
     */
    public void loadFromTag(E objectToLoad, CompoundTag tag, HolderLookup.Provider provider)
    {
        if (tag != null)
        {
            load(objectToLoad, TagValueInput.create(ProblemReporter.DISCARDING, provider, tag));
        }
    }

    /**
     * Called to save data
     *
     * @param objectToSave to pull data from
     * @param out          layer to save against, normally handed in by a block entity or entity
     */
    public void save(E objectToSave, ValueOutput out)
    {
        roots.forEach(root -> root.save(objectToSave, out));
        mainRoot.save(objectToSave, out);
    }

    /**
     * Called to load save data
     *
     * @param objectToLoad to push data back into
     * @param in           save from minecraft
     */
    public void load(E objectToLoad, ValueInput in)
    {
        if (in != null)
        {
            roots.forEach(root -> root.load(objectToLoad, in));
            mainRoot.load(objectToLoad, in);
        }
    }

    /**
     * Adds a new parent root for saving against
     *
     * @param name of the root, can't be "root"
     * @return root created, used .base() to exit back up a layer
     */
    public SaveRoot<E> addRoot(final String name)
    {
        final SaveRoot<E> root = new SaveRoot<E>(name, this, null);
        roots.add(root);
        return root;
    }

    /**
     * Gets the main root for saving directly to the base compound tag
     *
     * @return main root
     */
    public SaveRoot<E> mainRoot()
    {
        return mainRoot;
    }
}
