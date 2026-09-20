package su.nightexpress.sunlight.moduleImpl.recipes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.FileUtil;

public class ConfiguredRecipe {

    public enum Action {
        ADD, MODIFY, REMOVE
    }

    public enum Type {
        SHAPED, SHAPELESS
    }

    private final String id;
    private final Action action;
    private final Type type;
    private final String group;

    private final ItemStack result;
    private final List<String> pattern;
    private final Map<Character, RecipeChoice> shapedIngredients;
    private final List<RecipeChoice> ingredients;

    private NamespacedKey key;
    private Recipe recipe;
    private Recipe replaced;

    private boolean applied;

    private ConfiguredRecipe(String id, Action action, Type type, String group, ItemStack result,
            List<String> pattern, Map<Character, RecipeChoice> shapedIngredients,
            List<RecipeChoice> ingredients) {
        this.id = id;
        this.action = action;
        this.type = type;
        this.group = group;

        this.result = result;
        this.pattern = pattern;
        this.shapedIngredients = shapedIngredients;
        this.ingredients = ingredients;
    }

    public static ConfiguredRecipe fromFile(RecipesModule module, Path file) {
        String id = FileUtil.getNameWithoutExtension(file);
        FileConfig config = FileConfig.load(file);

        try {
            return fromConfig(module, id, config);
        }
        catch (RecipeParseException exception) {
            module.error("Could not load recipe '%s': %s".formatted(file, exception.getMessage()));
            return null;
        }
    }

    private static ConfiguredRecipe fromConfig(RecipesModule module, String id, FileConfig config)
            throws RecipeParseException {
        if (!config.getBoolean("Enabled", true)) {
            return null;
        }

        Action action = config.getEnum("Action", Action.class, Action.ADD);
        if (action == null) {
            throw new RecipeParseException("Missing or invalid 'Action'. Use ADD, MODIFY or REMOVE.");
        }

        String keyString = config.getString("Key");
        if (keyString == null || keyString.isBlank()) {
            throw new RecipeParseException("Missing or invalid 'Key'.");
        }

        Type type = config.getEnum("Type", Type.class, Type.SHAPED);
        String group = config.getString("Group", "");

        ItemStack result = null;
        List<String> pattern = new ArrayList<>();
        Map<Character, RecipeChoice> shapedIngredients = new LinkedHashMap<>();
        List<RecipeChoice> ingredients = new ArrayList<>();

        if (action != Action.REMOVE) {
            result = config.getItem("Result");
            if (result == null || result.getType().isAir()) {
                throw new RecipeParseException("Missing or invalid 'Result' item.");
            }

            if (type == Type.SHAPED) {
                pattern.addAll(config.getStringList("Pattern"));
                if (pattern.isEmpty()) {
                    throw new RecipeParseException("Missing 'Pattern' for a SHAPED recipe.");
                }

                for (String key : config.getSection("Ingredients")) {
                    if (key.length() != 1 || key.charAt(0) == ' ') {
                        throw new RecipeParseException(
                                "Ingredient key '%s' must be a single character (not a space) for SHAPED recipes."
                                        .formatted(key));
                    }
                    RecipeChoice choice = readChoice(config, "Ingredients." + key);
                    shapedIngredients.put(key.charAt(0), choice);
                }
                if (shapedIngredients.isEmpty()) {
                    throw new RecipeParseException("Missing 'Ingredients' for a SHAPED recipe.");
                }
            }
            else {
                for (String key : config.getSection("Ingredients")) {
                    ingredients.add(readChoice(config, "Ingredients." + key));
                }
                if (ingredients.isEmpty()) {
                    throw new RecipeParseException("Missing 'Ingredients' for a SHAPELESS recipe.");
                }
            }
        }

        ConfiguredRecipe configured = new ConfiguredRecipe(id, action, type, group, result, pattern,
                shapedIngredients, ingredients);

        String defaultNamespace = action == Action.ADD ? module.getDefaultNamespace() : NamespacedKey.MINECRAFT;
        configured.key = parseKey(keyString, defaultNamespace);

        if (action != Action.REMOVE) {
            configured.recipe = configured.buildRecipe();
        }

        return configured;
    }

    public boolean apply() {
        try {
            switch (this.action) {
                case ADD -> {
                    if (Bukkit.getRecipe(this.key) != null) {
                        return false;
                    }
                    if (!Bukkit.addRecipe(this.recipe)) {
                        return false;
                    }
                }
                case MODIFY -> {
                    this.replaced = Bukkit.getRecipe(this.key);
                    Bukkit.removeRecipe(this.key);
                    if (!Bukkit.addRecipe(this.recipe)) {
                        if (this.replaced != null) {
                            Bukkit.addRecipe(this.replaced);
                        }
                        return false;
                    }
                }
                case REMOVE -> {
                    this.replaced = Bukkit.getRecipe(this.key);
                    if (this.replaced == null) {
                        this.applied = true;
                        return true;
                    }
                    if (!Bukkit.removeRecipe(this.key)) {
                        return false;
                    }
                }
            }
        }
        catch (IllegalArgumentException exception) {
            return false;
        }

        this.applied = true;
        return true;
    }

    public void unapply() {
        if (!this.applied) {
            return;
        }

        if (this.action == Action.ADD || this.action == Action.MODIFY) {
            Bukkit.removeRecipe(this.key);
        }
        if (this.replaced != null) {
            Bukkit.addRecipe(this.replaced);
        }

        this.applied = false;
    }

