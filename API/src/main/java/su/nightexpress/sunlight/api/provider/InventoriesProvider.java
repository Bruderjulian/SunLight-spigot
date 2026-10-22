package su.nightexpress.sunlight.api.provider;

/**
 * View of the Inventories module's settings that other plugins need to respect. Obtained via
 * {@code SunLightPlugin#inventoriesProvider()}, which returns an empty {@link java.util.Optional}
 * when the module is disabled, so callers must treat this as a soft dependency.
 * <p>
 * This module holds no state of its own: the work is done by its own {@code /inv}, {@code /enderchest}
 * and {@code /clear} commands and the NMS bridge behind them. What is exposed here is the pair of
 * settings those commands enforce, so that a third-party command clearing a player's inventory can
 * ask the same question the module asks and get the same answer.
 */
public interface InventoriesProvider {

    /**
     * Whether the module asks the player to confirm before clearing their inventory. A command
     * that clears an inventory should honour this.
     */
    boolean isClearConfirmationRequired();

    /**
     * Whether that confirmation is limited to the player confirming their own inventory. When
     * {@code false}, clearing somebody else's needs no confirmation from anyone.
     */
    boolean isClearConfirmSelfOnly();
}