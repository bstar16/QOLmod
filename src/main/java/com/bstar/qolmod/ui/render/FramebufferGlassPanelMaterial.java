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
    private static AdvancedBlurState advancedBlurState = AdvancedBlurState.UNINITIALIZED;

    private final Pool framebufferPool = new Pool(3);
    private final LiquidGlassSurfaceRenderer surfaceRenderer = new LiquidGlassSurfaceRenderer();
    private Framebuffer captureFramebuffer;
    private TextureSetup captureTextureSetup;
    private FramebufferRegion captureRegion;
    private int captureReadFramebuffer;
    private int captureDrawFramebuffer;
    private boolean prepared;
    private boolean permanentlyDisabled;
    private boolean failureLogged;
    private boolean initializationLogged;

    /** Validates and warms the managed post-effect outside the per-frame GUI render path. */
    public static synchronized void initializeAdvancedPath(MinecraftClient client) {
        if (advancedBlurState != AdvancedBlurState.UNINITIALIZED) {
            return;
        }
        if (!BLUR_ENABLED) {
            advancedBlurState = AdvancedBlurState.DISABLED;
            QOLmodClient.LOGGER.info("QOLmod liquid-glass blur is disabled by system property.");
            return;
        }
        try {
            PostEffectProcessor blur = client.getShaderLoader().loadPostEffect(
                    BLUR_EFFECT,
                    net.minecraft.client.render.DefaultFramebufferSet.MAIN_ONLY
            );
            if (blur == null) {
                advancedBlurState = AdvancedBlurState.UNAVAILABLE;
                QOLmodClient.LOGGER.warn(
                        "QOLmod liquid-glass blur resource is unavailable; translucent fallback is active."
                );
            } else {
                advancedBlurState = AdvancedBlurState.AVAILABLE;
                QOLmodClient.LOGGER.info(
                        "QOLmod liquid-glass shader initialized; panel-local blur is available."
                );
            }
        } catch (RuntimeException exception) {
            advancedBlurState = AdvancedBlurState.UNAVAILABLE;
            QOLmodClient.LOGGER.error(
                    "QOLmod liquid-glass shader failed to initialize; translucent fallback is active.",
                    exception
            );
        }
    }

    /** Revalidates managed shader resources after Minecraft completes the shader reload phase. */
    public static synchronized void reloadAdvancedPath(MinecraftClient client) {
        advancedBlurState = AdvancedBlurState.UNINITIALIZED;
        initializeAdvancedPath(client);
    }

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
            if (BLUR_ENABLED && glass.blurStrength() > 0.0) {
                initializeAdvancedPath(client);
                if (advancedBlurState != AdvancedBlurState.AVAILABLE) {
                    return;
                }
            }
            captureRegion = FramebufferRegion.capture(
                    x, y, width, height, glass.blurPaddingPixels(),
                    main.textureWidth, main.textureHeight,
                    context.getScaledWindowWidth(), context.getScaledWindowHeight()
            );
            ensureCaptureFramebuffer(captureRegion.width(), captureRegion.height());
            capture(main);

            boolean blurApplied = false;
            if (BLUR_ENABLED && glass.blurStrength() > 0.0) {
                PostEffectProcessor blur = client.getShaderLoader().loadPostEffect(
                        BLUR_EFFECT,
                        net.minecraft.client.render.DefaultFramebufferSet.MAIN_ONLY
                );
                if (blur != null) {
                    blur.render(captureFramebuffer, framebufferPool);
                    blurApplied = true;
                } else {
                    advancedBlurState = AdvancedBlurState.UNAVAILABLE;
                }
            }
            framebufferPool.decrementLifespan();
            prepared = !BLUR_ENABLED || glass.blurStrength() <= 0.0 || blurApplied;
            if (prepared && !initializationLogged) {
                initializationLogged = true;
                QOLmodClient.LOGGER.info(BLUR_ENABLED && blurApplied
                        ? "QOLmod liquid glass initialized with panel-local framebuffer blur."
                        : "QOLmod liquid glass initialized with advanced blur disabled.");
            } else if (!prepared && !failureLogged) {
                failureLogged = true;
                QOLmodClient.LOGGER.warn(
                        "QOLmod liquid-glass blur resource was unavailable; using the translucent fallback."
                );
            }
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
        captureTextureSetup = TextureSetup.of(
                captureFramebuffer.getColorAttachmentView(),
                RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
        );
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
        int tint = TINT_ENABLED ? glass.mainTint() : 0;
        GlassSurface surface = new GlassSurface(
                x,
                y,
                width,
                height,
                1.0,
                tint,
                0,
                glass.blurStrength()
        );
        surfaceRenderer.draw(context, surface, prepared ? this::drawBackdrop : null);

        if (DEBUG_BOUNDS && captureRegion != null) {
            drawDebugBounds(context, x, y, width, height);
        }
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
        // The internal settings view already sits over the main panel's correctly aligned glass.
        // Re-compositing the capture texture here creates a second deferred textured surface with
        // independent UV/scissor state. Keep the settings material panel-local by layering only its
        // tint over the existing main glass sample. The screen's existing header/sidebar/footer
        // dividers already define this internal surface, so it does not need a second outer edge.
        drawOverlay(context, x, y, width, height, clipLeft, clipRight, glass.drawerTint());
    }

    private void drawBackdrop(DrawContext context, GlassSurface surface) {
        if (captureFramebuffer == null || captureRegion == null) {
            return;
        }
        int backdropAlpha = LiquidGlassSurfaceRenderer.multiplyAlpha(
                0xFFFFFFFF,
                surface.opacity() * surface.blurStrength()
        );
        drawCapturedQuad(
                context,
                surface.x(),
                surface.y(),
                surface.width(),
                surface.height(),
                0.0,
                0.0,
                backdropAlpha
        );
    }

    private void drawOverlay(
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

        context.enableScissor(safeClipLeft, y, safeClipRight, y + height);
        if (TINT_ENABLED) {
            context.fill(x, y, x + width, y + height, tint);
        }
        context.disableScissor();
    }

    private void drawCapturedQuad(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            double framebufferOffsetX,
            double framebufferOffsetY,
            int color
    ) {
        if (width <= 0 || height <= 0 || captureFramebuffer == null || captureRegion == null) {
            return;
        }
        FramebufferRegion.SurfaceUv uv = captureRegion.uvFor(
                x,
                y,
                width,
                height,
                framebufferOffsetX,
                framebufferOffsetY
        );
        ScreenRect scissor = new ScreenRect(x, y, width, height);
        context.state.addSimpleElement(new TexturedQuadGuiElementRenderState(
                RenderPipelines.GUI_TEXTURED,
                captureTextureSetup,
                new Matrix3x2f(context.getMatrices()),
                x, y, x + width, y + height,
                uv.uLeft(), uv.uRight(), uv.vTop(), uv.vBottom(),
                color,
                scissor
        ));
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
            captureTextureSetup = null;
        }
        if (captureReadFramebuffer != 0) {
            GlStateManager._glDeleteFramebuffers(captureReadFramebuffer);
            GlStateManager._glDeleteFramebuffers(captureDrawFramebuffer);
            captureReadFramebuffer = 0;
            captureDrawFramebuffer = 0;
        }
    }

    private enum AdvancedBlurState {
        UNINITIALIZED,
        AVAILABLE,
        UNAVAILABLE,
        DISABLED
    }
}
