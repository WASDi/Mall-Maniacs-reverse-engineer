package tpgviewer;

import senfile.Util;
import tpgviewer.tpg.TpgImage;
import tpgviewer.tpg.TpgImageFactory;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class ImageBrowserPanel extends JPanel implements KeyListener {

    public static final int SCALE_FACTOR = 2;

    private int imgIndex;
    private final Consumer<String> titleSetter;

    TpgImage tpgImage;


    public ImageBrowserPanel(int imgIndex, Consumer<String> titleSetter) {

        this.imgIndex = imgIndex;
        this.titleSetter = titleSetter;
    }

    public void loadBytes() {
        String filePath = String.format(Util.ROOT_DIR + "scene_ica/MERGED%02d.TPG", imgIndex);
//        filePath = String.format(Util.ROOT_DIR + "scene_aqua/MERGED%02d.TPG", imgIndex);
//        filePath = String.format(Util.ROOT_DIR + "scene_future/FUT%02d.TPG", imgIndex);
//        String filePath = Util.ROOT_DIR + "menu/SUSANNE00.TPG";
        try {
            tpgImage = TpgImageFactory.fromFile(filePath);
            titleSetter.accept(filePath);
        } catch (RuntimeException ex) {
            titleSetter.accept(ex.getMessage());
        }

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage img = tpgImage.renderLazy();

        int scaledX = img.getWidth() * SCALE_FACTOR;
        int scaledY = img.getHeight() * SCALE_FACTOR;
        Image scaledInstance = img.getScaledInstance(scaledX, scaledY, Image.SCALE_FAST);
        g.drawImage(scaledInstance,
                    getWidth() / 2 - scaledX / 2,
                    getHeight() / 2 - scaledX / 2,
                    scaledX,
                    scaledY,
                    null);
    }


    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            imgIndex--;
            reload();
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            imgIndex++;
            reload();
        }
    }

    private void reload() {
        loadBytes();
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
