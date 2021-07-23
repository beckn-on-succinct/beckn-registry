package in.succinct.beckn.registry.db.model;

import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.model.Model;

public interface SubscriberLocation extends Model , GeoLocation {
    @UNIQUE_KEY
    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();

    @UNIQUE_KEY
    public String getProviderLocationId();
    public void setProviderLocationId(String id);

    public String getGps();
    public void setGps(String gps);

    public double getRadius();
    public void setRadius(double radius);

    @COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
    public boolean isActive();
    public void setActive(boolean active);

}
