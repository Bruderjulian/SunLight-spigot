package su.nightexpress.sunlight.moduleImpl.inventories.dialog;

import su.nightexpress.nightcore.ui.dialog.wrap.DialogKey;
import su.nightexpress.sunlight.moduleImpl.inventories.dialog.impl.InventoryClearDialog;

public class InventoryDialogKeys {

    private InventoryDialogKeys() {

    }

    public static final DialogKey<InventoryClearDialog.ClearRequest> CLEAR = new DialogKey<>("clear");

}
