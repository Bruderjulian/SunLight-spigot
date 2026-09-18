package su.nightexpress.sunlight.module.bans.punishment;

import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;

public class PunishmentReason implements Writeable {

    private String text;

    public PunishmentReason(String text) {
        this.setText(text);
    }

    public static PunishmentReason read(FileConfig config, String path) {
        String message = ConfigValue.create(path + ".Message", "Violation of the rules.").read(config);

        return new PunishmentReason(message);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Message", this.text);
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
