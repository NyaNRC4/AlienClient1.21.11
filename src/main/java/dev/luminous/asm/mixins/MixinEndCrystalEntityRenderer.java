package dev.luminous.asm.mixins;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import dev.luminous.asm.accessors.IModelPart;
import dev.luminous.asm.accessors.IModelPartCuboid;
import dev.luminous.mod.modules.impl.render.Chams;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.model.*;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalEntityRenderer.class)
public abstract class MixinEndCrystalEntityRenderer extends EntityRenderer<EndCrystalEntity, EndCrystalEntityRenderState> {
    protected MixinEndCrystalEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Shadow
    @Final
    private static Identifier TEXTURE;

    @Shadow
    @Final
    private EndCrystalEntityModel model;

    @Unique
    private static final Identifier BLANK = Identifier.ofVanilla("textures/blank.png");

    @Unique
    private static final RenderLayer END_CRYSTAL_BLANK = RenderLayers.entityTranslucent(BLANK);
    @Unique
    private static RenderLayer END_CRYSTAL_CUSTOM;
    @Unique
    private static RenderLayer CRYSTAL_CUSTOM_NO_DEPTH;

    @Unique
    private static final RenderPipeline ENTITY_TRANSLUCENT_NO_DEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.of("alienclient", "pipeline/entity_translucent_no_depth"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withSampler("Sampler1")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );

