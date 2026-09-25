package su.nightexpress.sunlight.moduleImpl.nametags;

import su.nightexpress.sunlight.user.property.UserProperty;

public class NametagProperties {

    public static final UserProperty<String> TAG_SELECTED = UserProperty.create("nametags_selected_tag", String.class, "", true);

    public static final UserProperty<Boolean> RANK_ENABLED = UserProperty.create("nametags_rank_enabled", Boolean.class, true, true);

    public static final UserProperty<Boolean> TAG_ENABLED = UserProperty.create("nametags_tag_enabled", Boolean.class, true, true);
}