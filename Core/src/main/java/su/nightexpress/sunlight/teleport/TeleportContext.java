package su.nightexpress.sunlight.teleport;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import su.nightexpress.sunlight.module.Module;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class TeleportContext {

    private final Module module;
    private final CommandSender sender;
    private final Player target;
    private final EnumSet<TeleportFlag> flags;
    private final Runnable callback;

    private Location destination;

    public TeleportContext(Module module,
            CommandSender sender,
            Player target,
            Location destination,
            EnumSet<TeleportFlag> flags,
            Runnable callback) {
        this.module = module;
        this.sender = sender;
        this.target = target;
        this.flags = flags;
        this.callback = callback;

        this.setDestination(destination);
    }

    public static Builder builder(Module module, Player target, Location destination) {
        return new Builder(module, target, destination);
    }

    public void runCallback() {
        if (this.hasCallback()) {
            this.callback.run();
        }
    }

    public boolean hasSender() {
        return this.sender != null;
    }

    public boolean hasFlags() {
        return !this.flags.isEmpty();
    }

    public boolean hasFlag(TeleportFlag flag) {
        return this.flags.contains(flag);
    }

    public boolean hasCallback() {
        return this.callback != null;
    }

    public CommandSender getExecutor() {
        return this.hasSender() ? this.sender : this.target;
    }

    public Module getModule() {
        return this.module;
    }

    public CommandSender getSender() {
        return this.sender;
    }

    public Player getTarget() {
        return this.target;
    }

    public Location getDestination() {
        return this.destination;
    }

    public void setDestination(Location destination) {
        this.destination = destination.clone();
    }

    public EnumSet<TeleportFlag> getFlags() {
        return this.flags;
    }

    public Runnable getCallback() {
        return this.callback;
    }

    public static class Builder {

        private final Module module;
        private final Player target;
        private final Location destination;
        private final Set<TeleportFlag> flags;

        private CommandSender sender;
        private Runnable callback;

        Builder(Module module, Player target, Location destination) {
            this.module = module;
            this.target = target;
            this.destination = destination.clone();
            this.flags = new HashSet<>();
        }

        public TeleportContext build() {
            EnumSet<TeleportFlag> flagSet = this.flags.isEmpty() ? EnumSet.noneOf(TeleportFlag.class)
                    : EnumSet.copyOf(this.flags);
            return new TeleportContext(this.module, this.sender, this.target, this.destination, flagSet, this.callback);
        }

        public Builder sender(CommandSender sender) {
            if (sender != this.target) {
                this.sender = sender;
            }
            return this;
        }

        public Builder withFlag(TeleportFlag flag) {
            this.flags.add(flag);
            return this;
        }

        public Builder withFlagIf(TeleportFlag flag, Supplier<Boolean> predicate) {
            if (predicate.get()) {
                return this.withFlag(flag);
            }
            return this;
        }

        public Builder callback(Runnable callback) {
            this.callback = callback;
            return this;
        }
    }
}
