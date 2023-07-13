package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.indexing.Index;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.validations.Enumeration;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.model.Model;

import java.util.List;

@HAS_DESCRIPTION_FIELD("SUBSCRIBER_ID")
public interface NetworkRole extends Model {
    static NetworkRole find(String subscriberId) {

        NetworkRole role = Database.getTable(NetworkRole.class).newRecord();
        role.setSubscriberId(subscriberId);
        role = Database.getTable(NetworkRole.class).getRefreshed(role);
        return role;
    }

    @HIDDEN
    @UNIQUE_KEY
    @Index
    @IS_NULLABLE(false)
    @PARTICIPANT

    public Long getNetworkParticipantId();
    public void setNetworkParticipantId(Long id);
    public NetworkParticipant getNetworkParticipant();

    @UNIQUE_KEY
    @Index
    public Long getNetworkDomainId();
    public void setNetworkDomainId(Long id);
    public NetworkDomain getNetworkDomain();

    @COLUMN_DEF(value = StandardDefault.SOME_VALUE,args = "v0")
    public String getCoreVersion();
    public void setCoreVersion(String coreVersion);


    public static final String SUBSCRIBER_TYPE_BAP = "BAP";
    public static final String SUBSCRIBER_TYPE_BPP = "BPP";
    public static final String SUBSCRIBER_TYPE_LOCAL_REGISTRY = "LREG";
    public static final String SUBSCRIBER_TYPE_COUNTRY_REGISTRY = "CREG";
    public static final String SUBSCRIBER_TYPE_ROOT_REGISTRY = "RREG";
    public static final String SUBSCRIBER_TYPE_BG = "BG";
    public static final String SUBSCRIBER_TYPE_UNKNOWN = " ";


    public static final String SUBSCRIBER_ENUM = SUBSCRIBER_TYPE_UNKNOWN + "," +SUBSCRIBER_TYPE_BAP+"," +SUBSCRIBER_TYPE_BPP + ","  +
            SUBSCRIBER_TYPE_LOCAL_REGISTRY + "," + SUBSCRIBER_TYPE_COUNTRY_REGISTRY + "," + SUBSCRIBER_TYPE_ROOT_REGISTRY + "," + SUBSCRIBER_TYPE_BG;

    @Enumeration( SUBSCRIBER_ENUM )
    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    @Index
    @IS_NULLABLE
    public String getType();
    public void setType(String type);


    @UNIQUE_KEY("K2")
    @Index
    public String getSubscriberId();
    public void setSubscriberId(String subscriberId);

    public String getUrl();
    public void setUrl(String url);

    public List<OperatingRegion> getOperatingRegions();


    public static final String SUBSCRIBER_STATUS_INITIATED = "INITIATED";
    public static final String SUBSCRIBER_STATUS_UNDER_SUBSCRIPTION = "UNDER_SUBSCRIPTION";
    public static final String SUBSCRIBER_STATUS_SUBSCRIBED = "SUBSCRIBED";
    public static final String SUBSCRIBER_STATUS_INVALID_SSL = "INVALID_SSL";
    public static final String SUBSCRIBER_STATUS_UNSUBSCRIBED = "UNSUBSCRIBED";


    @Enumeration(SUBSCRIBER_STATUS_INITIATED + "," +SUBSCRIBER_STATUS_UNDER_SUBSCRIPTION+ "," +SUBSCRIBER_STATUS_SUBSCRIBED + "," +SUBSCRIBER_STATUS_INVALID_SSL + "," +SUBSCRIBER_STATUS_UNSUBSCRIBED )
    @COLUMN_DEF(value = StandardDefault.SOME_VALUE,args = "INITIATED")
    @Index
    public String getStatus();
    public void setStatus(String status);



}
