package senfile.serializer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static senfile.serializer.SenFileSerializer.roundTo4WithMargin;

public class SenFileSerializerTest {
    @Test
    public void testRoundTo4WithMargin() {
        assertEquals(4, roundTo4WithMargin(0));
        assertEquals(4, roundTo4WithMargin(1));
        assertEquals(4, roundTo4WithMargin(2));
        assertEquals(4, roundTo4WithMargin(3));
        assertEquals(8, roundTo4WithMargin(4));
        assertEquals(8, roundTo4WithMargin(5));
        assertEquals(8, roundTo4WithMargin(6));
        assertEquals(8, roundTo4WithMargin(7));
        assertEquals(12, roundTo4WithMargin(8));
        assertEquals(12, roundTo4WithMargin(9));
        assertEquals(12, roundTo4WithMargin(10));
        assertEquals(12, roundTo4WithMargin(11));
        assertEquals(16, roundTo4WithMargin(12));
        assertEquals(16, roundTo4WithMargin(13));
        assertEquals(16, roundTo4WithMargin(14));
    }

}