package su.nightexpress.sunlight.config;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionTree {

    private final String name;
    private final String prefix;
    private final Map<String, PermissionTree> branches;
    private final Map<String, Permission> permissions;

    private PermissionTree(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
        this.branches = new HashMap<>();
        this.permissions = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public static PermissionTree root(String name) {
        return new PermissionTree(name, name);
    }

    public PermissionTree detached(String name) {
        return new PermissionTree(name, this.childrenNode(name));
    }

    public PermissionTree branch(String prefix) {
        PermissionTree tree = this.detached(prefix);
        this.merge(tree);
        return tree;
    }

    public void merge(PermissionTree other) {
        this.branches.put(other.name, other);
    }

    public Permission permission(String name) {
        Permission permission = this.children(name);

        this.permissions.put(permission.getName(), permission);
        return permission;
    }

    public Permission getRoot() {
        return new Permission(this.childrenNode("*"));
    }

    public Permission children(String name) {
        return new Permission(this.childrenNode(name));
    }

    public String childrenNode(String name) {
        return this.prefix + "." + name;
    }

    public boolean hasChildAccess(CommandSender sender, String name) {
        return sender.hasPermission(this.childrenNode("*")) || sender.hasPermission(this.childrenNode(name));
    }

    public List<Permission> toList() {
        List<Permission> accumulated = new ArrayList<>();

        accumulated.add(this.getRoot());
        accumulated.addAll(this.permissions.values());

        this.branches.values().forEach(branch -> {
            accumulated.addAll(branch.toList());
        });

        return accumulated;
    }

    public Permission accumulate() {
        Permission root = this.getRoot();

        this.permissions.values().forEach(permission -> {
            permission.addParent(root, true);
        });
        this.branches.values().forEach(branch -> {
            branch.accumulate().addParent(root, true);
        });

        return root;
    }
}
