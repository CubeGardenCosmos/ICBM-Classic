package icbm.classic.lib.radio.imp;

import icbm.classic.api.data.IBoundBox;
import icbm.classic.api.radio.IRadioMessage;
import icbm.classic.api.radio.IRadioReceiver;
import icbm.classic.api.radio.IRadioSender;
import icbm.classic.lib.radio.RadioRegistry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class RadioTile<T extends BlockEntity> extends Radio {

    protected final T host;

    public RadioTile(T host) {
        this.host = host;
    }

    @Override
    public BlockPos getBlockPos() {
        return host.getBlockPos();
    }

    @Override
    public Level getWorld() {
        return host.getLevel();
    }

    @Override
    public IBoundBox<BlockPos> getRange() {
        return RadioRegistry.INFINITE;
    }

    public boolean canReceive(IRadioSender sender, IRadioMessage packet) {
        return !isDisabled()
            // Usually sender isn't receive, but could happen in rare cases
            && sender != this
            // Only accept server side
            && host.getLevel() != null && !host.getLevel().isClientSide()
            // Validate channel, this might create the channel string if null
            && Objects.equals(getChannel(), packet.getChannel());
    }

    /**
     * Registers the radio with {@link RadioRegistry} for the level of its host. Idempotent: an already
     * registered radio only has its range/cache updated. 1.12 called {@code RadioRegistry.add} from the
     * host's {@code onLoad}/{@code validate}; 26.3 has no such hook, so the host (S5) calls this on its
     * first tick instead (step card S1.05).
     */
    public void ensureRegistered() {
        // Only receiver radios were added to the registry in 1.12; sender only radios (e.g. the radar)
        // are not registrable this way, so guard on the subtype instead of registering blindly.
        if(this instanceof IRadioReceiver receiver) {
            RadioRegistry.addOrUpdate(receiver);
        }
    }

    /**
     * Removes the radio from {@link RadioRegistry}. 1.12 called {@code RadioRegistry.remove} from the
     * host's {@code invalidate}; the 26.3 host calls this from {@code setRemoved} (step card S1.05).
     */
    public void unregister() {
        if(this instanceof IRadioReceiver receiver) {
            RadioRegistry.remove(receiver);
        }
    }
}
