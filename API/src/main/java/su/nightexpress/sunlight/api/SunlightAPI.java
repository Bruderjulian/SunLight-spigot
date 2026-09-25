package su.nightexpress.sunlight.api;

import su.nightexpress.sunlight.api.provider.AfkProvider;
import su.nightexpress.sunlight.api.provider.FreezeProvider;
import su.nightexpress.sunlight.api.provider.GlowProvider;
import su.nightexpress.sunlight.api.provider.NametagsProvider;
import su.nightexpress.sunlight.api.provider.NickProvider;
import su.nightexpress.sunlight.api.provider.ReportsProvider;
import su.nightexpress.sunlight.api.provider.SocialsProvider;
import su.nightexpress.sunlight.api.provider.VanishProvider;

import java.util.Optional;

public interface SunlightAPI {

     Optional<? extends AfkProvider> afkProvider();

     Optional<? extends FreezeProvider> freezeProvider();

     Optional<? extends GlowProvider> glowProvider();

     Optional<? extends NametagsProvider> nametagsProvider();

     Optional<? extends NickProvider> nickProvider();

     Optional<? extends ReportsProvider> reportsProvider();

     Optional<? extends SocialsProvider> socialsProvider();

     Optional<? extends VanishProvider> vanishProvider();
}
