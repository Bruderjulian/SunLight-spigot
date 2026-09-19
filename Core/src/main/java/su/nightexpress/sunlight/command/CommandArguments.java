package su.nightexpress.sunlight.command;

import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.builder.ArgumentNodeBuilder;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.exceptions.CommandSyntaxException;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.Enums;
import su.nightexpress.nightcore.util.bridge.RegistryType;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.config.Lang;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CommandArguments {

        public static final String PLAYER = "player";
        public static final String TARGET = "target";
        public static final String AMOUNT = "amount";
        public static final String TIME = "time";
        public static final String VALUE = "value";
        public static final String WORLD = "world";
        public static final String TYPE = "type";
        public static final String MODE = "mode";
        public static final String STATE = "state";
        public static final String INET_ADDRESS = "address";
        public static final String ITEM = "item";
        public static final String RADIUS = "radius";
        public static final String NAME = "name";
        public static final String LEVEL = "level";
        public static final String ENCHANT = "enchant";
        public static final String TEXT = "text";
        public static final String POSITION = "position";
        public static final String X = "x";
        public static final String Y = "y";
        public static final String Z = "z";

        public static final String FLAG_SILENT = "s";
        public static final String FLAG_FORCE = "f";

        public static List<String> getTargetPosSuggestions(CommandContext context, Function<Block, Integer> function) {
                return Optional.ofNullable(context.getPlayer())
                                .map(player -> player.getTargetBlock(null, 100))
                                .map(function)
                                .map(String::valueOf)
                                .map(List::of)
                                .orElse(Collections.emptyList());
        }

        public static ArgumentNodeBuilder<EquipmentSlot> slot(String name) {
                return Commands.argument(name, (context, string) -> Enums.parse(string, EquipmentSlot.class)
                                .filter(slot -> slot != EquipmentSlot.BODY)
                                .orElseThrow(() -> CommandSyntaxException.custom(Lang.COMMAND_SYNTAX_INVALID_SLOT)))
                                .localized(Lang.COMMAND_ARGUMENT_NAME_SLOT.text())
                                .suggestions((reader, context) -> Arrays.stream(EntityUtil.EQUIPMENT_SLOTS)
                                                .map(Enum::name)
                                                .map(String::toLowerCase).toList());
        }

        public static <E extends Enum<E>> ArgumentNodeBuilder<E> enumed(String name, Class<E> clazz) {
                return Commands
                                .argument(name,
                                                (context, str) -> Enums.parse(str, clazz).orElseThrow(
                                                                () -> CommandSyntaxException.custom(
                                                                                Lang.ERROR_COMMAND_INVALID_TYPE_ARGUMENT)))
                                .localized(Lang.COMMAND_ARGUMENT_NAME_TYPE.text())
                                .suggestions((reader, tabContext) -> Stream.of(clazz.getEnumConstants()).sorted()
                                                .map(c -> String.valueOf(c).toLowerCase()).toList());
        }

        public static ArgumentNodeBuilder<EntityType> entityType(Predicate<EntityType> predicate) {
                return Commands.argument(TYPE,
                                (context, string) -> Optional.ofNullable(BukkitThing.getEntityType(string))
                                                .filter(predicate)
                                                .orElseThrow(() -> CommandSyntaxException
                                                                .custom(Lang.COMMAND_SYNTAX_INVALID_ENTITY_TYPE)))
                                .localized(Lang.COMMAND_ARGUMENT_NAME_ENTITY_TYPE)
                                .suggestions((reader, tabContext) -> BukkitThing.getAll(RegistryType.ENTITY_TYPE)
                                                .stream()
                                                .filter(predicate).map(BukkitThing::getValue).toList());
        }

        public static ArgumentNodeBuilder<PotionEffectType> effect(String name, Predicate<PotionEffectType> predicate) {
                return Commands.argument(name,
                                (context, string) -> Optional.ofNullable(BukkitThing.getEffectType(string))
                                                .filter(predicate)
                                                .orElseThrow(() -> CommandSyntaxException
                                                                .custom(Lang.COMMAND_SYNTAX_INVALID_MOB_EFFECT)))
                                .localized(Lang.COMMAND_ARGUMENT_NAME_TYPE.text())
                                .suggestions((reader, tabContext) -> BukkitThing.getAll(RegistryType.MOB_EFFECT)
                                                .stream()
                                                .filter(predicate).map(BukkitThing::getAsString).toList());
        }

        /*
         * @Deprecated
         * public static ArgumentNodeBuilder<ToggleMode> toggle( String name) {
         * return Commands.argument(name, (context, string) -> Enums.parse(string,
         * ToggleMode.class)
         * .orElseThrow(() ->
         * CommandSyntaxException.custom(Lang.COMMAND_SYNTAX_INVALID_MODE))
         * )
         * .localized(Lang.COMMAND_ARGUMENT_NAME_MODE)
         * .suggestions((reader, context) -> Enums.getNames(ToggleMode.class));
         * }
         */

        @Deprecated
        public static ArgumentNodeBuilder<ToggleMode> toggleMode(String name) {
                return enumed(name, ToggleMode.class)
                                .localized(Lang.COMMAND_ARGUMENT_NAME_MODE.text());
        }

        public static ArgumentNodeBuilder<InetAddress> inetAddress(String name) {
                return Commands.argument(INET_ADDRESS, (builder, string) -> {
                        try {
                                return InetAddress.getByName(string);
                        } catch (UnknownHostException exception) {
                                throw CommandSyntaxException.custom(Lang.COMMAND_SYNTAX_INVALID_INET_ADDRESS);
                        }
                }).localized(Lang.COMMAND_ARGUMENT_NAME_INET_ADDRESS);
        }

        public static boolean handleItemInHandOrError(CommandContext context,
                        BiFunction<Player, ItemStack, Boolean> consumer) {
                Player player = context.getPlayerOrThrow();
                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack.getType().isAir()) {
                        context.send(Lang.ERROR_REQUIRES_ITEM_IN_HAND);
                        return false;
                }

                return consumer.apply(player, itemStack);
        }
}
