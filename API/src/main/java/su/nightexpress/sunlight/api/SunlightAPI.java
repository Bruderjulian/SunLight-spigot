package su.nightexpress.sunlight.api;

import su.nightexpress.sunlight.api.provider.AfkProvider;
import su.nightexpress.sunlight.api.provider.FreezeProvider;
import su.nightexpress.sunlight.api.provider.NickProvider;
import su.nightexpress.sunlight.api.provider.VanishProvider;

import java.util.Optional;

public interface SunlightAPI {

     Optional<? extends AfkProvider> afkProvider();

     Optional<? extends FreezeProvider> freezeProvider();

     Optional<? extends NickProvider> nickProvider();

     Optional<? extends VanishProvider> vanishProvider();
}
