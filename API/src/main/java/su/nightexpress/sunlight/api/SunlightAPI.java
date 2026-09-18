package su.nightexpress.sunlight.api;

import su.nightexpress.sunlight.api.provider.AfkProvider;
import su.nightexpress.sunlight.api.provider.VanishProvider;

import java.util.Optional;

public interface SunlightAPI {

     Optional<? extends AfkProvider> afkProvider();

     Optional<? extends VanishProvider> vanishProvider();
}
