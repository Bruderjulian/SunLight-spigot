package su.nightexpress.sunlight.api;

public enum PortableContainer {

    ANVIL("anvil"),
    WORKBENCH("workbench"),
    ENCHANTING_TABLE("enchanting"),
    GRINDSTONE("grindstone"),
    LOOM("loom"),
    SMITHING_TABLE("smithing"),
    CARTOGRAPHY_TABLE("cartography"),
    STONECUTTER("stonecutter");

    private final String label;

    private PortableContainer(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }
}
