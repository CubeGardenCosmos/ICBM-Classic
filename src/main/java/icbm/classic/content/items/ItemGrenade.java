package icbm.classic.content.items;

import icbm.classic.content.entity.EntityGrenade;
import icbm.classic.content.reg.ExplosiveForms.ExplosiveForm;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Flat grenade item ({@code <explosive>_grenade}, registry-ids.md §1 form G). Holding it charges a throw
 * (1.12 {@code ItemGrenade#onPlayerStoppedUsing}) that spawns an {@link EntityGrenade} with a throw energy
 * based on how long the item was held.
 * <p>
 * 1.12 used metadata to select the explosive, ADR 0002 flattens that into one item per explosive, so the
 * explosive id is the {@link ExplosiveFormItem#getExplosiveId()} of the item (see
 * {@link icbm.classic.lib.capability.ex.CapabilityExplosiveStack#fromStack}).
 * <p>
 * Porting notes: {@code EnumAction.BOW} → {@link ItemUseAnimation#BOW}, {@code getMaxItemUseDuration} →
 * {@link #getUseDuration(ItemStack, LivingEntity)}, {@code onItemRightClick} →
 * {@link #use(Level, Player, InteractionHand)}, {@code onPlayerStoppedUsing} →
 * {@link #releaseUsing(ItemStack, Level, LivingEntity, int)}. The Forge {@code initCapabilities} override is
 * gone (items carry the explosive id directly) and the 1.12 metadata plumbing
 * ({@code getMetadata}/{@code getSubItems}/{@code getUnlocalizedName}) is dropped, one item per explosive.
 *
 * Created by Dark(DarkGuardsman, Robin) on 1/7/19.
 */
public class ItemGrenade extends ExplosiveFormItem
{
    /** 1.12 {@code ItemGrenade#MAX_USE_DURATION} (3 seconds of charge) */
    public static final int MAX_USE_DURATION = 3 * 20; //TODO config

    /** 1.12 {@code setMaxStackSize(16)} */
    public static final int MAX_STACK_SIZE = 16;

    public ItemGrenade(Identifier explosiveId, ExplosiveForm form, Item.Properties properties)
    {
        super(explosiveId, form, properties);
    }

    /** 1.12 {@code getItemUseAction} returned {@code EnumAction.BOW}. */
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack)
    {
        return ItemUseAnimation.BOW;
    }

    /** 1.12 {@code getMaxItemUseDuration}. */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity)
    {
        return MAX_USE_DURATION;
    }

    /** 1.12 {@code onItemRightClick}: starts charging the throw. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand)
    {
        // 1.12 playerIn.setActiveHand(handIn) -> 26.3 Player#startUsingItem (LivingEntity)
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** 1.12 {@code onPlayerStoppedUsing}: throws the grenade with an energy based on the charge time. */
    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entityLiving, int timeLeft)
    {
        if (!level.isClientSide())
        {
            //Play throw sound
            level.playSound(null, entityLiving.getX(), entityLiving.getY(), entityLiving.getZ(), SoundEvents.TNT_PRIMED,
                    SoundSource.BLOCKS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            //Calculate energy based on player hold time
            final int useDuration = getUseDuration(itemStack, entityLiving);
            final float throwEnergy = (float) (useDuration - timeLeft) / (float) useDuration;

            //Create generate entity
            new EntityGrenade(level)
                    .setItemStack(itemStack)
                    .setThrower(entityLiving)
                    .aimFromThrower()
                    .setThrowMotion(throwEnergy).spawn();

            //Consume item
            // 1.12: !(thrower is the player type) || !player.capabilities.isCreativeMode
            if (!(entityLiving instanceof Player player) || !player.isCreative())
            {
                itemStack.shrink(1);
            }
        }
        return true;
    }

    /**
     * 1.12 {@code hasDetailedInfo}/{@code getDetailedInfo} delegated to {@code ItemBlockExplosive}, which
     * appended the explosive tier.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag)
    {
        appendExplosiveTier(tooltip);
    }
}