    private Recipe buildRecipe() throws RecipeParseException {
        try {
            if (this.type == Type.SHAPED) {
                ShapedRecipe shaped = new ShapedRecipe(this.key, this.result.clone());
                shaped.shape(this.pattern.toArray(new String[0]));
                shaped.setGroup(this.group);
                this.shapedIngredients.forEach(shaped::setIngredient);
                return shaped;
            }

            ShapelessRecipe shapeless = new ShapelessRecipe(this.key, this.result.clone());
            shapeless.setGroup(this.group);
            this.ingredients.forEach(shapeless::addIngredient);
            return shapeless;
        }
        catch (IllegalArgumentException exception) {
            throw new RecipeParseException(exception.getMessage());
        }
    }

    private static RecipeChoice readChoice(FileConfig config, String path) throws RecipeParseException {
        Object raw = config.get(path);
        if (raw instanceof String string) {
            return readChoiceString(string, path);
        }
        if (raw instanceof Map<?, ?> map) {
            return readChoiceMap(map, path);
        }

        throw new RecipeParseException("Invalid ingredient defined at '%s'.".formatted(path));
    }

    private static RecipeChoice readChoiceString(String string, String path) throws RecipeParseException {
        if (looksLikeItemTag(string)) {
            return new RecipeChoice.ExactChoice(parseItemStack(string));
        }

        List<Material> materials = resolveMaterials(string, path);
        return new RecipeChoice.MaterialChoice(materials);
    }

    private static RecipeChoice readChoiceMap(Map<?, ?> map, String path) throws RecipeParseException {
        Object item = map.get("Item");
        if (item instanceof String string) {
            return new RecipeChoice.ExactChoice(parseItemStack(string));
        }

        List<Material> materials = new ArrayList<>();

        Object material = map.get("Material");
        if (material instanceof String string) {
            materials.addAll(resolveMaterials(string, path));
        }

        Object materialsRaw = map.get("Materials");
        if (materialsRaw instanceof Collection<?> collection) {
            for (Object entry : collection) {
                if (entry instanceof String string) {
                    materials.addAll(resolveMaterials(string, path));
                }
                else {
                    throw new RecipeParseException(
                            "Invalid material entry inside 'Materials' at '%s'.".formatted(path));
                }
            }
        }

        if (materials.isEmpty()) {
            throw new RecipeParseException("Missing 'Material', 'Materials' or 'Item' at '%s'.".formatted(path));
        }

        return new RecipeChoice.MaterialChoice(materials);
    }

    private static List<Material> resolveMaterials(String string, String path) throws RecipeParseException {
        if (string.startsWith("#")) {
            String tagString = string.substring(1);
            NamespacedKey tagKey = NamespacedKey.fromString(tagString);
            if (tagKey == null) {
                throw new RecipeParseException("Invalid item tag '%s' at '%s'.".formatted(string, path));
            }

            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, tagKey, Material.class);
            if (tag == null) {
                throw new RecipeParseException("Unknown item tag '%s' at '%s'.".formatted(string, path));
            }

            List<Material> values = new ArrayList<>(tag.getValues());
            values.removeIf(material -> material.isAir() || !material.isItem());
            if (values.isEmpty()) {
                throw new RecipeParseException("Item tag '%s' contains no usable materials at '%s'."
                        .formatted(string, path));
            }
            return values;
        }

        Material material = BukkitThing.getMaterial(string);
        if (material == null || material.isAir() || !material.isItem()) {
            throw new RecipeParseException("Unknown material '%s' at '%s'.".formatted(string, path));
        }
        return List.of(material);
    }

    private static ItemStack parseItemStack(String string) throws RecipeParseException {
        ItemStack itemStack;
        try {
            itemStack = Bukkit.getItemFactory().createItemStack(string);
        }
        catch (IllegalArgumentException exception) {
            throw new RecipeParseException("Invalid item '%s': %s".formatted(string, exception.getMessage()));
        }

        if (itemStack == null || itemStack.getType().isAir()) {
            throw new RecipeParseException("Item '%s' is AIR.".formatted(string));
        }
        return itemStack;
    }

    private static boolean looksLikeItemTag(String string) {
        return string.indexOf('{') >= 0 || string.indexOf('[') >= 0;
    }

    private static NamespacedKey parseKey(String string, String defaultNamespace) throws RecipeParseException {
        if (string.indexOf(':') > 0) {
            NamespacedKey key = NamespacedKey.fromString(string);
            if (key == null) {
                throw new RecipeParseException("Invalid recipe key '%s'.".formatted(string));
            }
            return key;
        }

        try {
            return new NamespacedKey(defaultNamespace, string);
        }
        catch (IllegalArgumentException exception) {
            throw new RecipeParseException("Invalid recipe key '%s'.".formatted(string));
        }
    }

    public String getId() {
        return this.id;
    }

    public Action getAction() {
        return this.action;
    }

    public Type getType() {
        return this.type;
    }

    public NamespacedKey getKey() {
        return this.key;
    }

    public Recipe getRecipe() {
        return this.recipe;
    }

    private static class RecipeParseException extends Exception {

        public RecipeParseException(String message) {
            super(message);
        }
    }
}