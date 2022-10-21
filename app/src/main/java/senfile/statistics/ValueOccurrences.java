package senfile.statistics;

public class ValueOccurrences implements Comparable<ValueOccurrences> {

    public final Object value;
    public final int occurrences;

    public ValueOccurrences(Object value, int occurrences) {
        this.value = value;
        this.occurrences = occurrences;
    }

    @Override
    public String toString() {
        if (value instanceof Number) {
            return String.format("(0x%-8X) %10d - %d", value, value, occurrences);
        }
        return String.format("%20s - %d", value.toString(), occurrences);
    }

    @Override
    public int compareTo(ValueOccurrences o) {
        return value.toString().compareTo(o.value.toString());
    }
}
