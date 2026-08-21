package com.bstar.qolmod.ui.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FramebufferRegionTest {
    @Test
    void convertsIntegerGuiScaleWithoutOffsetOrVerticalFlip() {
        FramebufferRegion region = FramebufferRegion.capture(
                100, 50, 400, 300, 12,
                1920, 1080, 960, 540
        );

        assertEquals(188, region.left());
        assertEquals(88, region.top());
        assertEquals(1012, region.right());
        assertEquals(712, region.bottom());
        assertEquals(368, region.sourceBottom());
        assertEquals(992, region.sourceTop());

        FramebufferRegion.SurfaceUv uv = region.uvFor(100, 50, 400, 300);
        assertEquals(12.0 / 824.0, uv.uLeft(), 0.00001);
        assertEquals(812.0 / 824.0, uv.uRight(), 0.00001);
        assertEquals(612.0 / 624.0, uv.vTop(), 0.00001);
        assertEquals(12.0 / 624.0, uv.vBottom(), 0.00001);
        assertTrue(uv.vTop() > uv.vBottom(), "GUI top must sample the top of the OpenGL texture");
    }

    @Test
    void preservesFractionalFramebufferRatioAtNonIntegerGuiScale() {
        int guiWidth = 853;
        int guiHeight = 480;
        int framebufferWidth = 1920;
        int framebufferHeight = 1080;
        int x = 137;
        int y = 73;
        int width = 411;
        int height = 287;
        FramebufferRegion region = FramebufferRegion.capture(
                x, y, width, height, 12,
                framebufferWidth, framebufferHeight, guiWidth, guiHeight
        );
        FramebufferRegion.SurfaceUv uv = region.uvFor(x, y, width, height);

        double reconstructedLeft = region.left() + uv.uLeft() * region.width();
        double reconstructedRight = region.left() + uv.uRight() * region.width();
        double reconstructedTop = region.bottom() - uv.vTop() * region.height();
        double reconstructedBottom = region.bottom() - uv.vBottom() * region.height();

        assertEquals(x * framebufferWidth / (double) guiWidth, reconstructedLeft, 0.0001);
        assertEquals((x + width) * framebufferWidth / (double) guiWidth, reconstructedRight, 0.0001);
        assertEquals(y * framebufferHeight / (double) guiHeight, reconstructedTop, 0.0001);
        assertEquals((y + height) * framebufferHeight / (double) guiHeight, reconstructedBottom, 0.0001);
    }

    @Test
    void clampsPaddingAtFramebufferEdgesWithoutChangingSurfaceMapping() {
        FramebufferRegion region = FramebufferRegion.capture(
                0, 0, 300, 200, 12,
                1280, 720, 640, 360
        );

        assertEquals(0, region.left());
        assertEquals(0, region.top());
        FramebufferRegion.SurfaceUv uv = region.uvFor(0, 0, 300, 200);
        assertEquals(0.0, uv.uLeft(), 0.00001);
        assertEquals(1.0, uv.vTop(), 0.00001);
    }
}
