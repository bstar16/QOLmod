package com.bstar.qolmod.ui.render;

/**
 * Integer framebuffer capture bounds derived from GUI-scaled coordinates. Framebuffer Y values in
 * this type use a top-left origin; {@link #sourceBottom()} performs the OpenGL bottom-left conversion.
 */
record FramebufferRegion(
        int left,
        int top,
        int right,
        int bottom,
        int framebufferWidth,
        int framebufferHeight,
        int guiWidth,
        int guiHeight
) {
    static FramebufferRegion capture(
            int guiX,
            int guiY,
            int guiRegionWidth,
            int guiRegionHeight,
            int paddingPixels,
            int framebufferWidth,
            int framebufferHeight,
            int guiWidth,
            int guiHeight
    ) {
        if (framebufferWidth <= 0 || framebufferHeight <= 0 || guiWidth <= 0 || guiHeight <= 0) {
            throw new IllegalArgumentException("Framebuffer and GUI dimensions must be positive");
        }
        double scaleX = framebufferWidth / (double) guiWidth;
        double scaleY = framebufferHeight / (double) guiHeight;
        int left = Math.max(0, (int) Math.floor(guiX * scaleX) - paddingPixels);
        int top = Math.max(0, (int) Math.floor(guiY * scaleY) - paddingPixels);
        int right = Math.min(framebufferWidth,
                (int) Math.ceil((guiX + guiRegionWidth) * scaleX) + paddingPixels);
        int bottom = Math.min(framebufferHeight,
                (int) Math.ceil((guiY + guiRegionHeight) * scaleY) + paddingPixels);
        return new FramebufferRegion(
                left, top, right, bottom,
                framebufferWidth, framebufferHeight, guiWidth, guiHeight
        );
    }

    int width() {
        return right - left;
    }

    int height() {
        return bottom - top;
    }

    int sourceBottom() {
        return framebufferHeight - bottom;
    }

    int sourceTop() {
        return framebufferHeight - top;
    }

    SurfaceUv uvFor(int guiX, int guiY, int guiRegionWidth, int guiRegionHeight) {
        return uvFor(guiX, guiY, guiRegionWidth, guiRegionHeight, 0.0, 0.0);
    }

    SurfaceUv uvFor(
            int guiX,
            int guiY,
            int guiRegionWidth,
            int guiRegionHeight,
            double framebufferOffsetX,
            double framebufferOffsetY
    ) {
        double scaleX = framebufferWidth / (double) guiWidth;
        double scaleY = framebufferHeight / (double) guiHeight;
        double surfaceLeft = guiX * scaleX + framebufferOffsetX;
        double surfaceRight = (guiX + guiRegionWidth) * scaleX + framebufferOffsetX;
        double surfaceTop = guiY * scaleY + framebufferOffsetY;
        double surfaceBottom = (guiY + guiRegionHeight) * scaleY + framebufferOffsetY;

        float uLeft = (float) ((surfaceLeft - left) / width());
        float uRight = (float) ((surfaceRight - left) / width());
        // The capture texture and OpenGL use a bottom-left origin, while GUI Y grows downward.
        float vTop = (float) ((bottom - surfaceTop) / height());
        float vBottom = (float) ((bottom - surfaceBottom) / height());
        return new SurfaceUv(uLeft, uRight, vTop, vBottom);
    }

    int debugGuiLeft() {
        return (int) Math.floor(left * guiWidth / (double) framebufferWidth);
    }

    int debugGuiTop() {
        return (int) Math.floor(top * guiHeight / (double) framebufferHeight);
    }

    int debugGuiRight() {
        return (int) Math.ceil(right * guiWidth / (double) framebufferWidth);
    }

    int debugGuiBottom() {
        return (int) Math.ceil(bottom * guiHeight / (double) framebufferHeight);
    }

    record SurfaceUv(float uLeft, float uRight, float vTop, float vBottom) {
    }
}
