package su.nightexpress.sunlight.moduleImpl.nick;

import su.nightexpress.sunlight.user.property.UserProperty;

public class NickProperties {

    public static final UserProperty<String> CUSTOM_NAME = UserProperty.create("custom_name", String.class, "", true);
}
