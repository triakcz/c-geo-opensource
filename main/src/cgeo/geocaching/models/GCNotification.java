package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogType;

import java.util.HashSet;
import java.util.Set;

public final class GCNotification {

    private final int nid;
    private boolean isEnabled;
    private final CacheType cacheType;
    private final String name;
    private final Set<LogType> logTypes = new HashSet<LogType>();
    private Geopoint coords;
    private double distance;
    private boolean isChecked = false;

    public GCNotification(final int nid, final String name, final CacheType cacheType, final Set<LogType> logTypes, final boolean isEnabled) {
        this.nid = nid;
        this.name = name;
        this.logTypes.addAll(logTypes);
        this.cacheType = cacheType;
        this.isEnabled = isEnabled;
    }

    public String getName() {
        return name;
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public int getNid() {
        return nid;
    }

    public Set<LogType> getLogTypes() {
        return logTypes;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setCoords(final Geopoint coords) {
        this.coords = coords;
    }

    public Geopoint getCoords() {
        return coords;
    }

    public void setDistance(final double distance) {
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(final boolean checked) {
        isChecked = checked;
    }
}
