package senviewer;

import senfile.SenFile;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

public class SenFileTreeBuilder {

    private final IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();

    public DefaultTreeModel buildModel(SenFile senFile) {
        return new DefaultTreeModel(createNode("SenFile", senFile));
    }

    private DefaultMutableTreeNode createNode(String name, Object value) {
        if (value == null) {
            return leaf(name + " = null", null);
        }
        if (FieldInspector.isSimple(value)) {
            return leaf(name + " = " + value, value);
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            DefaultMutableTreeNode node = branch(name + " (" + type.getComponentType().getSimpleName() + "[" + Array.getLength(value) + "])", value);
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                node.add(createNode("[" + i + "]", Array.get(value, i)));
            }
            return node;
        }
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            DefaultMutableTreeNode node = branch(name + " (Collection " + collection.size() + ")", value);
            int i = 0;
            for (Object element : collection) {
                node.add(createNode("[" + i++ + "]", element));
            }
            return node;
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            DefaultMutableTreeNode node = branch(name + " (Map " + map.size() + ")", value);
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                node.add(createNode(String.valueOf(entry.getKey()), entry.getValue()));
            }
            return node;
        }
        if (visited.containsKey(value)) {
            return leaf(name + " (already shown)", value);
        }
        if (FieldInspector.isLeafObject(value)) {
            return leaf(name + " = " + FieldInspector.formatLeafObject(value), value);
        }
        visited.put(value, Boolean.TRUE);
        String objectName = objectName(value);
        String label = name + " (" + type.getSimpleName() + (objectName != null ? ": " + objectName : "") + ")";
        DefaultMutableTreeNode node = branch(label, value);
        for (Field field : FieldInspector.getFields(type)) {
            Object fieldValue;
            try {
                fieldValue = field.get(value);
            } catch (IllegalAccessException ex) {
                fieldValue = "<inaccessible>";
            }
            node.add(createNode(field.getName(), fieldValue));
        }
        return node;
    }

    private static String objectName(Object value) {
        for (Field field : FieldInspector.getFields(value.getClass())) {
            if (field.getType() == String.class
                    && (field.getName().equals("name") || field.getName().equals("nameOfMesh"))) {
                try {
                    return (String) field.get(value);
                } catch (IllegalAccessException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private static DefaultMutableTreeNode branch(String label, Object value) {
        return new DefaultMutableTreeNode(new NodeValue(label, value));
    }

    private static DefaultMutableTreeNode leaf(String label, Object value) {
        return new DefaultMutableTreeNode(new NodeValue(label, value));
    }

    public static final class NodeValue {
        public final String label;
        public final Object value;

        public NodeValue(String label, Object value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
