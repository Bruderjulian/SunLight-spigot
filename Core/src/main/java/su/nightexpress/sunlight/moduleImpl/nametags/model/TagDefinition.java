package su.nightexpress.sunlight.moduleImpl.nametags.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.sunlight.utils.Utils;

import java.util.List;

/**
 * A per-player selectable nameplate. One tag contributes an additional prefix, suffix and
 * optional name color, which is combined with the rank and team parts.
 * <p>
 * Mutable by design: the admin GUI edits tags in place and persists them to
 * {@code settings.yml}. Setters normalise and clamp values so the GUI can never
 * write an invalid definition.
 */
public class TagDefinition implements Writeable {

    private String id;
    private String display;
    private List<String> description;
    private String prefix;
    private String suffix;
    private String color;
    private String permission;
    private TagAccessMode accessMode;
    private PriceMode priceMode;
    private double price;
    private SubscriptionPeriod subscriptionPeriod;
    private String iconMaterial;
    private boolean glow;
    private int sortPriority;

    public TagDefinition(@NotNull String id) {
        this(id,
            id,
            List.of(),
            "",
            "",
            "",
            "",
            TagAccessMode.FREE,
            PriceMode.INTERNAL,
            0D,
            SubscriptionPeriod.MONTHLY,
            "NAME_TAG",
            false,
            0
        );
    }

    public TagDefinition(@NotNull String id,
            @NotNull String display,
            @NotNull List<String> description,
            @NotNull String prefix,
            @NotNull String suffix,
            @NotNull String color,
            @NotNull String permission,
            @NotNull TagAccessMode accessMode,
            @NotNull PriceMode priceMode,
            double price,
            @NotNull SubscriptionPeriod subscriptionPeriod,
            @NotNull String iconMaterial,
            boolean glow,
            int sortPriority
    ) {
        this.id = Utils.lowercase(id);
        this.display = display;
        this.description = List.copyOf(description);
        this.prefix = prefix;
        this.suffix = suffix;
        this.color = color;
        this.permission = permission;
        this.accessMode = accessMode;
        this.priceMode = priceMode;
        this.price = Math.max(0D, price);
        this.subscriptionPeriod = subscriptionPeriod;
        this.iconMaterial = iconMaterial;
        this.glow = glow;
        this.sortPriority = sortPriority;
    }

    public static TagDefinition read(FileConfig config, String path) {
        String id = path.substring(path.lastIndexOf('.') + 1);
        TagDefinition tag = new TagDefinition(id);
        tag.setDisplay(config.getString(path + ".Display", id));
        tag.setDescription(config.getStringList(path + ".Description"));
        tag.setPrefix(config.getString(path + ".Prefix", ""));
        tag.setSuffix(config.getString(path + ".Suffix", ""));
        tag.setColor(config.getString(path + ".Color", ""));
        tag.setPermission(config.getString(path + ".Permission", ""));
        tag.setAccessMode(Utils.enumValueOf(config.getString(path + ".Access-Mode", "FREE"), TagAccessMode.class));
        tag.setPriceMode(Utils.enumValueOf(config.getString(path + ".Price-Mode", "INTERNAL"), PriceMode.class));
        tag.setPrice(config.getDouble(path + ".Price"));
        tag.setSubscriptionPeriod(Utils.enumValueOf(config.getString(path + ".Subscription-Period", "MONTHLY"), SubscriptionPeriod.class));
        tag.setIconMaterial(config.getString(path + ".Icon", "NAME_TAG"));
        tag.setGlow(config.getBoolean(path + ".Glow", false));
        tag.setSortPriority(config.getInt(path + ".Sort-Priority"));
        return tag;
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Display", this.display);
        config.set(path + ".Description", this.description);
        config.set(path + ".Prefix", this.prefix);
        config.set(path + ".Suffix", this.suffix);
        config.set(path + ".Color", this.color);
        config.set(path + ".Permission", this.permission);
        config.set(path + ".Access-Mode", this.accessMode.name());
        config.set(path + ".Price-Mode", this.priceMode.name());
        config.set(path + ".Price", this.price);
        config.set(path + ".Subscription-Period", this.subscriptionPeriod.name());
        config.set(path + ".Icon", this.iconMaterial);
        config.set(path + ".Glow", this.glow);
        config.set(path + ".Sort-Priority", this.sortPriority);
    }

