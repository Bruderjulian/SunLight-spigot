package su.nightexpress.sunlight.module.kits.model;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.bukkit.NightItem;

import java.util.ArrayList;
import java.util.List;

public class KitDefinition {

    private String name;
    private List<String> description;
    private boolean permissionRequired;
    private int cooldown;
    private double cost;
    private int priority;
    private NightItem icon;
    private List<String> commands;
    private KitContent content;

    public KitDefinition(String name,
            List<String> description,
            boolean permissionRequired,
            int cooldown,
            double cost,
            int priority,
            NightItem icon,
            List<String> commands,
            KitContent content) {
        this.setName(name);
        this.setDescription(description);
        this.setPermissionRequired(permissionRequired);
        this.setCooldown(cooldown);
        this.setCost(cost);
        this.setPriority(priority);
        this.setIcon(icon);
        this.setCommands(commands);
        this.setContent(content);
    }

    public static KitDefinition createDefault(String name) {
        List<String> description = new ArrayList<>();
        boolean permissionRequired = true;
        int cooldown = 0;
        double cost = 0D;
        int priority = 0;
        NightItem icon = NightItem.fromType(Material.DIAMOND_SWORD);
        List<String> commands = new ArrayList<>();
        KitContent content = KitContent.empty();

        return new KitDefinition(name, description, permissionRequired, cooldown, cost, priority, icon, commands,
                content);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return this.description;
    }

    public void setDescription(List<String> description) {
        this.description = new ArrayList<>(description);
    }

    public boolean isPermissionRequired() {
        return this.permissionRequired;
    }

    public void setPermissionRequired(boolean permissionRequired) {
        this.permissionRequired = permissionRequired;
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public double getCost() {
        return this.cost;
    }

    public void setCost(double cost) {
        this.cost = Math.max(0, cost);
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public NightItem getIcon() {
        return this.icon.copy();
    }

    public void setIcon(NightItem icon) {
        this.icon = icon.copy();
    }

    public List<String> getCommands() {
        return this.commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = new ArrayList<>(commands);
    }

    public KitContent getContent() {
        return this.content;
    }

    public void setContent(KitContent content) {
        this.content = content;
    }
}
