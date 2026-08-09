package senviewer;

import senfile.GameMap;
import senfile.SenFile;
import senfile.factories.SenFileFactory;
import senfile.parts.mesh.SenMesh;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public class SenViewerMain {

    private final JFrame frame;
    private final JComboBox<GameMap> mapCombo;
    private final JComboBox<SenFileChoice> fileCombo;
    private final JTree tree;
    private final PropertyTableModel tableModel;
    private final MeshViewPanel meshViewPanel;
    private boolean ignoreFileComboEvents;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SenViewerMain::new);
    }

    public SenViewerMain() {
        frame = new JFrame("SEN Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mapCombo = new JComboBox<>(GameMap.values());
        mapCombo.addActionListener(e -> populateFileCombo((GameMap) mapCombo.getSelectedItem()));

        fileCombo = new JComboBox<>();
        fileCombo.addActionListener(e -> loadSelectedFile());

        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.add(new JLabel("GameMap:"));
        selectorPanel.add(mapCombo);
        selectorPanel.add(new JLabel("SEN-file:"));
        selectorPanel.add(fileCombo);

        tree = new JTree(new DefaultMutableTreeNode("No file loaded"));
        tree.setRootVisible(true);

        tableModel = new PropertyTableModel();
        JTable table = new JTable(tableModel);

        meshViewPanel = new MeshViewPanel();

        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null) {
                tableModel.setSelected(null);
                meshViewPanel.showMesh(null);
                return;
            }
            Object userObject = node.getUserObject();
            if (userObject instanceof SenFileTreeBuilder.NodeValue nodeValue) {
                tableModel.setSelected(nodeValue.value);
                if (nodeValue.value instanceof SenMesh mesh) {
                    meshViewPanel.showMesh(mesh);
                } else {
                    meshViewPanel.showMesh(null);
                }
            } else {
                tableModel.setSelected(null);
                meshViewPanel.showMesh(null);
            }
        });

        JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(table), meshViewPanel);
        rightPane.setResizeWeight(0.6);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(tree), rightPane);
        splitPane.setResizeWeight(0.35);

        frame.add(selectorPanel, BorderLayout.NORTH);
        frame.add(splitPane, BorderLayout.CENTER);

        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);

        populateFileCombo((GameMap) mapCombo.getSelectedItem());
        frame.setVisible(true);
    }

    private void populateFileCombo(GameMap map) {
        if (map == null) {
            return;
        }
        ignoreFileComboEvents = true;
        fileCombo.removeAllItems();
        List<String> paths = map.listSenFilePaths();
        for (String path : paths) {
            fileCombo.addItem(new SenFileChoice(path));
        }
        ignoreFileComboEvents = false;

        if (fileCombo.getItemCount() > 0) {
            fileCombo.setSelectedIndex(0);
            loadSelectedFile();
        } else {
            tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("No .SEN files in " + map.senFileDirectory)));
            tableModel.setSelected(null);
            meshViewPanel.setSenFile(null);
            frame.setTitle("SEN Viewer");
        }
    }

    private void loadSelectedFile() {
        if (ignoreFileComboEvents) {
            return;
        }
        SenFileChoice choice = (SenFileChoice) fileCombo.getSelectedItem();
        if (choice == null) {
            return;
        }
        try {
            SenFile senFile = SenFileFactory.fromFile(choice.path());
            DefaultTreeModel model = new SenFileTreeBuilder().buildModel(senFile);
            tree.setModel(model);
            tree.expandRow(0);
            expandChildrenOfRoot(model);
            meshViewPanel.setSenFile(senFile);
            frame.setTitle("SEN Viewer - " + choice.path());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Failed to load " + choice.path() + ":\n" + ex.getMessage(),
                    "Load error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void expandChildrenOfRoot(DefaultTreeModel model) {
        Object root = model.getRoot();
        for (int i = 0; i < model.getChildCount(root); i++) {
            tree.expandPath(new TreePath(new Object[]{root, model.getChild(root, i)}));
        }
    }

    private record SenFileChoice(String path) {
        @Override
        public String toString() {
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }
}