    // -----------------------------------------------------
    // Getters / setters
    // -----------------------------------------------------

    public @NotNull String getId() {
        return this.id;
    }

    public void setId(@NotNull String id) {
        this.id = Utils.lowercase(id);
    }

    public @NotNull String getDisplay() {
        return this.display;
    }

    public void setDisplay(@NotNull String display) {
        this.display = display.isBlank() ? this.id : display;
    }

    public @NotNull List<String> getDescription() {
        return this.description;
    }

    public void setDescription(@NotNull List<String> description) {
        this.description = List.copyOf(description);
    }

    public @NotNull String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(@NotNull String prefix) {
        this.prefix = prefix;
    }

    public @NotNull String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(@NotNull String suffix) {
        this.suffix = suffix;
    }

    public @NotNull String getColor() {
        return this.color;
    }

    public void setColor(@NotNull String color) {
        this.color = color;
    }

    public boolean hasColor() {
        return !this.color.isBlank();
    }

    public @NotNull String getPermission() {
        return this.permission;
    }

    public void setPermission(@NotNull String permission) {
        this.permission = permission;
    }

    public @NotNull TagAccessMode getAccessMode() {
        return this.accessMode;
    }

    public void setAccessMode(@Nullable TagAccessMode accessMode) {
        if (accessMode != null) this.accessMode = accessMode;
    }

    public @NotNull PriceMode getPriceMode() {
        return this.priceMode;
    }

    public void setPriceMode(@Nullable PriceMode priceMode) {
        if (priceMode != null) this.priceMode = priceMode;
    }

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = Math.max(0D, price);
    }

    public @NotNull SubscriptionPeriod getSubscriptionPeriod() {
        return this.subscriptionPeriod;
    }

    public void setSubscriptionPeriod(@Nullable SubscriptionPeriod period) {
        if (period != null) this.subscriptionPeriod = period;
    }

    public @NotNull String getIconMaterial() {
        return this.iconMaterial;
    }

    public void setIconMaterial(@NotNull String iconMaterial) {
        this.iconMaterial = iconMaterial.isBlank() ? "NAME_TAG" : iconMaterial;
    }

    /** Whether this tag is allowed to carry the player's glow color. */
    public boolean isGlow() {
        return this.glow;
    }

    public void setGlow(boolean glow) {
        this.glow = glow;
    }

    public int getSortPriority() {
        return this.sortPriority;
    }

    public void setSortPriority(int sortPriority) {
        this.sortPriority = sortPriority;
    }

    // -----------------------------------------------------
    // Derived
    // -----------------------------------------------------

    /** Whether this tag costs money to obtain. */
    public boolean hasPrice() {
        return this.price > 0D;
    }

    public boolean isFree() {
        return this.accessMode == TagAccessMode.FREE;
    }

    public boolean isPurchasable() {
        return this.accessMode == TagAccessMode.PURCHASE || this.accessMode == TagAccessMode.SUBSCRIPTION;
    }

    public boolean isSubscription() {
        return this.accessMode == TagAccessMode.SUBSCRIPTION;
    }

    /** Whether a valid grant is required to use this tag. */
    public boolean requiresGrant() {
        return this.accessMode == TagAccessMode.PURCHASE || this.accessMode == TagAccessMode.SUBSCRIPTION;
    }

    /**
     * Checks the optional extra permission. The derived {@code nametags.tag.<id>} node is
     * always checked separately by the access service.
     */
    public boolean hasExtraPermission() {
        return !this.permission.isBlank();
    }
}
