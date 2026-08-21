package com.bstar.qolmod.ui.render;

import com.bstar.qolmod.QOLmodClient;
import com.bstar.qolmod.ui.theme.GlassStyle;
import com.bstar.qolmod.ui.theme.ThemeManager;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.Pool;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

/**
 * Captures and blurs only the padded framebuffer rectangle behind the visible QOLmod shell. The
 * blurred result is composited through per-surface scissors, so pixels outside the glass are never
 * written by this material.
 */
public final class FramebufferGlassPanelMaterial implements PanelMaterial {
    private static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    private static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_COLOR_BUFFER_BIT = 0x4000;
    private static final int GL_NEAREST = 0x2600;
    private static final Identifier BLUR_EFFECT = Identifier.of(QOLmodClient.MOD_ID, "glass_blur");
    private static final boolean BLUR_ENABLED = !Boolean.getBoolean("qolmod.glass.disableBlur");
    private static final boolean TINT_ENABLED = !Boolean.getBoolean("qolmod.glass.disableTint");
    private static final boolean DEBUG_BOUNDS = Boolean.getBoolean("qolmod.glass.debugBounds");

    private final Pool framebufferPool = new Pool(3);
    private Framebuffer captureFramebuffer;
    private FramebufferRegion captureRegion;
    private int captureReadFramebuffer;
    private int captureDrawFramebuffer;
    private boolean prepared;
    private boolean permanentlyDisabled;
    private boolean failureLogged;

