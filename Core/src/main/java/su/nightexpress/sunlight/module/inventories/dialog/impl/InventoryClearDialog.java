package su.nightexpress.sunlight.module.inventories.dialog.impl;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.DialogActions;
import su.nightexpress.nightcore.ui.dialog.build.DialogBases;
import su.nightexpress.nightcore.ui.dialog.build.DialogBodies;
import su.nightexpress.nightcore.ui.dialog.build.DialogButtons;
import su.nightexpress.nightcore.ui.dialog.build.DialogTypes;
import su.nightexpress.nightcore.ui.dialog.wrap.Dialog;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;

public class InventoryClearDialog extends Dialog<InventoryClearDialog.ClearRequest> {

        public enum ClearType {
                INVENTORY,
                ENDER_CHEST
        }

        public record ClearRequest(Player target, ClearType type) {

        }

        private static final TextLocale TITLE = LangEntry.builder("Inventories.Dialog.Clear.Title")
                        .text(title("Inventory",
                                        "Clear"));

        private static final DialogElementLocale BODY_INVENTORY = LangEntry
                        .builder("Inventories.Dialog.Clear.Body.Inventory").dialogElement(400,
                                        "Are you sure you want to clear your inventory?",
                                        "",
                                        TagWrappers.SOFT_RED.wrap("This action cannot be undone!"));

        private static final DialogElementLocale BODY_ENDER_CHEST = LangEntry
                        .builder("Inventories.Dialog.Clear.Body.EnderChest").dialogElement(400,
                                        "Are you sure you want to clear your Ender Chest?",
                                        "",
                                        TagWrappers.SOFT_RED.wrap("This action cannot be undone!"));

        @Override

        public WrappedDialog create(Player viewer, ClearRequest request) {
                DialogElementLocale body = request.type() == ClearType.INVENTORY ? BODY_INVENTORY : BODY_ENDER_CHEST;

                return Dialogs.builder()
                                .base(DialogBases.builder(TITLE)
                                                .body(DialogBodies.plainMessage(body))
                                                .build())
                                .type(DialogTypes.multiAction(DialogButtons.confirm())
                                                .exitAction(DialogButtons.cancel()).build())
                                .handleResponse(DialogActions.CONFIRM, (v, identifier, nbtHolder) -> v.callback())
                                .build();
        }
}
