package su.nightexpress.sunlight.module;

@FunctionalInterface
public interface ModuleFactory<T extends Module> {

    T load(ModuleContext context);
}
