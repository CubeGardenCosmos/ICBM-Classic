package icbm.classic.lib.network.lambda;

import icbm.classic.api.radio.IRadioChannelAccess;
import icbm.classic.lib.network.lambda.tile.PacketCodexTile;
import icbm.classic.lib.radio.imp.Radio;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Function;

/**
 * Common codexes shared by radio based tiles (channel and disable toggle).
 *
 * 1.12 used a resource location and a tile entity; 26.3 uses {@link Identifier} and
 * {@link BlockEntity} (see porting-guide.md §2.1/§5).
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public final class GeneralCodexs {

    public static <T extends BlockEntity> PacketCodexTile<T, IRadioChannelAccess> radioChannel(Identifier parent, Function<T, IRadioChannelAccess> converter) {
        return (PacketCodexTile<T, IRadioChannelAccess>) new PacketCodexTile<T, IRadioChannelAccess>(parent, "radio.frequency", IRadioChannelAccess.class, converter)
            .fromClient()
            .nodeString(IRadioChannelAccess::getChannel, IRadioChannelAccess::setChannel)
            .onFinished((tile, target, player) -> tile.setChanged());
    }

    public static <T extends BlockEntity> PacketCodexTile<T, Radio> radioToggleDisable(Identifier parent, Function<T, Radio> converter) {
        return (PacketCodexTile<T, Radio>) new PacketCodexTile<T, Radio>(parent, "radio.disable.toggle", Radio.class, converter)
            .fromClient()
            .toggleBoolean(Radio::isDisabled, Radio::setDisabled)
            .onFinished((tile, target, player) -> tile.setChanged());
    }
}
