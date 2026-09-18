package su.nightexpress.sunlight.data.serialize;

import su.nightexpress.nightcore.user.UserInfo;

import java.lang.reflect.Type;
import java.util.UUID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class UserInfoSerializer implements JsonSerializer<UserInfo>, JsonDeserializer<UserInfo> {

    @Override
    public UserInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject object = json.getAsJsonObject();

        UUID id = UUID.fromString(object.get("id").getAsString());
        String name = object.get("name").getAsString();

        return new UserInfo(id, name);
    }

    @Override
    public JsonElement serialize(UserInfo userInfo, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        object.addProperty("id", userInfo.id().toString());
        object.addProperty("name", userInfo.name());

        return object;
    }
}
