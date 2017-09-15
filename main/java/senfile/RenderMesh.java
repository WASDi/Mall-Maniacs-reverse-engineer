package senfile;

import com.wasd.lib3d.World;
import com.wasd.lib3d.gui.Window3D;
import com.wasd.lib3d.objects.Box;
import senfile.parts.mesh.SenMesh;
import senfile.parts.mesh.Vertex;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.ArrayList;

public class RenderMesh {

    public static void render(SenMesh mesh, boolean incrementally) {
        World world = new World();
        if (!incrementally) {
            for (Vertex vertex : mesh.getVertices()) {
                addSmallBox(world, vertex, 0, Color.GREEN);
            }
        }

        ArrayList animations = new ArrayList();
        Window3D wasdWindow = new Window3D(world, animations);
        wasdWindow.makeVisible();

        if (incrementally) {
            new Thread(() -> {
                Vertex[] vertices = mesh.getVertices();
                // 0 to 55
                // 56 to 75
                // 76 to
                int start = 0;
                int end = vertices.length - 1;
                for (int i = start; i <= end; i++) {
                    Vertex vertex = vertices[i];

                    final float offsetY;
                    final Color color;
                    if (i >= 76) {
                        offsetY = 0.3f;
                        color = Color.YELLOW;
                    } else if (i >= 56) {
                        offsetY = 0.1f;
                        color = Color.RED;
                    } else {
                        offsetY = 0f;
                        color = Color.GREEN;
                    }
                    try {
                        Thread.sleep(30);
                        SwingUtilities.invokeLater(() -> {
                            addSmallBox(world, vertex, offsetY, color);
                            wasdWindow.repaint();
                        });
//                        System.out.println(i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }).start();
        }
    }

    private static void addSmallBox(World world, Vertex vertex, float offsetY, Color color) {
        float scale = .0005f;
        float x = vertex.x * scale;
        float y = vertex.y * scale - offsetY;
        float z = vertex.z * scale;
        if (x < 0) {
//            return;
        }
        world.add((new Box(x, y, z, 0.00F)).withDotColor(color));
    }
}
