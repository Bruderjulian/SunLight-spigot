package su.nightexpress.sunlight.module;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ModuleRegistration {

    private final ModuleFactory<?> factory;
    private final Supplier<LoadCondition> condition;

    public ModuleRegistration(ModuleFactory<?> factory, Supplier<LoadCondition> condition) {
        this.factory = factory;
        this.condition = condition;
    }

    public ModuleRegistration(ModuleFactory<?> factory) {
        this(factory, LoadCondition::success);
    }

    public ModuleFactory<?> getFactory() {
        return this.factory;
    }

    public Supplier<LoadCondition> getCondition() {
        return this.condition;
    }
}
