package su.nightexpress.sunlight.moduleImpl.links;

import su.nightexpress.nightcore.util.placeholder.TypedPlaceholder;

public class LinksPlaceholders {

    public static final String LINK_ID             = "%link_id%";
    public static final String LINK_NAME           = "%link_name%";
    public static final String LINK_URL            = "%link_url%";
    public static final String LINK_COMMAND        = "%link_command%";
    public static final String LINK_EXECUTOR       = "%link_executor%";
    public static final String LINK_PERMISSION     = "%link_permission%";
    public static final String LINK_USE_PERMISSION = "%link_use_permission%";
    public static final String LINK_PRIORITY       = "%link_priority%";
    public static final String LINK_ENABLED        = "%link_enabled%";
    public static final String LINK_CLICKS         = "%link_clicks%";
    public static final String LINK_UNIQUE_CLICKS  = "%link_unique_clicks%";
    public static final String LINK_COOLDOWN       = "%link_cooldown%";
    public static final String LINK_COST           = "%link_cost%";

    public static final TypedPlaceholder<Link> LINK = TypedPlaceholder.builder(Link.class)
            .with(LINK_ID, Link::getId)
            .with(LINK_NAME, Link::getDisplay)
            .with(LINK_URL, Link::getUrl)
            .with(LINK_COMMAND, Link::getCommand)
            .with(LINK_EXECUTOR, link -> link.getExecutor().name())
            .with(LINK_PERMISSION, Link::getPermission)
            .with(LINK_USE_PERMISSION, Link::getUsePermission)
            .with(LINK_PRIORITY, link -> String.valueOf(link.getPriority()))
            .with(LINK_ENABLED, link -> String.valueOf(link.isEnabled()))
            .with(LINK_CLICKS, link -> String.valueOf(link.getClicks()))
            .with(LINK_UNIQUE_CLICKS, link -> String.valueOf(link.getUniqueClicks()))
            .with(LINK_COOLDOWN, link -> String.valueOf(link.getCooldown()))
            .with(LINK_COST, link -> String.valueOf(link.getCost()))
            .build();
}
