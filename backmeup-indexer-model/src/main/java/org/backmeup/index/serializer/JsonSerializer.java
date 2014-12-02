package org.backmeup.index.serializer;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.backmeup.index.model.IndexDocument;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.reflect.TypeToken;

public class JsonSerializer {

    private static class DateSerializer implements com.google.gson.JsonSerializer<Date>, JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Date(json.getAsLong());
        }

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getTime());
        }
    }

    private static class IndexDocumentSerializer implements JsonDeserializer<IndexDocument> {

        @Override
        public IndexDocument deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject self = json.getAsJsonObject();

            IndexDocument document = new IndexDocument();
            deserializeFieldsInto(document, self);
            document.setLargeFields(deserializeLargeFields(self, context));
            return document;
        }

        private void deserializeFieldsInto(IndexDocument document, JsonObject jsonDocument) {
            JsonElement fieldsJson = jsonDocument.get("fields");
            for (Entry<String, JsonElement> entry : fieldsJson.getAsJsonObject().entrySet()) {
                String name = entry.getKey();
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive() && ((JsonPrimitive) value).isNumber()) {
                    document.field(name, value.getAsLong());
                } else {
                    document.field(name, value.getAsString());
                }
            }
        }

        private Map<String, String> deserializeLargeFields(JsonObject jsonDocument, JsonDeserializationContext context) {
            JsonElement largeFieldsMapJson = jsonDocument.get("largeFields");
            Type mapType = new TypeToken<Map<String, String>>() {
            }.getType();
            return context.<Map<String, String>> deserialize(largeFieldsMapJson, mapType);
        }

    }

    private static final GsonBuilder builder = new GsonBuilder();

    static {
        builder.registerTypeAdapter(Date.class, new DateSerializer());
        builder.registerTypeAdapter(IndexDocument.class, new IndexDocumentSerializer());
    }

    public static <T> String serialize(T entry) {
        Gson gson = builder.create();
        return gson.toJson(entry);
    }

    public static <T> T deserialize(String entry, Class<T> clazz) {
        Gson gson = builder.create();
        return gson.fromJson(entry, clazz);
    }

    public static <T> T deserialize(String entry, Type type) {
        Gson gson = builder.create();
        return gson.fromJson(entry, type);
    }
}
