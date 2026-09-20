package su.nightexpress.sunlight.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import su.nightexpress.nightcore.db.column.Column;
import su.nightexpress.nightcore.db.column.ColumnDataReader;
import su.nightexpress.sunlight.command.CommandKey;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserColumns {

    public static final Column<Integer> ID = Column.intType("id").autoIncrement().primaryKey().build();
    public static final Column<UUID> UUID = Column.uuidType("uuid").unique().build();
    public static final Column<String> NAME = Column.stringType("name", 24).build();
    public static final Column<Long> DATE_CREATED = Column.longType("dateCreated").build();
    public static final Column<Long> LAST_ONLINE = Column.longType("last_online").build();
    public static final Column<String> INET_ADDRESS = Column.tinyText("ip").build();

    public static final Column<Map<CommandKey, Long>> COMMAND_COOLDOWNS = Column
            .json("commandCooldowns", ColumnDataReader.jsonMap(DataHandler.GSON, CommandKey.class, Long.class))
            .defaultValue("{}")
            .build();

    public static final Column<Map<String, Object>> PROPERTIES = Column.json("properties", (resultSet, column) -> {
        Map<String, Object> properties = new HashMap<>();

        String jsonString;
        try {
            jsonString = resultSet.getString(column);
        } catch (Exception exception) {
            return properties;
        }
        if (jsonString == null || jsonString.isBlank()) return properties;

        JsonElement root;
        try {
            root = JsonParser.parseString(jsonString);
        } catch (Exception exception) {
            return properties;
        }
        if (!root.isJsonObject()) return properties;

        JsonObject json = root.getAsJsonObject();
        json.asMap().forEach((key, element) -> {
            UserProperty<?> property;
            try {
                property = UserPropertyRegistry.getByName(key);
            } catch (Exception exception) {
                return;
            }
            if (property == null) return;
            if (element == null || element.isJsonNull()) return;

            try {
                Object value = DataHandler.GSON.fromJson(element, property.getGenericType());
                if (value != null) {
                    properties.put(property.getName(), value);
                }
            } catch (Exception ignored) {
            }
        });

        return properties;
    }).defaultValue("{}").build();
}
