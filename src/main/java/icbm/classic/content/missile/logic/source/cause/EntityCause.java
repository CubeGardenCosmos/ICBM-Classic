package icbm.classic.content.missile.logic.source.cause;

import icbm.classic.ICBMConstants;
import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.actions.cause.IActionCause;
import icbm.classic.api.actions.cause.ICausedByEntity;
import icbm.classic.api.reg.obj.IBuilderRegistry;
import icbm.classic.lib.saving.SaveHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * General purpose entity cause
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EntityCause extends ActionCause implements ICausedByEntity {

    public static final Identifier REG_NAME = ICBMConstants.id("entity");

    private String name;
    private UUID id;
    private boolean isPlayer;

    // runtime cache vars
    private Entity entity;

    public EntityCause(Entity source) {
        entity = source;
        name = source.getName().getString();
        id = source.getUUID();
        isPlayer = source instanceof Player;
    }

    /**
     * 1.12 handed back the runtime cache only, which is empty after a save/load of the cause.
     * <p>
     * 26.3 asks the level for the entity by UUID instead ({@code Level#getEntity(UUID)}, the 1.12
     * {@code ServerLevel#getEntity(UUID)} equivalent). The answer is null while the chunk holding the
     * entity is not loaded, in that case the caller can try again later. Any hit is cached in {@link #entity}.
     *
     * @param level to resolve the entity in
     * @return entity or null when it is not loaded in the level
     */
    public Entity getEntity(Level level) {
        if (entity == null && id != null && level != null) {
            entity = level.getEntity(id);
        }
        return entity;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @NonNull
    @Override
    public Identifier getRegistryKey() {
        return REG_NAME;
    }

    @NonNull
    @Override
    public IBuilderRegistry<IActionCause> getRegistry() {
        return ICBMClassicAPI.ACTION_CAUSE_REGISTRY;
    }

    @Override
    public void save(ValueOutput out) {
        super.save(out);
        SAVE_LOGIC.save(this, out);
    }

    @Override
    public void load(ValueInput in) {
        super.load(in);
        SAVE_LOGIC.load(this, in);
    }


    private static final SaveHandler<EntityCause> SAVE_LOGIC = new SaveHandler<EntityCause>()
        .mainRoot()
        /* */.nodeString("name", EntityCause::getName, EntityCause::setName)
        /* */.nodeUUID("uuid", EntityCause::getId, EntityCause::setId)
        /* */.nodeBoolean("player", EntityCause::isPlayer, EntityCause::setPlayer)
        .base();
}
