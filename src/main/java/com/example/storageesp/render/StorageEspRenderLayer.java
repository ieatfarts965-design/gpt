package com.example.storageesp.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import java.util.OptionalDouble;

/** Through-wall line layer used for tracers and storage outlines. */
public final class StorageEspRenderLayer {
    public static final RenderLayer LINES = RenderLayer.of(
            "silo_client:through_wall_lines",
            VertexFormats.LINES,
            VertexFormat.DrawMode.LINES,
            256,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.LINES_PROGRAM)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(2.0)))
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .writeMaskState(RenderPhase.ALL_MASK)
                    .build(false)
    );

    private StorageEspRenderLayer() {
    }
}

