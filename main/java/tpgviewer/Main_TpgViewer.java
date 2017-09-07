package tpgviewer;

import tpgviewer.tpg.TpgImage;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class Main_TpgViewer {

    public static void main(String[] args) throws Exception {
        browser();
//        inspector();
    }

    private static void browser() {
        JFrame frame = new JFrame();

        ImageBrowserPanel panel = new ImageBrowserPanel(0, frame::setTitle);
        panel.loadBytes();
        frame.add(panel);

        frame.addKeyListener(panel);
//        panel.addMouseListener(panel);

        frame.setSize(TpgImage.SIZE * ImageBrowserPanel.SCALE_FACTOR + 2 + 20,
                      TpgImage.SIZE * ImageBrowserPanel.SCALE_FACTOR + 19 + 20 );
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private static void inspector() {
        JFrame frame = new JFrame();

        ImageInspectorPanel panel = new ImageInspectorPanel(0, frame::setTitle);
        panel.loadBytes();
        frame.add(panel);

        frame.addKeyListener(panel);
        panel.addMouseListener(panel);

        frame.setSize(TpgImage.SIZE * ImageInspectorPanel.SCALE_FACTOR + 2,
                      TpgImage.SIZE * ImageInspectorPanel.SCALE_FACTOR + 19 + 30 );
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

}
