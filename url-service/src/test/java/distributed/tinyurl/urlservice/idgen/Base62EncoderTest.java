package distributed.tinyurl.urlservice.idgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Test
    void encodesZeroAsFirstCharacterOfAlphabet() {
        assertEquals("0", Base62Encoder.encode(0L));
    }

    @Test
    void encodesSmallValuesCorrectly() {
        assertEquals("1", Base62Encoder.encode(1L));
        assertEquals("Z", Base62Encoder.encode(35L)); // 0-9 (10) + A-Z (26) -> index 35 = 'Z'
    }

    @Test
    void maxLongValueNeverExceedsElevenCharacters() {
        String encoded = Base62Encoder.encode(Long.MAX_VALUE);

        assertTrue(encoded.length() <= 11,
                "ceil(log62(2^63)) = 11, Long should never exceed that.");
    }

    @Test
    void differentInputsProduceDifferentOutputs() {
        String a = Base62Encoder.encode(123456789L);
        String b = Base62Encoder.encode(987654321L);

        assertNotEquals(a, b);
    }
}