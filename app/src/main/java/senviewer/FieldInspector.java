package senviewer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FieldInspector {

    private FieldInspector() {
    }

    public static List<Field> getFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    public static boolean isSimple(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof String
                || value instanceof Character
                || value instanceof Boolean
                || value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double
                || value instanceof Enum;
    }

    public static boolean isLeafObject(Object value) {
        for (Field field : getFields(value.getClass())) {
            Object fieldValue;
            try {
                fieldValue = field.get(value);
            } catch (IllegalAccessException ex) {
                return false;
            }
            if (!isSimple(fieldValue)) {
                return false;
            }
        }
        return true;
    }

    public static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (isSimple(value)) {
            return value.toString();
        }
        Class<?> type = value.getClass();
        if (type.isArray()) {
            return type.getComponentType().getSimpleName() + "[" + Array.getLength(value) + "]";
        }
        if (value instanceof Collection) {
            return "Collection (" + ((Collection<?>) value).size() + ")";
        }
        if (value instanceof Map) {
            return "Map (" + ((Map<?, ?>) value).size() + ")";
        }
        return type.getSimpleName() + " @" + Integer.toHexString(System.identityHashCode(value));
    }

    public static String formatLeafObject(Object value) {
        StringBuilder sb = new StringBuilder(value.getClass().getSimpleName());
        sb.append('(');
        boolean first = true;
        for (Field field : getFields(value.getClass())) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            try {
                sb.append(field.getName()).append('=').append(field.get(value));
            } catch (IllegalAccessException ex) {
                sb.append(field.getName()).append("=?");
            }
        }
        return sb.append(')').toString();
    }
}
