package su.nightexpress.sunlight.moduleImpl.kits.config;

public class KitsPerms {

    public static final String MODULE  = "sunlight.kits";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";
    public static final String KIT     = MODULE + ".kit";

    public static final String COMMAND_KITS_ROOT          = COMMAND + ".kits.root";
    public static final String COMMAND_EDIT_KIT           = COMMAND + ".kits.editor";
    public static final String COMMAND_PREVIEW_KIT        = COMMAND + ".kits.preview";
    public static final String COMMAND_PREVIEW_KIT_OTHERS = COMMAND + ".kits.preview.others";
    public static final String COMMAND_KIT_GET            = COMMAND + ".kits.get";
    public static final String COMMAND_KIT_GIVE           = COMMAND + ".kits.give";
    public static final String COMMAND_KIT_LIST           = COMMAND + ".kits.list";
    public static final String COMMAND_KIT_LIST_OTHERS    = COMMAND + ".kits.list.others";
    public static final String COMMAND_RESET_KIT_COOLDOWN = COMMAND + ".kits.resetcooldown";
    public static final String COMMAND_SET_KIT_COOLDOWN   = COMMAND + ".kits.setcooldown";

    public static final String BYPASS_COST     = BYPASS + ".cost";
    public static final String BYPASS_COOLDOWN = BYPASS + ".cooldown";

    private KitsPerms() {
    }
}
