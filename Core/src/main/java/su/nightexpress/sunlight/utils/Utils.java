package su.nightexpress.sunlight.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import su.nightexpress.nightcore.util.Reflex;

import java.util.concurrent.CompletableFuture;

public class Utils {
  private static final boolean isPapiPresent = Reflex.classExists("me.clip.placeholderapi.PlaceholderAPI");

  public static boolean isInstalled(String pluginName) {
    Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
    return plugin != null;
  }

  public static boolean isLoaded(String pluginName) {
    Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
    return plugin != null && plugin.isEnabled();
  }

  public static boolean hasPlaceholderAPI() {
    return isPapiPresent;
  }

  public static <T> CompletableFuture<T> loggable(CompletableFuture<T> future) {
    return future.whenComplete(Utils::printStacktrace);
  }

  public static <T> void printStacktrace(T object, Throwable throwable) {
    if (throwable != null) {
      throwable.printStackTrace();
    }
  }

  public static String lowercase(String str) {
    return str == null ? null : str.toLowerCase(Locale.ROOT);
  }

  public static <T extends Enum<T>> T enumValueOf(String str, Class<T> type) {
    try {
      return str == null ? null : Enum.valueOf(type, str.toUpperCase());
    } catch (Exception exception) {
      return null;
    }
  }

  public static <T extends Enum<T>> Optional<T> enumOptionalValueOf(String str, Class<T> type) {
    try {
      return str == null ? Optional.empty() : Optional.of(Enum.valueOf(type, str.toUpperCase()));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  public static String enumToString(Class<? extends Enum<?>> type) {
    return Stream.of(type.getEnumConstants())
        .sorted(Comparator.comparingInt(Enum::ordinal)).map(Object::toString).reduce("", (prev, curr) -> {
          return prev.isEmpty() ? curr.toString() : prev + ", " + curr;
        });
  }

  public static List<String> getEnumNames(Class<? extends Enum<?>> type) {
    return Stream.of(type.getEnumConstants())
        .sorted(Comparator.comparingInt(Enum::ordinal))
        .map(Object::toString)
        .toList();
  }

  public static Player getPlayer(String name) {
    return Bukkit.getServer().getPlayer(name);
  }

  public static Player getPlayer(UUID uuid) {
    return Bukkit.getServer().getPlayer(uuid);
  }

  public static Set<Player> onlinePlayers() {
    return Set.copyOf(Bukkit.getServer().getOnlinePlayers());
  }

}
