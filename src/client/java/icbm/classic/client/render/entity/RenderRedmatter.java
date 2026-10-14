package icbm.classic.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import icbm.classic.ICBMConstants;
import icbm.classic.client.render.entity.state.RedmatterRenderState;
import icbm.classic.config.blast.ConfigBlast;
import icbm.classic.content.blast.redmatter.EntityRedmatter;
import icbm.classic.lib.colors.ColorB;
import icbm.classic.lib.colors.ColorHelper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the redmatter core: a black sphere (the hole), a translucent pulsing shell, the rotating black disc
 * and the random light beams around it.
 * <p>
 * 26.3 port notes (porting-guide.md §2.4):
 * <ul>
 *     <li>1.12 drew everything with raw OpenGL calls ({@code gl11} spheres, hand written quads and triangle
 *     lists, immediate mode blend state). 26.3 does not allow direct graphics API calls, the backend may be
 *     OpenGL or Vulkan, so every piece is submitted through
 *     {@link SubmitNodeCollector}: the sphere becomes a triangle mesh, the disc a textured quad pair and the
 *     beams the same triangle spikes the ender dragon death animation uses. The maths, order and colours of
 *     the 1.12 renderer are kept.</li>
 *     <li>The entity position is no longer translated by the renderer, the 26.3 dispatcher already puts the
 *     {@link PoseStack} at the interpolated entity position before {@link #submit} runs. 1.12 translated by
 *     {@code x, y, z} inside each sub render.</li>
 *     <li>1.12 {@code doRender} never called {@code super.doRender}, so the core has no name tag. Kept.</li>
 *     <li>The beam colours come from a static list that 1.12 only ever appended to, so the random colours
 *     survive every redmatter and every world. Kept (golden rule: do not fix 1.12 behaviour).</li>
 *     <li>1.12 {@code doRender} drew with the {@code visualSize} of the previous frame and lerped afterwards.
 *     {@link #extractRenderState} copies first and lerps last, which is the same value the 1.12 renderer saw.
 *     </li>
 * </ul>
 */
public class RenderRedmatter extends EntityRenderer<EntityRedmatter, RedmatterRenderState>
{
    public static final Identifier TEXTURE_FILE = Identifier.fromNamespaceAndPath(ICBMConstants.DOMAIN, ICBMConstants.TEXTURE_DIRECTORY + "entity/redmatter/blackhole.png");
    public static final Identifier GREY_TEXTURE = Identifier.fromNamespaceAndPath(ICBMConstants.DOMAIN, ICBMConstants.TEXTURE_DIRECTORY + "models/grey.png");

    /** Full brightness light value, the 1.12 renderer disabled lighting for every part it drew unlit. */
    private static final int FULL_BRIGHT = 15728880;

    public static final List<ColorB> randomColorsForBeams = new ArrayList<>();

    public ColorB colorIn = new ColorB(255, 255, 255); //TODO figure out how this works
    public ColorB colorOut = new ColorB(0, 0, 0);

    public RenderRedmatter(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public RedmatterRenderState createRenderState()
    {
        return new RedmatterRenderState();
    }

    @Override
    public void extractRenderState(EntityRedmatter entity, RedmatterRenderState state, float partialTicks)
    {
        super.extractRenderState(entity, state, partialTicks);
        //Copy the size the 1.12 renderer drew with, then run the client side lerp for the next frame
        state.visualSize = entity.clientVisualSize;
        state.ticksExisted = entity.tickCount;
        entity.lerpClientVisualSize(partialTicks);
    }

    @Override
    public void submit(RedmatterRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera)
    {
        submitDisk(state, poseStack, submitNodeCollector);
        submitSphere(state, poseStack, submitNodeCollector);
        submitBeams(state, poseStack, submitNodeCollector);
    }

    /** 1.12 {@code renderDisk}: the rotating black hole texture, drawn on both faces. */
    private void submitDisk(RedmatterRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector)
    {
        final float size = (float) Math.max(0.2, ConfigBlast.redmatter.RENDER_SCALE * state.visualSize * 3);
        final RenderType renderType = RenderTypes.entityTranslucent(TEXTURE_FILE);

        //1.12 colour(1, 0, 0, 1)
        //TODO pick color randomly and pulse the color using sin(angle)
        final int color = ARGB.color(255, 0, 0);

        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, -state.ticksExisted);
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            addTexturedVertex(buffer, pose, color, -size, 0, -size, 0, 0);
            addTexturedVertex(buffer, pose, color, -size, 0, size, 0, 1);
            addTexturedVertex(buffer, pose, color, size, 0, size, 1, 1);
            addTexturedVertex(buffer, pose, color, size, 0, -size, 1, 0);
        });

        //Bottom render, 1.12 rotated the matrix 180 degrees around X and mirrored the texture
        poseStack.rotateDegrees(Axis.XP, 180);
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            addTexturedVertex(buffer, pose, color, -size, 0, -size, 1, 1);
            addTexturedVertex(buffer, pose, color, -size, 0, size, 1, 0);
            addTexturedVertex(buffer, pose, color, size, 0, size, 0, 0);
            addTexturedVertex(buffer, pose, color, size, 0, -size, 0, 1);
        });
        poseStack.popPose();
    }

    /** 1.12 {@code renderSphere}: an opaque black hole plus the translucent shell that pulses every 40 ticks. */
    private void submitSphere(RedmatterRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector)
    {
        final float radius = Math.max(ConfigBlast.redmatter.RENDER_SCALE * state.visualSize, 0.1f);
        final int ticks = state.ticksExisted % 40;

        //Inside sphere: 1.12 colour(0, 0, 0, 1) with lighting still enabled, so it reads the entity light
        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entitySolid(GREY_TEXTURE), (pose, buffer) -> {
            submitSphereMesh(buffer, pose, radius * 0.8f, ARGB.color(255, 0, 0, 0), state.lightCoords);
        });

        //Outside sphere: 1.12 colour(0, 0, 0.2, 0.8) with lighting disabled
        final float scaleSize = 0.0005f;
        final float fullSize = radius * scaleSize * 20;
        final float scaleDelta;
        if (ticks > 20)
        {
            scaleDelta = fullSize - (radius * scaleSize * (ticks - 20));
        }
        else
        {
            scaleDelta = radius * scaleSize * ticks;
        }

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(GREY_TEXTURE), (pose, buffer) -> {
            submitSphereMesh(buffer, pose, radius + scaleDelta, ARGB.colorFromFloat(0.8f, 0.0f, 0.0f, 0.2f), FULL_BRIGHT);
        });
    }

    /**
     * 1.12 {@code renderBeams}: a copy of the ender dragon death rays, with the animation reshaped so it ramps
     * up and down over {@code totalAnimationTime} ticks. The random sequence (seed 432) and the six random
     * rotations per beam are the 1.12 ones.
     */
    private void submitBeams(RedmatterRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector)
    {
        final int totalAnimationTime = 4000;
        final int rotationAnimationTime = 200;
        final float animationPhaseTime = totalAnimationTime / 2f;

        final int ticks = state.ticksExisted % totalAnimationTime;

        //Controls rotation of beams
        final float rotationScale = (state.ticksExisted % rotationAnimationTime) / (float) rotationAnimationTime;

        //Controls beam count and other factors based on animation phase time
        final float timeScale;
        if (ticks < animationPhaseTime)
        {
            timeScale = (ticks / animationPhaseTime);
        }
        else
        {
            timeScale = (2 - (ticks / animationPhaseTime));
        }

        //0 - 61 for scale of 1
        final int beamCount = (int) ((timeScale + timeScale * timeScale) / 2.0F * 30.0F);
        final float visualSize = state.visualSize;
        final RandomSource redmatterBeamRandom = RandomSource.create(432L);
        final ColorB colorOutDefault = this.colorOut;
        final ColorB colorIn = this.colorIn;

        submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.dragonRays(), (pose, buffer) -> {
            final PoseStack.Pose beamPose = new PoseStack.Pose();
            final Vector3f center = new Vector3f();
            final Vector3f tipA = new Vector3f();
            final Vector3f tipB = new Vector3f();
            final Vector3f tipC = new Vector3f();

            for (int beamIndex = 0; beamIndex < beamCount; ++beamIndex)
            {
                //Calculate size
                final float beamLength = (redmatterBeamRandom.nextFloat() + 1F) * visualSize;
                final float beamWidth = (float) Math.max(0.1, redmatterBeamRandom.nextFloat()) * visualSize / 10;

                //Random rotations TODO see if we need to rotate so much
                beamPose.set(pose);
                beamPose.rotateDegrees(Axis.XP, redmatterBeamRandom.nextFloat() * 360.0F);
                beamPose.rotateDegrees(Axis.YP, redmatterBeamRandom.nextFloat() * 360.0F);
                beamPose.rotateDegrees(Axis.ZP, redmatterBeamRandom.nextFloat() * 360.0F);
                beamPose.rotateDegrees(Axis.XP, redmatterBeamRandom.nextFloat() * 360.0F);
                beamPose.rotateDegrees(Axis.YP, redmatterBeamRandom.nextFloat() * 360.0F);
                beamPose.rotateDegrees(Axis.ZP, redmatterBeamRandom.nextFloat() * 360.0F + rotationScale * 360.0F);

                //Get color based on state
                ColorB colorOut = colorOutDefault;
                if (ConfigBlast.redmatter.RENDER_COLORED_BEAMS)
                {
                    if (beamIndex < randomColorsForBeams.size())
                    {
                        colorOut = randomColorsForBeams.get(beamIndex);
                    }
                    else
                    {
                        //to get rainbow, pastel colors https://stackoverflow.com/questions/4246351/creating-random-colour-in-java
                        final float hue = redmatterBeamRandom.nextFloat();
                        final float saturation = 0.9f;//1.0 for brilliant, 0.0 for dull
                        final float luminance = 0.5f; //1.0 for brighter, 0.0 for black
                        colorOut = ColorHelper.HSBtoRGB(hue, saturation, luminance);

                        randomColorsForBeams.add(colorOut);
                    }
                }

                //Draw spike shape: the center vertex and the three tips of the 1.12 triangle fan. 1.12 emitted
                //it as a fan (center, A, B, C, A), the 26.3 dragon ray pipeline takes triangles, so the fan is
                //spelled out as the three triangles it describes: (center, A, B), (center, B, C), (center, C, A)
                center.set(0.0F, 0.0F, 0.0F);
                tipA.set(-0.866D * beamWidth, beamLength, -0.5F * beamWidth);
                tipB.set(0.866D * beamWidth, beamLength, -0.5F * beamWidth);
                tipC.set(0.0F, beamLength, 1.0F * beamWidth);

                final int innerColor = toVertexColor(colorIn);
                final int outerColor = toVertexColor(colorOut);
                addBeamVertex(buffer, beamPose, center, innerColor);
                addBeamVertex(buffer, beamPose, tipA, outerColor);
                addBeamVertex(buffer, beamPose, tipB, outerColor);

                addBeamVertex(buffer, beamPose, center, innerColor);
                addBeamVertex(buffer, beamPose, tipB, outerColor);
                addBeamVertex(buffer, beamPose, tipC, outerColor);

                addBeamVertex(buffer, beamPose, center, innerColor);
                addBeamVertex(buffer, beamPose, tipC, outerColor);
                addBeamVertex(buffer, beamPose, tipA, outerColor);
            }
        });
    }

    private static void addBeamVertex(VertexConsumer buffer, PoseStack.Pose pose, Vector3f vertex, int color)
    {
        buffer.addVertex(pose, vertex).setColor(color);
    }

    private static void addTexturedVertex(VertexConsumer buffer, PoseStack.Pose pose, int color, float x, float y, float z, float u, float v)
    {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /**
     * Submits a sphere as triangles: 32 slices by 32 stacks, the same resolution the 1.12 renderer asked the
     * immediate mode sphere helper for. The 1.12 helper put the poles on the Z axis, this one puts them on Y;
     * the texture is a flat grey and the colour a solid fill, so the rotation has no visible effect.
     */
    private static void submitSphereMesh(VertexConsumer buffer, PoseStack.Pose pose, float radius, int color, int light)
    {
        final int stacks = 32;
        final int slices = 32;

        for (int stack = 0; stack < stacks; stack++)
        {
            final float lat0 = (float) (-Math.PI / 2.0 + Math.PI * stack / stacks);
            final float lat1 = (float) (-Math.PI / 2.0 + Math.PI * (stack + 1) / stacks);
            final float y0 = (float) Math.sin(lat0) * radius;
            final float y1 = (float) Math.sin(lat1) * radius;
            final float r0 = (float) Math.cos(lat0) * radius;
            final float r1 = (float) Math.cos(lat1) * radius;
            final float v0 = (float) stack / stacks;
            final float v1 = (float) (stack + 1) / stacks;

            for (int slice = 0; slice < slices; slice++)
            {
                //counter clockwise seen from outside: (x00, y0) -> (x10, y1) -> (x11, y1) -> (x01, y0)
                final float lon0 = (float) (2.0 * Math.PI * slice / slices);
                final float lon1 = (float) (2.0 * Math.PI * (slice + 1) / slices);
                final float x00 = (float) Math.cos(lon0) * r0;
                final float z00 = (float) Math.sin(lon0) * r0;
                final float x01 = (float) Math.cos(lon1) * r0;
                final float z01 = (float) Math.sin(lon1) * r0;
                final float x10 = (float) Math.cos(lon0) * r1;
                final float z10 = (float) Math.sin(lon0) * r1;
                final float x11 = (float) Math.cos(lon1) * r1;
                final float z11 = (float) Math.sin(lon1) * r1;
                final float u0 = (float) slice / slices;
                final float u1 = (float) (slice + 1) / slices;

                addSphereVertex(buffer, pose, color, light, radius, x00, y0, z00, u0, v0);
                addSphereVertex(buffer, pose, color, light, radius, x10, y1, z10, u0, v1);
                addSphereVertex(buffer, pose, color, light, radius, x11, y1, z11, u1, v1);
                addSphereVertex(buffer, pose, color, light, radius, x01, y0, z01, u1, v0);
            }
        }
    }

    private static void addSphereVertex(VertexConsumer buffer, PoseStack.Pose pose, int color, int light, float radius,
                                        float x, float y, float z, float u, float v)
    {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, x / radius, y / radius, z / radius);
    }

    /**
     * 1.12 wrote the byte based {@link ColorB} components into a normalised unsigned vertex colour (the parts
     * are stored as signed bytes, see {@link ColorB}), so they are read back as unsigned bytes here.
     */
    private static int toVertexColor(ColorB color)
    {
        return ARGB.color(Byte.toUnsignedInt(color.getAlpha()), Byte.toUnsignedInt(color.getRed()),
                Byte.toUnsignedInt(color.getGreen()), Byte.toUnsignedInt(color.getBlue()));
    }
}
