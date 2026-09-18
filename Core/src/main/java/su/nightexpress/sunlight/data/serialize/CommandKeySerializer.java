package su.nightexpress.sunlight.data.serialize;

import su.nightexpress.sunlight.command.CommandKey;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class CommandKeySerializer implements JsonSerializer<CommandKey>, JsonDeserializer<CommandKey> {

    @Override
    public CommandKey deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return CommandKey.fromKeyString(json.getAsString());
    }

    @Override
    public JsonElement serialize(CommandKey key, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(key.toKeyString());
    }
}
