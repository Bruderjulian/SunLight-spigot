package su.nightexpress.sunlight.module;

import java.nio.file.Path;

import su.nightexpress.nightcore.ui.dialog.wrap.DialogRegistry;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandRegistry;
import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.user.UserManager;

/**
 * A Parameter Object that bundles all common services and instance-specific
 * data required for loading a new Module.
 * <p>
 * This avoids "parameter proliferation" in constructors and loaders.
 *
 * @param plugin          The main SunLight instance.
 * @param dataHandler     The DataHandler instance.
 * @param userManager     The UserManager instance.
 * @param commandRegistry The SunLight's command registry.
 * @param id              The unique ID for this specific module instance.
 * @param path            The data folder path for this module.
 * @param definition      The configuration-defined definition for this module.
 */
public record ModuleContext(
        SunLightPlugin plugin,
        DataHandler dataHandler,
        UserManager userManager,
        CommandRegistry commandRegistry,
        DialogRegistry dialogRegistry,
        String id,
        Path path,
        ModuleDefinition definition) {

}
