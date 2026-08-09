package senviewer;

import javax.swing.table.AbstractTableModel;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PropertyTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"Property", "Value"};

    private final List<Object[]> rows = new ArrayList<>();

    public void setSelected(Object value) {
        rows.clear();
        if (value != null) {
            if (value.getClass().isArray()) {
                int length = Array.getLength(value);
                for (int i = 0; i < length; i++) {
                    rows.add(new Object[]{"[" + i + "]", FieldInspector.formatValue(Array.get(value, i))});
                }
            } else if (value instanceof Collection) {
                int i = 0;
                for (Object element : (Collection<?>) value) {
                    rows.add(new Object[]{"[" + i++ + "]", FieldInspector.formatValue(element)});
                }
            } else if (value instanceof Map) {
                for (Object key : ((Map<?, ?>) value).keySet()) {
                    rows.add(new Object[]{key, FieldInspector.formatValue(((Map<?, ?>) value).get(key))});
                }
            } else {
                for (Field field : FieldInspector.getFields(value.getClass())) {
                    Object fieldValue;
                    try {
                        fieldValue = field.get(value);
                    } catch (IllegalAccessException ex) {
                        fieldValue = "<inaccessible>";
                    }
                    rows.add(new Object[]{field.getName(), FieldInspector.formatValue(fieldValue)});
                }
            }
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
