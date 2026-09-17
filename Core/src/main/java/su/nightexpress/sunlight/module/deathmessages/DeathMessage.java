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

    public DeathMessage(@NotNull List<String> messages) {
        this(messages, Lists.newList());
    }

    public DeathMessage(@NotNull List<String> messages, @NotNull List<String> messagesByPlayer) {
        this.messages = messages;
        this.messagesByPlayer = messagesByPlayer;
    }

    @NotNull
    public static DeathMessage simple(@NotNull String message) {
        return new DeathMessage(Lists.newList(message));
    }

    @NotNull
    public static DeathMessage read(@NotNull FileConfig config, @NotNull String path) {
        List<String> messages = config.getStringList(path + ".Messages");
        List<String> messagesByPlayer = config.getStringList(path + ".Messages_By_Player");
        return new DeathMessage(messages, messagesByPlayer);
    }

    @Override
    public void write(@NotNull FileConfig config, @NotNull String path) {
        config.set(path + ".Messages", this.messages);
        if (!this.messagesByPlayer.isEmpty()) {
            config.set(path + ".Messages_By_Player", this.messagesByPlayer);
        }
    }

    @Nullable
    public String selectMessage() {
        return this.selectMessage(false);
    }

    @Nullable
    public String selectMessage(boolean playerCaused) {
        if (playerCaused && !this.messagesByPlayer.isEmpty()) {
            return Rnd.get(this.messagesByPlayer);
        }
        return this.messages.isEmpty() ? null : Rnd.get(this.messages);
    }

    @NotNull
    public List<String> getMessages() {
        return messages;
    }

    @NotNull
    public List<String> getMessagesByPlayer() {
        return messagesByPlayer;
    }
}
