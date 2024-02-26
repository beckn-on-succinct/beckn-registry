package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.geo.GeoLocation;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;
import in.succinct.beckn.registry.db.model.City;
import in.succinct.beckn.registry.db.model.Country;

import java.math.BigDecimal;

public interface OperatingRegion extends Model, GeoLocation {
    @IS_NULLABLE(false)
    @UNIQUE_KEY
    public long getNetworkRoleId();
    public void setNetworkRoleId(long id);
    public NetworkRole getNetworkRole();

    @IS_NULLABLE(false)
    @UNIQUE_KEY
    public long getCountryId();
    public void setCountryId(long id);
    public Country getCountry();

    @IS_NULLABLE
    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    public Long getCityId();
    public void setCityId(Long id);
    public City getCity();

    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)

    public BigDecimal getLat();

    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    public BigDecimal getLng();

    @IS_NULLABLE
    public Double getRadius();
    public void setRadius(Double radius);

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


    @COLUMN_DEF(StandardDefault.BOOLEAN_TRUE)
    public Boolean isActive();
    public void setActive(Boolean active);

    @Index
    @PARTICIPANT
    public Long getCreatorUserId();
}
