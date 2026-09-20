package su.nightexpress.sunlight;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.ConfigType;
import su.nightexpress.nightcore.util.RankTable;

public class SLConfigTypes {

    public static final ConfigType<RankTable> RANK_TABLE = ConfigType.of(
            RankTable::read,
            FileConfig::set);
}
