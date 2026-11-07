package dev.autobuilder.client.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Provides backward and forward compatibility for line rendering across Minecraft
 * 1.21.1 (RenderLayer.getLines()), 1.21.11 (RenderLayers.lines()), and 26.1.2.
 */
public class RenderLayerHelper {

    private static RenderLayer cachedLinesLayer = null;
    private static boolean initialized = false;

    public static VertexConsumer getLinesBuffer(VertexConsumerProvider consumers) {
        if (!initialized) {
            initialized = true;
            cachedLinesLayer = resolveLinesLayer();
        }

        if (cachedLinesLayer != null) {
            return consumers.getBuffer(cachedLinesLayer);
        }

        // Default fallback for compile-time target
        return consumers.getBuffer(RenderLayers.lines());
    }

    private static RenderLayer resolveLinesLayer() {
        // 1. Try modern 1.21.11 / 26.1.2 RenderLayers.lines() or RenderLayers.LINES
        try {
            Class<?> layersClass = Class.forName("net.minecraft.client.render.RenderLayers");
            try {
                Method method = layersClass.getMethod("lines");
                Object result = method.invoke(null);
                if (result instanceof RenderLayer rl) return rl;
            } catch (NoSuchMethodException ignored) {
                Field field = layersClass.getField("LINES");
                Object result = field.get(null);
                if (result instanceof RenderLayer rl) return rl;
            }
        } catch (Throwable ignored) {}

        // 2. Try 1.21.1 RenderLayer.getLines()
        try {
            Class<?> layerClass = Class.forName("net.minecraft.client.render.RenderLayer");
            Method method = layerClass.getMethod("getLines");
            Object result = method.invoke(null);
            if (result instanceof RenderLayer rl) return rl;
        } catch (Throwable ignored) {}

        return null;
    }
}
