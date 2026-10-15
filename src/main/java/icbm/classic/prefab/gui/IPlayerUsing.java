package icbm.classic.prefab.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Used to track players currently using an object. Primaryly used
 * for GUI handling.
 * <p>
 * 1.12 tracked a player; 26.3 only the server player is handed to the machines (menus exist on
 * both sides but the machine data and the codex packets are server side), so the list is
 * {@code List<ServerPlayer>} and a client side player is ignored. The packet helper of the machines
 * ({@code PACKET_GUI.sendPacketToGuiUsers(this, playersUsing)}) is server side only in 1.12 as well.
 * <p>
 * Created by Robin on 1/12/2015.
 */
public interface IPlayerUsing
{
    List<ServerPlayer> getPlayersUsing();

    default boolean addPlayerToUseList(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            return getPlayersUsing().add(serverPlayer);
        }
        return false;
    }

    default boolean removePlayerToUseList(Player player)
    {
        return getPlayersUsing().remove(player);
    }
}
