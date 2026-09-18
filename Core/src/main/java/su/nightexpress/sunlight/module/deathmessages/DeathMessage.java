package su.nightexpress.sunlight.module.deathmessages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.random.Rnd;

import java.util.List;

public class DeathMessage implements Writeable {

    private final List<String> messages;
    private final List<String> messagesByPlayer;

    public DeathMessage(List<String> messages) {
        this(messages, Lists.newList());
    }

    public DeathMessage(List<String> messages, List<String> messagesByPlayer) {
        this.messages = messages;
        this.messagesByPlayer = messagesByPlayer;
    }

    public static DeathMessage simple(String message) {
        return new DeathMessage(Lists.newList(message));
    }

    public static DeathMessage read(FileConfig config, String path) {
        List<String> messages = config.getStringList(path + ".Messages");
        List<String> messagesByPlayer = config.getStringList(path + ".Messages_By_Player");
        return new DeathMessage(messages, messagesByPlayer);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Messages", this.messages);
        if (!this.messagesByPlayer.isEmpty()) {
            config.set(path + ".Messages_By_Player", this.messagesByPlayer);
        }
    }

    public String selectMessage() {
        return this.selectMessage(false);
    }

    public String selectMessage(boolean playerCaused) {
        if (playerCaused && !this.messagesByPlayer.isEmpty()) {
            return Rnd.get(this.messagesByPlayer);
        }
        return this.messages.isEmpty() ? null : Rnd.get(this.messages);
    }

    public List<String> getMessages() {
        return messages;
    }

    public List<String> getMessagesByPlayer() {
        return messagesByPlayer;
    }
}
