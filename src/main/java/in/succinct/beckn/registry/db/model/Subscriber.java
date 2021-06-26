package in.succinct.beckn.registry.db.model;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;

import java.sql.Timestamp;

public interface Subscriber extends Model {
    @UNIQUE_KEY
    @Index
    public String getSubscriberId();
    public void setSubscriberId(String id);

    public String getSubscriberUrl();
    public void setSubscriberUrl(String url);

    @Index
    public long getCountryId();
    public void setCountryId(long id);
    public Country getCountry();

    @Index
    @IS_NULLABLE
    public Long getCityId();
    public void setCityId(Long id);
    public City getCity();

    @Enumeration("local-retail")
    @Index
    public String getDomain();
    public void setDomain(String domain);

    @COLUMN_SIZE(4096)
    public String getSigningPublicKey();
    public void setSigningPublicKey(String key);

    @COLUMN_SIZE(4096)
    public String getEncrPublicKey();
    public void setEncrPublicKey(String key);

    public Timestamp getValidFrom();
    public void setValidFrom(Timestamp from);

    public Timestamp getValidUntil();
    public void setValidUntil(Timestamp until);

    @Enumeration("INITIATED,UNDER_SUBSCRIPTION,SUBSCRIBED,INVALID_SSL,UNSUBSCRIBED")
    @COLUMN_DEF(value = StandardDefault.SOME_VALUE,args = "INITIATED")
    @Index
    public String getStatus();
    public void setStatus(String status);

    @UNIQUE_KEY
    @Enumeration("bap,bpp,bg,lreg,creg,rreg")
    @Index
    public String getType();
    public void setType(String type);

    @COLUMN_NAME("CREATED_AT")
    public Timestamp getCreated();
    public void setCreated(Timestamp created);

    @COLUMN_NAME("UPDATED_AT")
    public Timestamp getUpdated();
    public void setUpdated(Timestamp updated);


}
