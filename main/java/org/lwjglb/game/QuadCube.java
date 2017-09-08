package org.lwjglb.game;

import org.lwjglb.engine.graph.IMesh;
import org.lwjglb.engine.graph.MeshQuad;
import org.lwjglb.engine.graph.Texture;

import java.io.IOException;

public class QuadCube {

    public static final IMesh MY_MESH = createQuadMesh();

    private QuadCube() {
    }

    private static MeshQuad createQuadMesh() {
        float v = 0.5f;
        float[] positions = new float[]{
                // top
                -v, 0, -v,
                -v, 0, v,
                v, 0, v,
                v, 0, -v,
        };
        float[] textCoords = new float[]{
                // top
                0.0f, 0.5f,
                0.0f, 1.0f,
                0.5f, 1.0f,
                0.5f, 0.5f,
        };
        int[] indices = new int[]{
                // top
                0, 1, 2, 3
        };

        Texture texture;
        try {
            texture = new Texture("/textures/grassblock.png");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
        return new MeshQuad(positions, textCoords, indices, texture);
    }

}
