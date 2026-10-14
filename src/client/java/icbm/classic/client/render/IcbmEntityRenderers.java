package icbm.classic.client.render;

import icbm.classic.ICBMConstants;
import icbm.classic.client.ICBMClassicClient;
import icbm.classic.client.model.ModelChickenHelmet;
import icbm.classic.client.render.entity.IcbmChickenRenderer;
import icbm.classic.client.render.entity.RenderBombDroplet;
import icbm.classic.client.render.entity.RenderEntityBlock;
import icbm.classic.client.render.entity.RenderExBlock;
import icbm.classic.client.render.entity.RenderFragments;
import icbm.classic.client.render.entity.RenderLightBeam;
import icbm.classic.client.render.entity.RenderMissile;
import icbm.classic.client.render.entity.RenderParachute;
import icbm.classic.client.render.entity.RenderRedmatter;
import icbm.classic.client.render.entity.item.RenderAsItem;
import icbm.classic.content.cargo.balloon.EntityBalloon;
import icbm.classic.content.entity.EntityGrenade;
import icbm.classic.content.reg.ModEntities;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

/**
 * Registers the client renderer of every entity this mod can spawn.
 * <p>
 * 26.3 {@code EntityRenderDispatcher#shouldRender} dereferences the renderer of an entity type without a null
 * check, so a single unregistered type crashes the whole render thread (1.12 only left the entity invisible).
 * Every type registered in {@code ModEntities} therefore has to be registered here as well; 1.12 did the same
 * in {@code client/ClientReg}, see the port ledger for the class by class mapping.
 * <p>
 * The three entity forms the 1.12 renderers drew nothing for (the explosion holder and the smoke cloud, plus
 * the empty base renderer) map onto the 26.3 {@link NoopRenderer}, which is what vanilla registers for its own
 * invisible types ({@code marker}, {@code interaction}, {@code area_effect_cloud}).
 */
public final class IcbmEntityRenderers
{
    private IcbmEntityRenderers() {}

    /**
     * Called from {@link ICBMClassicClient#onInitializeClient()}. Registers the renderers and a client start
     * check that reports any {@code icbmclassic} entity type the live render dispatcher cannot draw.
     */
    public static void register()
    {
        EntityRendererRegistry.register(ModEntities.EXPLOSION_HOLDER, NoopRenderer::new); //1.12 RenderExplosion
        EntityRendererRegistry.register(ModEntities.SMOKE, NoopRenderer::new); //1.12 RenderSmoke
        EntityRendererRegistry.register(ModEntities.PRIMED_EXPLOSIVE, RenderExBlock::new);
        EntityRendererRegistry.register(ModEntities.FRAGMENT, RenderFragments::new);
        EntityRendererRegistry.register(ModEntities.FLYING_BLOCK, RenderEntityBlock::new);
        EntityRendererRegistry.register(ModEntities.LIGHT_BEAM, RenderLightBeam::new);
        EntityRendererRegistry.register(ModEntities.REDMATTER, RenderRedmatter::new);

        //1.12 registered RenderMissile for the EntityMissile base class, 26.3 needs one registration per type
        EntityRendererRegistry.register(ModEntities.EXPLOSIVE_MISSILE, RenderMissile::new);
        EntityRendererRegistry.register(ModEntities.GENERIC_MISSILE, RenderMissile::new);
        EntityRendererRegistry.register(ModEntities.MISSILE_SAM, RenderMissile::new); //1.12 EntitySurfaceToAirMissile
        EntityRendererRegistry.register(ModEntities.HELD_ITEM_MISSILE, RenderMissile::new);
        //1.12: new RenderAsItem<>(rm, EntityGrenade::renderItemStack).setBillboard(true)
        EntityRendererRegistry.register(ModEntities.GRENADE,
                context -> new RenderAsItem<>(context, EntityGrenade::renderItemStack).setBillboard(true));
        //1.12: new RenderAsItem<>(rm, EntityBalloon::getRenderItemStack)
        EntityRendererRegistry.register(ModEntities.BALLOON,
                context -> new RenderAsItem<>(context, EntityBalloon::getRenderItemStack));
        EntityRendererRegistry.register(ModEntities.PARACHUTE, RenderParachute::new);
        EntityRendererRegistry.register(ModEntities.BOMB_DROPLET, RenderBombDroplet::new);
        //1.12 drew nothing for the seat (empty doRender with shadowSize 0), NoopRenderer is the 26.3 form
        EntityRendererRegistry.register(ModEntities.MISSILE_SEAT, NoopRenderer::new);
        //1.12 the bomb cart extended the vanilla TNT minecart and used its renderer, 26.3 needs the type
        //registered against our own entity type
        EntityRendererRegistry.register(ModEntities.BOMB_CART, TntMinecartRenderer::new);

        //Space chicken helmet layer (1.12 ClientProxy#init added it to the vanilla chicken renderer)
        ModelLayerRegistry.registerModelLayer(ModelChickenHelmet.LAYER, ModelChickenHelmet::createLayerDefinition);
        EntityRendererRegistry.register(EntityTypes.CHICKEN, IcbmChickenRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> validateRenderersWhenReady(client));
    }

    /** True once {@link #validateRenderersWhenReady(Minecraft)} reported its result. */
    private static boolean renderersValidated;

    /**
     * The entity renderers are built by {@code EntityRenderDispatcher} during the first client resource reload,
     * which happens after {@code CLIENT_STARTED}, so the check can not run from that event (it reported every
     * type as missing, see the S4.07 evidence). This waits for the dispatcher to answer for a vanilla type and
     * then checks the mod types once.
     */
    private static void validateRenderersWhenReady(Minecraft client)
    {
        if (renderersValidated)
        {
            return;
        }

        final EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        final EntityRenderState probe = new EntityRenderState();
        //A vanilla type answers only after the dispatcher reloaded the renderers
        probe.entityType = EntityTypes.CHICKEN;
        if (dispatcher.getRenderer(probe) == null)
        {
            return;
        }

        renderersValidated = true;
        validateRenderers(dispatcher, probe);
    }

    /**
     * Walks the entity type registry, asks the live {@link EntityRenderDispatcher} for the renderer of every
     * {@code icbmclassic} type and logs an error for the ones it cannot find. This is the check S4.07 asks for:
     * with a missing renderer the client does not just render nothing, it dies on the next frame of that
     * entity.
     */
    private static void validateRenderers(final EntityRenderDispatcher dispatcher, final EntityRenderState probe)
    {
        int checked = 0;

        for (final Identifier key : BuiltInRegistries.ENTITY_TYPE.keySet())
        {
            if (!ICBMConstants.DOMAIN.equals(key.getNamespace()))
            {
                continue;
            }

            final EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(key);
            if (type == null)
            {
                continue;
            }

            checked++;
            probe.entityType = type;
            if (dispatcher.getRenderer(probe) == null)
            {
                ICBMClassicClient.LOGGER.error("No renderer registered for entity type {}, the client will crash when it renders", key);
            }
        }

        ICBMClassicClient.LOGGER.info("Checked the renderer of {} icbmclassic entity types", checked);
    }
}
