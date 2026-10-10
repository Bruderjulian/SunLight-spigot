package su.nightexpress.sunlight.api;

import su.nightexpress.sunlight.api.provider.AfkProvider;
import su.nightexpress.sunlight.api.provider.BansProvider;
import su.nightexpress.sunlight.api.provider.FreezeProvider;
import su.nightexpress.sunlight.api.provider.GlowProvider;
import su.nightexpress.sunlight.api.provider.HomesProvider;
import su.nightexpress.sunlight.api.provider.KitsProvider;
import su.nightexpress.sunlight.api.provider.LinksProvider;
import su.nightexpress.sunlight.api.provider.NametagsProvider;
import su.nightexpress.sunlight.api.provider.NickProvider;
import su.nightexpress.sunlight.api.provider.PlaytimeProvider;
import su.nightexpress.sunlight.api.provider.ProfilesProvider;
import su.nightexpress.sunlight.api.provider.ReportsProvider;
import su.nightexpress.sunlight.api.provider.SocialsProvider;
import su.nightexpress.sunlight.api.provider.VanishProvider;
import su.nightexpress.sunlight.api.provider.WarpsProvider;

import java.util.Optional;

public interface SunlightAPI {

     Optional<? extends AfkProvider> afkProvider();

     Optional<? extends FreezeProvider> freezeProvider();

     Optional<? extends GlowProvider> glowProvider();

     Optional<? extends LinksProvider> linksProvider();

     Optional<? extends NametagsProvider> nametagsProvider();

     Optional<? extends NickProvider> nickProvider();

     Optional<? extends PlaytimeProvider> playtimeProvider();

     Optional<? extends ProfilesProvider> profilesProvider();

     Optional<? extends ReportsProvider> reportsProvider();

     Optional<? extends SocialsProvider> socialsProvider();

     Optional<? extends VanishProvider> vanishProvider();

     default Optional<? extends HomesProvider> homesProvider() {
          return Optional.empty();
     }

     default Optional<? extends WarpsProvider> warpsProvider() {
          return Optional.empty();
     }

     default Optional<? extends KitsProvider> kitsProvider() {
          return Optional.empty();
     }

     default Optional<? extends BansProvider> bansProvider() {
          return Optional.empty();
     }
}
