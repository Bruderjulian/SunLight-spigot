package su.nightexpress.sunlight.moduleImpl.recipes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;

public class RecipesModule extends Module {

    private final RecipesSettings settings;

    private final Map<String, ConfiguredRecipe> recipeByIdMap;

    public RecipesModule(ModuleDefinition<RecipesModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new RecipesSettings();
        this.recipeByIdMap = new HashMap<>();
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        this.settings.load(config);

        this.unregisterAll();
        this.recipeByIdMap.clear();

        this.loadRecipes();
        this.registerAll();
    }

    @Override
    protected void unloadModule() {
        this.unregisterAll();
        this.recipeByIdMap.clear();
    }

    @Override
    protected void registerCommands() {

    }

    @Override
    protected void registerPermissions(PermissionTree root) {

    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }

    public String getDefaultNamespace() {
        return this.settings.defaultNamespace.get();
    }

    public Set<ConfiguredRecipe> getRecipes() {
        return Set.copyOf(this.recipeByIdMap.values());
    }

    private void loadRecipes() {
        Path dirPath = Path.of(this.getSystemPath(), RecipesFiles.DIR_RECIPES);
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
                this.createExampleRecipe(dirPath);
            }
            catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        FileUtil.findYamlFiles(dirPath).forEach(file -> {
            ConfiguredRecipe configured = ConfiguredRecipe.fromFile(this, file);
            if (configured == null)
                return;

            this.recipeByIdMap.put(configured.getId(), configured);
        });

        this.info("Loaded " + this.recipeByIdMap.size() + " recipes.");
    }

    private void registerAll() {
        for (ConfiguredRecipe configured : this.recipeByIdMap.values()) {
            if (configured.apply()) {
                this.info("Applied recipe '" + configured.getKey() + "' (" + configured.getAction().name() + ").");
            }
            else {
                this.warn("Could not apply recipe '" + configured.getId() + "' (" + configured.getKey()
                        + "). It may already exist or contains invalid data.");
            }
        }
    }

    private void unregisterAll() {
        this.recipeByIdMap.values().forEach(ConfiguredRecipe::unapply);
    }

    private void createExampleRecipe(Path dirPath) throws IOException {
        Path filePath = Path.of(dirPath.toString(), FileConfig.withExtension(RecipesFiles.FILE_EXAMPLE));
        FileConfig config = FileConfig.load(filePath);

        config.set("Enabled", true);
        config.set("Action", ConfiguredRecipe.Action.ADD.name());
        config.set("Key", "sunlight_recipes:diamond_ring");
        config.set("Type", ConfiguredRecipe.Type.SHAPED.name());
        config.set("Group", "");
        config.set("Result.Material", "DIAMOND");
        config.set("Result.Amount", 1);
        config.set("Pattern", List.of("ABA", "BEB", "ABA"));
        config.set("Ingredients.A.Material", "EMERALD");
        config.set("Ingredients.B.Materials", List.of("IRON_INGOT", "GOLD_INGOT"));
        config.set("Ingredients.E.Item", "minecraft:stick{Custom_Model_Data:1}");

        config.setComments("Enabled",
                "Whether this recipe definition should be applied.");
        config.setComments("Action",
                "ADD    - Registers a brand new recipe with the given 'Key'.",
                "MODIFY - Replaces the recipe with the given 'Key' (if any) with the one defined below.",
                "REMOVE - Removes the recipe with the given 'Key'.");
        config.setComments("Key",
                "Unique key of the recipe: <namespace>:<key>.",
                "If the namespace part is omitted, 'ADD' recipes use the 'Default_Namespace' from",
                "settings.yml, while 'MODIFY'/'REMOVE' fall back to the 'minecraft' namespace.");
        config.setComments("Type",
                "Crafting recipe type: SHAPED (arrangement matters) or SHAPELESS (any arrangement).",
                "Not used for the REMOVE action.");
        config.setComments("Group",
                "Optional recipe group, used to organize recipes inside the recipe book.");
        config.setComments("Result",
                "Output of the recipe (required for ADD/MODIFY).",
                "Supports the standard item section: Material, Amount, Name, Lore, Enchants, Custom_Model_Data, ...");
        config.setComments("Pattern",
                "For SHAPED recipes: 1-3 rows of 1-3 characters.",
                "A space (' ') marks an empty slot; every other character must be defined in 'Ingredients'.", "");
        config.setComments("Ingredients",
                "Ingredients used by the recipe.",
                "For SHAPED recipes the keys are the single characters from the 'Pattern'.",
                "For SHAPELESS recipes the keys are arbitrary unique labels (e.g. 1, 2, 3...).",
                "Each ingredient can be defined as one of:", "",
                "  Material: <material>          - a single material",
                "  Materials: [<material>, ...]  - a choice between multiple materials",
                "  Item: \"<item-tag>\"           - an exact item (SNBT, e.g. 'minecraft:stick{Custom_Model_Data:1}')", "",
                "Use item tags by prefixing with '#', e.g. '#minecraft:planks' or '#minecraft:logs'.");

        config.save();
    }
}