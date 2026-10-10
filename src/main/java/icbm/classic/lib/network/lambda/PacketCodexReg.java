package icbm.classic.lib.network.lambda;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of packet codexes, keyed by {@link PacketCodex#getCodexId()}.
 *
 * 1.12 used an incrementing integer id for each codex; 26.3 uses the codex id (parent path + name) instead,
 * see porting-guide.md §5.
 */
public class PacketCodexReg
{
    private static final Map<Identifier, PacketCodex<?, ?>> builders = new HashMap<>();

    public static void register(PacketCodex<?, ?>... codex)
    {
        for (PacketCodex<?, ?> entry : codex)
        {
            register(entry);
        }
    }

    public static void register(PacketCodex<?, ?> codex)
    {
        if (builders.containsKey(codex.getCodexId()))
        {
            throw new RuntimeException("Codex already registered for " + codex);
        }
        builders.put(codex.getCodexId(), codex);
    }

    public static PacketCodex<?, ?> get(Identifier codexId)
    {
        return builders.get(codexId);
    }
}