    @Unique
    private static final RenderPipeline POSITION_COLOR_NO_DEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("alienclient", "pipeline/chams_crystal_color_no_depth"))
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );

    @Unique
    private static final RenderPipeline POSITION_COLOR_DEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("alienclient", "pipeline/chams_crystal_color_depth"))
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );

    @Unique
    private static final RenderPipeline LINES_NO_DEPTH = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("alienclient", "pipeline/chams_crystal_lines_no_depth"))
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );

    @Unique
    private static RenderLayer crystalLayer() {
        if (END_CRYSTAL_CUSTOM == null) {
            END_CRYSTAL_CUSTOM = RenderLayers.entityTranslucent(TEXTURE);
        }
        return END_CRYSTAL_CUSTOM;
    }

    @Unique
    private static RenderLayer crystalNoDepthLayer() {
        if (CRYSTAL_CUSTOM_NO_DEPTH == null) {
            CRYSTAL_CUSTOM_NO_DEPTH = RenderLayer.of(
                    "alienclient_crystal_custom_no_depth",
                    RenderSetup.builder(ENTITY_TRANSLUCENT_NO_DEPTH)
                            .texture("Sampler0", TEXTURE)
                            .useLightmap()
                            .useOverlay()
                            .translucent()
                            .outlineMode(RenderSetup.OutlineMode.NONE)
                            .build()
            );
        }
        return CRYSTAL_CUSTOM_NO_DEPTH;
    }

    @Unique
    private static final RenderLayer CRYSTAL_COLOR_NO_DEPTH_LAYER = RenderLayer.of(
            "alienclient_crystal_color_no_depth",
            RenderSetup.builder(POSITION_COLOR_NO_DEPTH).translucent().build()
    );

    @Unique
    private static final RenderLayer CRYSTAL_COLOR_DEPTH_LAYER = RenderLayer.of(
            "alienclient_crystal_color_depth",
            RenderSetup.builder(POSITION_COLOR_DEPTH).translucent().build()
    );

    @Unique
    private static final RenderLayer CRYSTAL_LINES_NO_DEPTH_LAYER = RenderLayer.of(
            "alienclient_crystal_lines_no_depth",
            RenderSetup.builder(LINES_NO_DEPTH).build()
    );

    @Unique
    private static final float SINE_45_DEGREES = MathHelper.sin(0.7853982F);
    @Unique
    private static final float PI3 = (float) Math.PI / 3F;

    @Unique
    private static final ModelPart OUTER_PART;
    @Unique
    private static final ModelPart CORE_PART;

    static {
        ModelData outerData = new ModelData();
        outerData.getRoot().addChild("outer", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), ModelTransform.NONE);
        OUTER_PART = TexturedModelData.of(outerData, 64, 32).createModel().getChild("outer");

        ModelData coreData = new ModelData();
        coreData.getRoot().addChild("core", ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), ModelTransform.NONE);
        CORE_PART = TexturedModelData.of(coreData, 64, 32).createModel().getChild("core");
    }

    @Unique
    private float yOffset(float age, Chams module) {
        float f = age * module.floatValue.getValueFloat();
        float g = MathHelper.sin(f * 0.2F) / 2.0F + 0.5F;
        g = (g * g + g) * 0.4F * module.bounceHeight.getValueFloat();
        return g - 1.4F + module.floatOffset.getValueFloat();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void render(EndCrystalEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        Chams module = Chams.INSTANCE;
        if (!module.customCrystal()) {
            return;
        }

        ci.cancel();
        float partialTick = state.age - MathHelper.floor(state.age);
        float age = module.spinSync.getValue() ? module.age + partialTick : state.age;
        float h = yOffset(age, module);
        float j = age * 3.0F * module.spinValue.getValueFloat();
        float scale = 2.0F * module.scale.getValueFloat();

        RenderLayer layer = module.texture.getValue() ? crystalLayer() : END_CRYSTAL_BLANK;
        boolean glint = module.glint.getValue();
        int light = state.light;
        int overlay = OverlayTexture.DEFAULT_UV;

        matrixStack.push();
        matrixStack.scale(scale, scale, scale);
        matrixStack.translate(0.0F, -0.5F, 0.0F);

        this.model.base.visible = state.baseVisible;
        if (state.baseVisible) {
            submitVanilla(queue, matrixStack, this.model.base, layer, light, overlay, -1, glint);
        }
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));
        matrixStack.translate(0.0F, 1.5F + h / 2.0F, 0.0F);
        matrixStack.multiply(new Quaternionf().setAngleAxis(PI3, SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));

        if (module.outerFrame.booleanValue) {
            if (module.custom.getValue()) {
                submitCustom(queue, matrixStack, OUTER_PART, module);
            }
            submitVanilla(queue, matrixStack, OUTER_PART, layer, light, overlay, module.outerFrame.getValue().getRGB(), glint);
        }

        matrixStack.scale(0.875F, 0.875F, 0.875F);
        matrixStack.multiply(new Quaternionf().setAngleAxis(PI3, SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));

        if (module.innerFrame.booleanValue) {
            if (module.custom.getValue()) {
                submitCustom(queue, matrixStack, OUTER_PART, module);
            }
            submitVanilla(queue, matrixStack, OUTER_PART, layer, light, overlay, module.innerFrame.getValue().getRGB(), glint);
        }

        matrixStack.scale(0.875F, 0.875F, 0.875F);
        matrixStack.multiply(new Quaternionf().setAngleAxis(PI3, SINE_45_DEGREES, 0.0F, SINE_45_DEGREES));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(j));

        if (module.core.booleanValue) {
            if (module.custom.getValue()) {
                submitCustom(queue, matrixStack, CORE_PART, module);
            }
            submitVanilla(queue, matrixStack, CORE_PART, layer, light, overlay, module.core.getValue().getRGB(), glint);
        }
        matrixStack.pop();

        Vec3d beamOffset = state.beamOffset;
        if (beamOffset != null) {
            matrixStack.push();
            float bx = (float) -beamOffset.x;
            float by = (float) (-beamOffset.y + h);
            float bz = (float) -beamOffset.z;
            matrixStack.translate(beamOffset);
            EnderDragonEntityRenderer.renderCrystalBeam(bx, by, bz, state.age, matrixStack, queue, light);
            matrixStack.pop();
        }
        super.render(state, matrixStack, queue, cameraRenderState);
    }

    @Unique
    private static void submitVanilla(OrderedRenderCommandQueue queue, MatrixStack matrices, ModelPart part, RenderLayer layer, int light, int overlay, int color, boolean glint) {
        queue.submitModelPart(part, matrices, layer, light, overlay, null, false, glint, color, null, 0);
    }

    @Unique
    private static void submitCustom(OrderedRenderCommandQueue queue, MatrixStack matrices, ModelPart part, Chams module) {
        boolean textured = module.chamsTexture.getValue();
        boolean depth = module.depth.getValue();

        if (module.fill.booleanValue) {
            int color = module.fill.getValue().getRGB();
            RenderLayer fillLayer = textured
                    ? (depth ? crystalLayer() : crystalNoDepthLayer())
                    : (depth ? CRYSTAL_COLOR_DEPTH_LAYER : CRYSTAL_COLOR_NO_DEPTH_LAYER);
            queue.submitCustom(matrices, fillLayer, (entry, vertexConsumer) ->
                    renderFill(entry, vertexConsumer, part, color, textured));
        }

        if (module.line.booleanValue) {
            int color = module.line.getValue().getRGB();
            RenderLayer lineLayer = depth ? RenderLayers.lines() : CRYSTAL_LINES_NO_DEPTH_LAYER;
            queue.submitCustom(matrices, lineLayer, (entry, vertexConsumer) ->
                    renderLines(entry, vertexConsumer, part, color));
        }
    }

    @Unique
    private static void renderFill(MatrixStack.Entry entry, VertexConsumer vertexConsumer, ModelPart part, int color, boolean textured) {
        for (ModelPart.Cuboid cuboid : ((IModelPart) (Object) part).getCuboids()) {
            for (ModelPart.Quad quad : ((IModelPartCuboid) (Object) cuboid).getSides()) {
                Vector3f normal = entry.transformNormal(quad.direction(), new Vector3f());
                for (ModelPart.Vertex vertex : quad.vertices()) {
                    if (textured) {
                        vertexConsumer.vertex(entry, vertex.worldX(), vertex.worldY(), vertex.worldZ())
                                .color(color)
                                .texture(vertex.u(), vertex.v())
                                .overlay(OverlayTexture.DEFAULT_UV)
                                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                                .normal(normal.x(), normal.y(), normal.z());
                    } else {
                        vertexConsumer.vertex(entry, vertex.worldX(), vertex.worldY(), vertex.worldZ()).color(color);
                    }
                }
            }
        }
    }

    @Unique
    private static void renderLines(MatrixStack.Entry entry, VertexConsumer vertexConsumer, ModelPart part, int color) {
        for (ModelPart.Cuboid cuboid : ((IModelPart) (Object) part).getCuboids()) {
            for (ModelPart.Quad quad : ((IModelPartCuboid) (Object) cuboid).getSides()) {
                Vector3f normal = entry.transformNormal(quad.direction(), new Vector3f());
                ModelPart.Vertex[] vertices = quad.vertices();
                for (int i = 0; i < vertices.length; i++) {
                    ModelPart.Vertex from = vertices[i];
                    ModelPart.Vertex to = vertices[(i + 1) % vertices.length];
                    vertexConsumer.vertex(entry, from.worldX(), from.worldY(), from.worldZ())
                            .color(color)
                            .normal(normal.x(), normal.y(), normal.z())
                            .lineWidth(1.0F);
                    vertexConsumer.vertex(entry, to.worldX(), to.worldY(), to.worldZ())
                            .color(color)
                            .normal(normal.x(), normal.y(), normal.z())
                            .lineWidth(1.0F);
                }
            }
        }
    }
}
