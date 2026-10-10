package icbm.classic.content.cluster.bomblet;

import icbm.classic.api.ICBMClassicAPI;
import icbm.classic.api.ICBMClassicHelpers;
import icbm.classic.api.caps.IExplosive;
import icbm.classic.api.reg.IExplosiveData;
import icbm.classic.content.blocks.explosive.ItemBlockExplosive;
import icbm.classic.content.reg.BlockReg;
import icbm.classic.lib.capability.ex.CapabilityExplosiveStack;
import icbm.classic.lib.capability.ex.CapabilityExplosiveStatic;
import icbm.classic.prefab.item.ItemBase;
import icbm.classic.prefab.item.ItemStackCapProvider;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemBombDroplet extends ItemBase {

    public ItemBombDroplet()
    {
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
        this.setMaxStackSize(16);
    }

    @Override
    @Nullable
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable NBTTagCompound nbt) {
        final ItemStackCapProvider provider = new ItemStackCapProvider(stack);
        provider.add("explosive", ICBMClassicAPI.EXPLOSIVE_CAPABILITY, new CapabilityExplosiveStack(stack));
        provider.add("projectile", ICBMClassicAPI.PROJECTILE_STACK_CAPABILITY, new BombletProjectileStack(stack::copy));
        return provider;
    }

    @Override
    public int getMetadata(int damage)
    {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack itemstack)
    {
        if (itemstack.hasCapability(ICBMClassicAPI.EXPLOSIVE_CAPABILITY, null))
        {
            final IExplosive explosive = itemstack.getCapability(ICBMClassicAPI.EXPLOSIVE_CAPABILITY, null);
            if (explosive != null)
            {
                final IExplosiveData data = explosive.getExplosiveData();
                if (data != null)
                {
                    return "bomblet." + data.getRegistryKey();
                }
            }
        }
        return super.getUnlocalizedName();
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (tab == getCreativeTab() || tab == CreativeTabs.SEARCH)
        {
            for (int id : ICBMClassicAPI.EX_GRENADE_REGISTRY.getExplosivesIDs())
            {
                items.add(new ItemStack(this, 1, id));
            }
        }
    }

    @Override
    protected boolean hasDetailedInfo(ItemStack stack, EntityPlayer player)
    {
        return true;
    }

    @Override
    protected void getDetailedInfo(ItemStack stack, EntityPlayer player, List list)
    {
        //TODO add hook
        ((ItemBlockExplosive) Item.getItemFromBlock(BlockReg.blockExplosive)).getDetailedInfo(stack, player, list);
        final IExplosive explosive = ICBMClassicHelpers.getExplosive(stack);
        if(explosive != null) { //TODO make shift-key display?
            explosive.collectInformation(list::add);
        }
    }
}
