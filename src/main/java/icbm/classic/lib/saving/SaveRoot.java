package icbm.classic.lib.saving;

import icbm.classic.api.reg.obj.IBuildableObject;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.lib.saving.nodes.*;
import icbm.classic.lib.transform.rotation.EulerAngle;
import icbm.classic.lib.transform.vector.Pos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SaveRoot<SaveObject> implements ISaveNode<SaveObject>
{
    private final String name;
    private final SaveHandler<SaveObject> handler;
    private final SaveRoot<SaveObject> parent;
    public final List<ISaveNode<SaveObject>> nodes = new LinkedList<ISaveNode<SaveObject>>();

    private boolean shouldSave = true;

    public SaveRoot(String name, SaveHandler<SaveObject> handler, SaveRoot<SaveObject> parent)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("save key can't be null");
        }
        this.name = name;
        this.handler = handler;
        this.parent = parent;
    }

    public SaveRoot<SaveObject> disableSave() {
        shouldSave = false;
        return this;
    }

    @Override
    public String getSaveKey()
    {
        return name;
    }

    /**
     * Main root of a handler writes straight to the layer it is given, any other root nests itself
     * inside a child compound of that layer.
     *
     * @return true when this is {@link SaveHandler#mainRoot()}
     */
    private boolean isMainRoot()
    {
        return handler != null && handler.mainRoot() == this;
    }

    @Override
    public void save(SaveObject objectToSave, ValueOutput out)
    {
        if (!shouldSave)
        {
            return;
        }
        if (isMainRoot())
        {
            nodes.forEach(node -> node.save(objectToSave, out));
        }
        else
        {
            final ValueOutput child = out.child(name);
            nodes.forEach(node -> node.save(objectToSave, child));
            if (child.isEmpty())
            {
                out.discard(name);
            }
        }
    }

    @Override
    public void load(SaveObject objectToLoad, ValueInput in)
    {
        if (!shouldSave)
        {
            return;
        }
        if (isMainRoot())
        {
            nodes.forEach(node -> node.load(objectToLoad, in));
        }
        else
        {
            in.child(name).ifPresent(child -> nodes.forEach(node -> node.load(objectToLoad, child)));
        }
    }

    public SaveRoot<SaveObject> addRoot(final String name)
    {
        final SaveRoot<SaveObject> root = new SaveRoot<SaveObject>(name, handler, this);
        nodes.add(root);
        return root;
    }

    public SaveRoot<SaveObject> node(SaveNode<SaveObject> node)
    {
        nodes.add(node);
        return this;
    }

    public SaveRoot<SaveObject> nodeString(final String name, Function<SaveObject, String> save, BiConsumer<SaveObject, String> load)
    {
        return node(new SaveNodeString<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeIdentifier(final String name, Function<SaveObject, Identifier> save, BiConsumer<SaveObject, Identifier> load)
    {
        return node(new SaveNodeIdentifier<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeCompoundTag(final String name, Function<SaveObject, CompoundTag> save, BiConsumer<SaveObject, CompoundTag> load)
    {
        return node(new SaveNodeCompoundTag<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeByte(final String name, Function<SaveObject, Byte> save, BiConsumer<SaveObject, Byte> load)
    {
        return node(new SaveNodeByte<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeInteger(final String name, Function<SaveObject, Integer> save, BiConsumer<SaveObject, Integer> load)
    {
        return node(new SaveNodeInteger<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeDouble(final String name, Function<SaveObject, Double> save, BiConsumer<SaveObject, Double> load)
    {
        return node(new SaveNodeDouble<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeFloat(final String name, Function<SaveObject, Float> save, BiConsumer<SaveObject, Float> load)
    {
        return node(new SaveNodeFloat<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeBoolean(final String name, Function<SaveObject, Boolean> save, BiConsumer<SaveObject, Boolean> load)
    {
        return node(new SaveNodeBoolean<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeBlockPos(final String name, Function<SaveObject, BlockPos> save, BiConsumer<SaveObject, BlockPos> load)
    {
        return node(new SaveNodeBlockPos<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeVec3(final String name, Function<SaveObject, Vec3> save, BiConsumer<SaveObject, Vec3> load)
    {
        return node(new SaveNodeVec3<SaveObject>(name, save, load));
    }

    /**
     * Saves the dimension as an {@link Identifier} (1.12 stored the integer dimension id). Levels are
     * resolved by the caller on load as a save handler has no access to the server, e.g.
     * {@code server.getLevel(ResourceKey.create(Registries.LEVEL, key))}.
     *
     * @param name key to store the dimension under
     * @param load receives the stored identifier
     */
    public SaveRoot<SaveObject> nodeWorldDim(final String name, Function<SaveObject, Level> save, BiConsumer<SaveObject, Identifier> load)
    {
        return node(new SaveNode<SaveObject>(name)
        {
            @Override
            public void save(SaveObject saveObject, ValueOutput out)
            {
                if (saveObject != null)
                {
                    final Level level = save.apply(saveObject);
                    if (level != null)
                    {
                        out.store(getSaveKey(), Identifier.CODEC, level.dimension().identifier());
                    }
                }
            }

            @Override
            public void load(SaveObject saveObject, ValueInput in)
            {
                if (saveObject != null)
                {
                    in.read(getSaveKey(), Identifier.CODEC).ifPresent(key -> load.accept(saveObject, key));
                }
            }
        });
    }

    @Deprecated
    public SaveRoot<SaveObject> nodePos(final String name, Function<SaveObject, Pos> save, BiConsumer<SaveObject, Pos> load)
    {
        return node(new SaveNodePos<SaveObject>(name, save, load));
    }

    @Deprecated
    public SaveRoot<SaveObject> nodeEulerAngle(final String name, Function<SaveObject, EulerAngle> save, BiConsumer<SaveObject, EulerAngle> load)
    {
        return node(new SaveNode<SaveObject>(name)
        {
            @Override
            public void save(SaveObject saveObject, ValueOutput out)
            {
                if (saveObject != null)
                {
                    final EulerAngle angle = save.apply(saveObject);
                    if (angle != null)
                    {
                        angle.save(out.child(getSaveKey()));
                    }
                }
            }

            @Override
            public void load(SaveObject saveObject, ValueInput in)
            {
                if (saveObject != null)
                {
                    in.child(getSaveKey()).ifPresent(child -> load.accept(saveObject, new EulerAngle(child)));
                }
            }
        });
    }

    public <EnumVal extends Enum<EnumVal>> SaveRoot<SaveObject> nodeEnumString(final String name, Function<SaveObject, EnumVal> save, BiConsumer<SaveObject, EnumVal> load, Function<String, EnumVal> accessor) {
        return node(new SaveNodeEnum<SaveObject, EnumVal>(name, save, load, accessor));
    }

    @Deprecated //switch to nodeEnumString for better human readability of save data
    public SaveRoot<SaveObject> nodeDirection(final String name, Function<SaveObject, Direction> save, BiConsumer<SaveObject, Direction> load)
    {
        return node(new SaveNodeDirection<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeBlockState(final String name, Function<SaveObject, BlockState> save, BiConsumer<SaveObject, BlockState> load)
    {
        return node(new SaveNodeBlockState<SaveObject>(name, save, load));
    }

    public SaveRoot<SaveObject> nodeUUID(final String name, Function<SaveObject, UUID> save, BiConsumer<SaveObject, UUID> load)
    {
        return node(new SaveNodeUUID<SaveObject>(name, save, load));
    }

    public <BuildableObject extends IBuildableObject> SaveRoot<SaveObject> nodeBuildableObject(final String name,
                                                                                              final Supplier<IBuilderRegistry<BuildableObject>> reg, Function<SaveObject, BuildableObject> getter, BiConsumer<SaveObject, BuildableObject> setter)
    {
        return node(new SaveBuildableObject<SaveObject, BuildableObject>(name, reg, getter, setter));
    }

    public <BuildableObject extends IBuildableObject> SaveRoot<SaveObject> nodeBuildableObjectList(final String name,
                                                                                                 final Supplier<IBuilderRegistry<BuildableObject>> reg, Function<SaveObject, Collection<BuildableObject>> getter)
    {
        return node(new SaveBuildableObjectList<SaveObject, BuildableObject>(name, reg, getter));
    }

    public SaveRoot<SaveObject> nodeSavable(final String name, final Function<SaveObject, ISavable> getter)
    {
        return node(new SaveNode<SaveObject>(name)
        {
            @Override
            public void save(SaveObject saveObject, ValueOutput out)
            {
                if (saveObject != null)
                {
                    final ISavable object = getter.apply(saveObject);
                    if (object != null)
                    {
                        object.save(out.child(getSaveKey()));
                    }
                }
            }

            @Override
            public void load(SaveObject saveObject, ValueInput in)
            {
                if (saveObject != null)
                {
                    final ISavable object = getter.apply(saveObject);
                    if (object != null)
                    {
                        object.load(in.childOrEmpty(getSaveKey()));
                    }
                }
            }
        });
    }

    public <C extends ISavable> SaveRoot<SaveObject> nodeSavable(final String name, Function<SaveObject, C> save, BiConsumer<SaveObject, C> load, Supplier<C> builder)
    {
        return node(new SaveNode<SaveObject>(name)
        {
            @Override
            public void save(SaveObject saveObject, ValueOutput out)
            {
                if (saveObject != null)
                {
                    final C object = save.apply(saveObject);
                    if (object != null)
                    {
                        object.save(out.child(getSaveKey()));
                    }
                }
            }

            @Override
            public void load(SaveObject saveObject, ValueInput in)
            {
                if (saveObject != null)
                {
                    in.child(getSaveKey()).ifPresent(child -> {
                        final C object = builder.get();
                        if (object != null)
                        {
                            object.load(child);
                        }
                        load.accept(saveObject, object);
                    });
                }
            }
        });
    }

    public SaveRoot<SaveObject> nodeItemStack(String name, Function<SaveObject, ItemStack> save, BiConsumer<SaveObject, ItemStack> load) {
        return node(new SaveNodeItemStack<SaveObject>(name, save, load));
    }

    /**
     * Goes up one nested level
     *
     * @return parent, can be null if this root is at base
     */
    public SaveRoot<SaveObject> parent()
    {
        return parent;
    }

    /**
     * Goes to top most level in save tree
     *
     * @return handler
     */
    public SaveHandler<SaveObject> base()
    {
        return handler;
    }
}
