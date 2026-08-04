package distributed.tinyurl.urlservice.idgen;

import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    private final SnowflakeIdGenerator idGenerator;

    public ShortCodeGenerator(SnowflakeIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public String nextCode() {
        return Base62Encoder.encode(idGenerator.nextId());
    }
}