package su.nightexpress.sunlight.module;

import java.nio.file.Path;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface ModuleContextProvider {

    
    ModuleContext createModuleContext( String id,  Path path,  ModuleDefinition definition);
}
