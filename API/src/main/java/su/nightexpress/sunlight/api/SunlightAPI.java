package su.nightexpress.sunlight.api;

import su.nightexpress.sunlight.api.provider.AfkProvider;
import su.nightexpress.sunlight.api.provider.BackLocationProvider;
import su.nightexpress.sunlight.api.provider.BansProvider;
import su.nightexpress.sunlight.api.provider.ChatProvider;
import su.nightexpress.sunlight.api.provider.EssentialProvider;
import su.nightexpress.sunlight.api.provider.FreezeProvider;
import su.nightexpress.sunlight.api.provider.GlowProvider;
import su.nightexpress.sunlight.api.provider.GreetingsProvider;
import su.nightexpress.sunlight.api.provider.HomesProvider;
import su.nightexpress.sunlight.api.provider.InventoriesProvider;
import su.nightexpress.sunlight.api.provider.KitsProvider;
import su.nightexpress.sunlight.api.provider.LinksProvider;
import su.nightexpress.sunlight.api.provider.NametagsProvider;
import su.nightexpress.sunlight.api.provider.NickProvider;
import su.nightexpress.sunlight.api.provider.PhantomsProvider;
import su.nightexpress.sunlight.api.provider.PlayerWarpsProvider;
import su.nightexpress.sunlight.api.provider.PlaytimeProvider;
import su.nightexpress.sunlight.api.provider.ProfilesProvider;
import su.nightexpress.sunlight.api.provider.PtpProvider;
import su.nightexpress.sunlight.api.provider.ReportsProvider;
import su.nightexpress.sunlight.api.provider.RtpProvider;
import su.nightexpress.sunlight.api.provider.SocialsProvider;
import su.nightexpress.sunlight.api.provider.SpawnsProvider;
import su.nightexpress.sunlight.api.provider.TextsProvider;
import su.nightexpress.sunlight.api.provider.VanishProvider;
import su.nightexpress.sunlight.api.provider.WarpsProvider;
import su.nightexpress.sunlight.api.provider.WarmupsProvider;

import java.util.Optional;

public interface SunlightAPI {

     default Optional<? extends AfkProvider> afkProvider() {
          return Optional.empty();
     }

     default Optional<? extends FreezeProvider> freezeProvider() {
          return Optional.empty();
     }

     default Optional<? extends GlowProvider> glowProvider() {
          return Optional.empty();
     }

     default Optional<? extends LinksProvider> linksProvider() {
          return Optional.empty();
     }

     default Optional<? extends NametagsProvider> nametagsProvider() {
          return Optional.empty();
     }

     default Optional<? extends NickProvider> nickProvider() {
          return Optional.empty();
     }

     default Optional<? extends PlaytimeProvider> playtimeProvider() {
          return Optional.empty();
     }

     default Optional<? extends ProfilesProvider> profilesProvider() {
          return Optional.empty();
     }

     default Optional<? extends ReportsProvider> reportsProvider() {
          return Optional.empty();
     }

     default Optional<? extends SocialsProvider> socialsProvider() {
          return Optional.empty();
     }

     default Optional<? extends VanishProvider> vanishProvider() {
          return Optional.empty();
     }

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

     default Optional<? extends SpawnsProvider> spawnsProvider() {
          return Optional.empty();
     }

     default Optional<? extends PtpProvider> ptpProvider() {
          return Optional.empty();
     }

     default Optional<? extends BackLocationProvider> backLocationProvider() {
          return Optional.empty();
     }

     default Optional<? extends PlayerWarpsProvider> playerWarpsProvider() {
          return Optional.empty();
     }

     default Optional<? extends WarmupsProvider> warmupsProvider() {
          return Optional.empty();
     }

     default Optional<? extends RtpProvider> rtpProvider() {
          return Optional.empty();
     }

     default Optional<? extends TextsProvider> textsProvider() {
          return Optional.empty();
     }

     default Optional<? extends ChatProvider> chatProvider() {
          return Optional.empty();
     }

     default Optional<? extends GreetingsProvider> greetingsProvider() {
          return Optional.empty();
     }

     default Optional<? extends EssentialProvider> essentialProvider() {
          return Optional.empty();
     }

     default Optional<? extends InventoriesProvider> inventoriesProvider() {
          return Optional.empty();
     }

     default Optional<? extends PhantomsProvider> phantomsProvider() {
          return Optional.empty();
     }
}
