package su.nightexpress.sunlight.moduleImpl.nametags.model;

/**
 * Where the purchase actually happens for a paid tag.
 */
public enum PriceMode {

    /**
     * SunLight withdraws the money itself and tracks the grant internally.
     * Works without any economy plugin installed.
     */
    INTERNAL,

    /**
     * The grant is owned by an external plugin and SunLight only stores a marker.
     */
    EXTERNAL

}
