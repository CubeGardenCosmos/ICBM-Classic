package icbm.classic.api.refs;

import icbm.classic.ICBMConstants;
import net.minecraft.resources.Identifier;

/**
 * Holds references related to ICBM entities
 * <p>
 * IDs are the 26.3 identifiers from {@code docs/architecture/registry-ids.md} §4 (the 1.12 dotted
 * names were replaced by vanilla style snake_case).
 *
 * Created by Robin Seifert on 1/7/19.
 */
public final class ICBMEntities
{
    public static final Identifier BLOCK_GRAVITY = ICBMConstants.id("flying_block");
    public static final Identifier BLOCK_FRAGMENT = ICBMConstants.id("fragment");
    public static final Identifier BLOCK_EXPLOSIVE = ICBMConstants.id("primed_explosive");

    public static final Identifier MISSILE_EXPLOSIVE = ICBMConstants.id("explosive_missile");
    public static final Identifier MISSILE_GENERIC = ICBMConstants.id("generic_missile");
    public static final Identifier MISSILE_SAM = ICBMConstants.id("anti_ballistic_missile");
    public static final Identifier MISSILE_HELD_ITEM = ICBMConstants.id("held_item_missile");

    public static final Identifier EXPLOSION = ICBMConstants.id("explosion_holder");
    public static final Identifier BEAM = ICBMConstants.id("light_beam");
    public static final Identifier GRENADE = ICBMConstants.id("grenade");
    public static final Identifier BOMB_CART = ICBMConstants.id("bomb_cart");
    public static final Identifier MISSILE_SEAT = ICBMConstants.id("missile_seat");
    public static final Identifier BOMB_DROPLET = ICBMConstants.id("bomblet");
    public static final Identifier PARACHUTE = ICBMConstants.id("parachute");
    public static final Identifier BALLOON = ICBMConstants.id("balloon");

    public static final Identifier REDMATTER = ICBMConstants.id("redmatter");

    public static final Identifier SMOKE = ICBMConstants.id("smoke");
}
