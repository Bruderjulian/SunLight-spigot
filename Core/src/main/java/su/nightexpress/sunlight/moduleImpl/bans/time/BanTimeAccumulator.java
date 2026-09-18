package su.nightexpress.sunlight.moduleImpl.bans.time;

@FunctionalInterface
public interface BanTimeAccumulator {

    long accumulate(long quantity);
}