    @Override
    public void prepareFrame(DrawContext context, int x, int y, int width, int height) {
        prepared = false;
        if (permanentlyDisabled || width <= 0 || height <= 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer main = client.getFramebuffer();
        if (main.getColorAttachment() == null) {
            return;
        }

        try {
            GlassStyle glass = ThemeManager.active().glass();
            captureRegion = FramebufferRegion.capture(
                    x, y, width, height, glass.blurPaddingPixels(),
                    main.textureWidth, main.textureHeight,
                    context.getScaledWindowWidth(), context.getScaledWindowHeight()
            );
            ensureCaptureFramebuffer(captureRegion.width(), captureRegion.height());
            capture(main);

            if (BLUR_ENABLED) {
                PostEffectProcessor blur = client.getShaderLoader()
                        .loadPostEffect(BLUR_EFFECT, net.minecraft.client.render.DefaultFramebufferSet.MAIN_ONLY);
                if (blur != null) {
                    blur.render(captureFramebuffer, framebufferPool);
                }
            }
            framebufferPool.decrementLifespan();
            prepared = true;
        } catch (RuntimeException exception) {
            permanentlyDisabled = true;
            if (!failureLogged) {
                failureLogged = true;
                QOLmodClient.LOGGER.error("Disabling QOLmod panel-local glass after a rendering failure", exception);
            }
        }
    }

    private void ensureCaptureFramebuffer(int width, int height) {
        if (captureFramebuffer != null
                && captureFramebuffer.textureWidth == width
                && captureFramebuffer.textureHeight == height) {
            return;
        }
        if (captureFramebuffer != null) {
            captureFramebuffer.delete();
        }
        captureFramebuffer = new SimpleFramebuffer("QOLmod panel glass", width, height, false);
        if (captureReadFramebuffer == 0) {
            captureReadFramebuffer = GlStateManager.glGenFramebuffers();
            captureDrawFramebuffer = GlStateManager.glGenFramebuffers();
        }
    }

    private void capture(Framebuffer main) {
        if (!(main.getColorAttachment() instanceof GlTexture source)
                || !(captureFramebuffer.getColorAttachment() instanceof GlTexture target)) {
            throw new IllegalStateException("QOLmod glass requires Minecraft's OpenGL texture backend");
        }

        int previousRead = GlStateManager.getFrameBuffer(GL_READ_FRAMEBUFFER);
        int previousDraw = GlStateManager.getFrameBuffer(GL_DRAW_FRAMEBUFFER);
        try {
            GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, captureReadFramebuffer);
            GlStateManager._glFramebufferTexture2D(
                    GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, source.getGlId(), 0
            );
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, captureDrawFramebuffer);
            GlStateManager._glFramebufferTexture2D(
                    GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target.getGlId(), 0
            );
            GlStateManager._glBlitFrameBuffer(
                    captureRegion.left(), captureRegion.sourceBottom(),
                    captureRegion.right(), captureRegion.sourceTop(),
                    0, 0, captureRegion.width(), captureRegion.height(),
                    GL_COLOR_BUFFER_BIT, GL_NEAREST
            );
        } finally {
            GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, previousRead);
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previousDraw);
        }
    }

    @Override
    public void drawMainPanel(DrawContext context, int x, int y, int width, int height) {
        GlassStyle glass = ThemeManager.active().glass();
        drawSurface(context, x, y, width, height, x, x + width, glass.mainTint());
    }

    @Override
    public void drawDrawer(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            boolean attached,
            int clipLeft,
            int clipRight
    ) {
        GlassStyle glass = ThemeManager.active().glass();
        drawSurface(context, x, y, width, height, clipLeft, clipRight, glass.drawerTint());
    }

    private void drawSurface(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int clipLeft,
            int clipRight,
            int tint
    ) {
        int safeClipLeft = Math.max(x, clipLeft);
        int safeClipRight = Math.min(x + width, clipRight);
        if (safeClipRight <= safeClipLeft) {
            return;
        }

        if (prepared && captureFramebuffer != null && captureRegion != null) {
            FramebufferRegion.SurfaceUv uv = captureRegion.uvFor(x, y, width, height);
            TextureSetup texture = TextureSetup.of(
                    captureFramebuffer.getColorAttachmentView(),
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
            );
            ScreenRect scissor = new ScreenRect(safeClipLeft, y, safeClipRight - safeClipLeft, height);
            context.state.addSimpleElement(new TexturedQuadGuiElementRenderState(
                    RenderPipelines.GUI_TEXTURED,
                    texture,
                    new Matrix3x2f(context.getMatrices()),
                    x, y, x + width, y + height,
                    uv.uLeft(), uv.uRight(), uv.vTop(), uv.vBottom(),
                    0xFFFFFFFF,
                    scissor
            ));
        }

        GlassStyle glass = ThemeManager.active().glass();
        context.enableScissor(safeClipLeft, y, safeClipRight, y + height);
        if (TINT_ENABLED) {
            context.fill(x, y, x + width, y + height, tint);
        }
        drawMaterialEdges(context, x, y, width, height, glass);
        context.disableScissor();

        if (DEBUG_BOUNDS && captureRegion != null) {
            drawDebugBounds(context, x, y, width, height);
        }
    }

    private void drawMaterialEdges(DrawContext context, int x, int y, int width, int height, GlassStyle glass) {
        context.fill(x, y, x + width, y + 1, glass.edge());
        context.fill(x, y + height - 1, x + width, y + height, glass.innerShadow());
        context.fill(x, y, x + 1, y + height, glass.edge());
        context.fill(x + width - 1, y, x + width, y + height, glass.innerShadow());
        context.fill(x + 2, y + 2, x + width - 2, y + 3, glass.innerHighlight());
        context.fill(x + 2, y + 3, x + 3, y + height - 2, glass.innerHighlight());
    }

    private void drawDebugBounds(DrawContext context, int x, int y, int width, int height) {
        int left = captureRegion.debugGuiLeft();
        int top = captureRegion.debugGuiTop();
        int right = captureRegion.debugGuiRight();
        int bottom = captureRegion.debugGuiBottom();
        int captureColor = 0xFFFF3B30;
        int surfaceColor = 0xFF30D158;
        context.drawHorizontalLine(left, right - 1, top, captureColor);
        context.drawHorizontalLine(left, right - 1, bottom - 1, captureColor);
        context.drawVerticalLine(left, top, bottom - 1, captureColor);
        context.drawVerticalLine(right - 1, top, bottom - 1, captureColor);
        context.drawHorizontalLine(x, x + width - 1, y, surfaceColor);
        context.drawHorizontalLine(x, x + width - 1, y + height - 1, surfaceColor);
    }

    @Override
    public void close() {
        prepared = false;
        framebufferPool.close();
        if (captureFramebuffer != null) {
            captureFramebuffer.delete();
            captureFramebuffer = null;
        }
        if (captureReadFramebuffer != 0) {
            GlStateManager._glDeleteFramebuffers(captureReadFramebuffer);
            GlStateManager._glDeleteFramebuffers(captureDrawFramebuffer);
            captureReadFramebuffer = 0;
            captureDrawFramebuffer = 0;
        }
    }
}
