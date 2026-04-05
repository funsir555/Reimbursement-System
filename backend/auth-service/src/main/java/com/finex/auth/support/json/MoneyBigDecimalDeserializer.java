package com.finex.auth.support.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.math.BigDecimal;

public class MoneyBigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        String raw = parser.getValueAsString();
        if (raw == null) {
            return null;
        }

        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return MoneySupport.parseInput(text);
        } catch (IllegalArgumentException ex) {
            throw InvalidFormatException.from(parser, ex.getMessage(), text, BigDecimal.class);
        }
    }
}
