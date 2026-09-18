package su.nightexpress.sunlight.module;

import java.nio.file.Path;

@FunctionalInterface
public interface ModuleContextProvider {

    ModuleContext createModuleContext(String id, Path path, ModuleDefinition definition);
}
