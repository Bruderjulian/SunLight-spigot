package su.nightexpress.sunlight.moduleImpl.chat.format;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;

public class FormatComponent implements Writeable {

    private final String text;

    public FormatComponent(String text) {
        this.text = text;
    }

    public static FormatComponent read(FileConfig config, String path) {
        String text = config.getString(path + ".Text", "");
        return new FormatComponent(text);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Text", this.text);
    }

    public String getText() {
        return this.text;
    }
}
