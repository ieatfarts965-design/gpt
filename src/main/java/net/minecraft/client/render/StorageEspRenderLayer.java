package net.minecraft.client.render;

import java.util.OptionalDouble;

/** Creates an ESP line layer that is rendered regardless of depth. */
public final class StorageEspRenderLayer {
    public static final RenderLayer LINES = RenderLayer.of(
            "storageesp:through_wall_lines",
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

