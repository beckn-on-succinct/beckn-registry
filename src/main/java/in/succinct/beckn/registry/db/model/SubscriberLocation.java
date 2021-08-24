package in.succinct.beckn.registry.db.model;

import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.model.Model;

import java.math.BigDecimal;

public interface SubscriberLocation extends Model , GeoLocation {
    @UNIQUE_KEY
    public Long getSubscriberId();
    public void setSubscriberId(Long id);
    public Subscriber getSubscriber();

    @UNIQUE_KEY
    public String getProviderLocationId();
    public void setProviderLocationId(String id);


    public double getRadius();
    public void setRadius(double radius);

    @COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
    public boolean isActive();
    public void setActive(boolean active);


    @IS_NULLABLE
    public BigDecimal getMinLat();
    public void setMinLat(BigDecimal lat);

    @IS_NULLABLE
    public BigDecimal getMinLng();
    public void setMinLng(BigDecimal lat);

    @IS_NULLABLE
    public BigDecimal getMaxLat();
    public void setMaxLat(BigDecimal lat);

    @IS_NULLABLE
    public BigDecimal getMaxLng();
    public void setMaxLng(BigDecimal lng);


}
