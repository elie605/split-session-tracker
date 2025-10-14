package com.splitmanager.utils;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;

/**
 * Gson adapter for java.time.Instant (ISO-8601 text), avoiding reflective access issues
 * on older JVMs/contexts. Stored as Instant.toString(), parsed with Instant.parse().
 */
public class InstantTypeAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
    @Override
    public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        return Instant.parse(json.getAsString());
    }
}
