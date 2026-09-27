package su.nightexpress.sunlight.moduleImpl.links.dialog;

import su.nightexpress.nightcore.ui.dialog.wrap.DialogKey;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

public class LinksDialogKeys {

    private LinksDialogKeys() {
    }

    public static final DialogKey<LinksModule> LINK_CREATION = new DialogKey<>("link_creation");
    public static final DialogKey<Link> LINK_DISPLAY_NAME  = new DialogKey<>("link_display_name");
    public static final DialogKey<Link> LINK_URL            = new DialogKey<>("link_url");
    public static final DialogKey<Link> LINK_COMMAND        = new DialogKey<>("link_command");
    public static final DialogKey<Link> LINK_PERMISSION     = new DialogKey<>("link_permission");
    public static final DialogKey<Link> LINK_USE_PERMISSION = new DialogKey<>("link_use_permission");
    public static final DialogKey<Link> LINK_PRIORITY       = new DialogKey<>("link_priority");
    public static final DialogKey<Link> LINK_COOLDOWN       = new DialogKey<>("link_cooldown");
    public static final DialogKey<Link> LINK_COST           = new DialogKey<>("link_cost");
    public static final DialogKey<Link> LINK_REWARD         = new DialogKey<>("link_reward");
    public static final DialogKey<Link> LINK_SOUND          = new DialogKey<>("link_sound");
    public static final DialogKey<Link> LINK_ACTIONBAR      = new DialogKey<>("link_actionbar");
    public static final DialogKey<Link> LINK_PARTICLE       = new DialogKey<>("link_particle");
    public static final DialogKey<Link> LINK_DELETION       = new DialogKey<>("link_deletion");
}
