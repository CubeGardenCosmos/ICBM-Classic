package icbm.classic.client.render;

import icbm.classic.ICBMConstants;
import icbm.classic.client.ICBMClassicClient;
import icbm.classic.client.model.CruiseLauncherTopModel;
import icbm.classic.client.render.tile.NoopTileRenderer;
import icbm.classic.client.render.tile.cruise.CruiseLauncherRenderer;
import icbm.classic.client.render.tile.emptower.EmpTowerRenderer;
import icbm.classic.client.render.tile.launcher.LauncherBaseRenderer;
import icbm.classic.content.reg.ModBlockEntities;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

/** Client registrations for block-entity renderers and their startup completeness check (S5.07). */
public final class IcbmBlockEntityRenderers
{
    private IcbmBlockEntityRenderers() {}

    public static void register()
    {
        ModelLayerRegistry.registerModelLayer(CruiseLauncherTopModel.LAYER, CruiseLauncherTopModel::createLayerDefinition);

        BlockEntityRendererRegistry.register(ModBlockEntities.LAUNCHER_BASE, LauncherBaseRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.CRUISE_LAUNCHER, CruiseLauncherRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.EMP_TOWER,
                context -> new EmpTowerRenderer<icbm.classic.content.blocks.emptower.TileEMPTower>(context));
        BlockEntityRendererRegistry.register(ModBlockEntities.EMP_TOWER_FAKE,
                context -> new EmpTowerRenderer<icbm.classic.content.blocks.emptower.TileEmpTowerFake>(context));

        // These forms have static block models and deliberately submit no extra geometry, but still need an
        // explicit renderer entry for the complete icbmclassic block-entity self-check.
        BlockEntityRendererRegistry.register(ModBlockEntities.EXPLOSIVE, NoopTileRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.LAUNCHER_FRAME, NoopTileRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.LAUNCHER_CONNECTOR, NoopTileRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.LAUNCHER_SCREEN, NoopTileRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.RADAR_STATION, NoopTileRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(IcbmBlockEntityRenderers::validateRenderersWhenReady);
    }

    private static boolean renderersValidated;

    private static void validateRenderersWhenReady(Minecraft client)
    {
        if (renderersValidated) return;

        final BlockEntityRenderDispatcher dispatcher = client.getBlockEntityRenderDispatcher();
        final BlockEntityRenderState probe = new BlockEntityRenderState();
        probe.blockEntityType = BlockEntityTypes.CHEST;
        if (dispatcher.getRenderer(probe) == null) return;

        renderersValidated = true;
        validateRenderers(dispatcher, probe);
    }

    private static void validateRenderers(BlockEntityRenderDispatcher dispatcher, BlockEntityRenderState probe)
    {
        int checked = 0;
        for (final Identifier key : BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet())
        {
            if (!ICBMConstants.DOMAIN.equals(key.getNamespace())) continue;

            final BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(key);
            if (type == null) continue;

            checked++;
            probe.blockEntityType = type;
            if (dispatcher.getRenderer(probe) == null)
            {
                ICBMClassicClient.LOGGER.error("No renderer registered for block entity type {}", key);
            }
        }
        ICBMClassicClient.LOGGER.info("Checked the renderer of {} icbmclassic block entity types", checked);
    }
}
