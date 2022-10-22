package tpgviewer;

import senfile.Util;
import tpgviewer.util.BytesFromFileGetter;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class ImageInspectorPanel extends JPanel implements KeyListener, MouseListener {

    private static final int OFFSET = 0;
    private static final int PALETTE_START = 256 * 256;

    public static final int SCALE_FACTOR = 2;

    private int imgIndex;
    private final Consumer<String> titleSetter;

    private byte[] bytes;

    public ImageInspectorPanel(int imgIndex, Consumer<String> titleSetter) {
        this.imgIndex = imgIndex;
        this.titleSetter = titleSetter;
    }

    public void loadBytes() {
        String filePath = String.format(Util.ROOT_DIR + "menu/LEVEL%02d.TPG", imgIndex);
//        String filePath = Util.ROOT_DIR + "menu/KALLE00.TPG";
        try {
            bytes = BytesFromFileGetter.getBytes(filePath);
            titleSetter.accept(filePath);
        } catch (RuntimeException ex) {
            titleSetter.accept(ex.getMessage());
        }

    }

    private void computeImage(BufferedImage img) {
        int i = OFFSET;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = getRgb(i);
                img.setRGB(x, y, rgb);
                i += 1; // TODO ???
            }
        }
    }

    private int getRgb(int i) {
        try {
            // remove sign
            int paletteOffset = bytes[i] & 0xFF;
            int r = bytes[PALETTE_START + 4 * paletteOffset];
            int g = bytes[PALETTE_START + 4 * paletteOffset + 1];
            int b = bytes[PALETTE_START + 4 * paletteOffset + 2];


            return (r << 16) + (g << 8) + b;
        } catch (IndexOutOfBoundsException ignored) {
            return (255 << 16) + 255;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage img = new BufferedImage(getWidth() / SCALE_FACTOR, getHeight() / SCALE_FACTOR, BufferedImage.TYPE_INT_RGB);
        computeImage(img);

        int scaledX = img.getWidth() * SCALE_FACTOR;
        int scaledY = img.getHeight() * SCALE_FACTOR;
        Image scaledInstance = img.getScaledInstance(scaledX, scaledY, Image.SCALE_FAST);
        g.drawImage(scaledInstance, 0, 0, scaledX, scaledY, null);
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

    @Override
    public void mouseClicked(MouseEvent e) {
        int x = (e.getX() - 1) / SCALE_FACTOR;
        int y = (e.getY() - 2) / SCALE_FACTOR;
        int widthRounded = getWidth() - getWidth() % SCALE_FACTOR;
        int totalByteOffset = OFFSET + y * widthRounded / SCALE_FACTOR + x;
        int byteUnsigned = bytes[totalByteOffset] & 0xFF;
        System.out.printf("(%3d, %3d) = %8s (%3d)\n", x, y, toBits(byteUnsigned), byteUnsigned);

        bytes[totalByteOffset] = -1;
        repaint();
    }

    private String toBits(int byteUnsigned) {
        return String.format("%8s", Integer.toBinaryString(byteUnsigned)).replace(' ', '0');
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
