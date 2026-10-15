package icbm.classic.content.blocks.launcher.screen;

import icbm.classic.api.actions.status.IActionStatus;

/** Status of one launcher in the screen's network. */
public class LauncherPair
{
    private final Integer group;
    private final Integer index;
    private final IActionStatus status;

    public LauncherPair(Integer group, Integer index, IActionStatus status)
    {
        this.group = group;
        this.index = index;
        this.status = status;
    }

    public Integer getGroup() { return group; }
    public Integer getIndex() { return index; }
    public IActionStatus getStatus() { return status; }
}
