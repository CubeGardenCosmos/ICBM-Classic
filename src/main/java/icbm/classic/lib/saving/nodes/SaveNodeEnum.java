package icbm.classic.lib.saving.nodes;

import icbm.classic.ICBMClassic;
import icbm.classic.lib.saving.SaveNode;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SaveNodeEnum<SaveObject, EnumVal extends Enum<EnumVal>> extends SaveNode<SaveObject>
{
    private final Function<SaveObject, EnumVal> save;
    private final BiConsumer<SaveObject, EnumVal> load;
    private final Function<String, EnumVal> accessor;

    public SaveNodeEnum(final String name, Function<SaveObject, EnumVal> save, BiConsumer<SaveObject, EnumVal> load, Function<String, EnumVal> accessor)
    {
        super(name);
        this.save = save;
        this.load = load;
        this.accessor = accessor;
    }

    @Override
    public void save(SaveObject obj, ValueOutput out)
    {
        if (obj != null)
        {
            final EnumVal value = save.apply(obj);
            if (value != null)
            {
                out.putString(getSaveKey(), value.name());
            }
        }
    }

    @Override
    public void load(SaveObject obj, ValueInput in)
    {
        if (obj != null)
        {
            final String raw = in.getString(getSaveKey()).orElse(null);
            if (raw != null)
            {
                try
                {
                    final EnumVal value = accessor.apply(raw);
                    if (value != null)
                    {
                        load.accept(obj, value);
                    }
                    else
                    {
                        ICBMClassic.logger().warn("Failed to load enum '{}' from key '{}', value '{}' has no match. Keeping default", save.apply(obj), getSaveKey(), raw);
                    }
                }
                catch (Exception exception)
                {
                    ICBMClassic.logger().warn("Failed to load enum from key '{}' value '{}', keeping default", getSaveKey(), raw, exception);
                }
            }
        }
    }
}
