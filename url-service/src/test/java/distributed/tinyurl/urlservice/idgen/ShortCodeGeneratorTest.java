package distributed.tinyurl.urlservice.idgen;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    @Test
    void producesNonEmptyAlphanumericCode() {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, Clock.systemUTC());
        ShortCodeGenerator shortCodeGenerator = new ShortCodeGenerator(idGenerator);

        String code = shortCodeGenerator.nextCode();

        assertNotNull(code);
        assertFalse(code.isBlank());
        assertTrue(code.length() <= 11);
        assertTrue(code.chars().allMatch(Character::isLetterOrDigit));
    }

    @Test
    void producesDifferentCodesOnSuccessiveCalls() {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, Clock.systemUTC());
        ShortCodeGenerator shortCodeGenerator = new ShortCodeGenerator(idGenerator);

        String code1 = shortCodeGenerator.nextCode();
        String code2 = shortCodeGenerator.nextCode();

        assertNotEquals(code1, code2);
    }
}