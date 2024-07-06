package in.succinct.beckn.registry.db.model.onboarding;

import java.sql.Date;

import com.venky.core.util.ObjectUtil;
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
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;

import in.succinct.beckn.Subscriber;

import java.util.List;

@HAS_DESCRIPTION_FIELD("SUBSCRIBER_ID")
public interface NetworkRole extends Model {

    static NetworkRole find(String subscriberId) {
        return find(new Subscriber() {
            {
                setSubscriberId(subscriberId);
            }
        });
    }

    static NetworkRole find(Subscriber subscriber) {
        List<NetworkRole> roles = all(subscriber);
        NetworkRole networkRole = null;
        if (roles.isEmpty()) {
            networkRole = Database.getTable(NetworkRole.class).newRecord();
            networkRole.setSubscriberId(subscriber.getSubscriberId());
            if (!ObjectUtil.isVoid(subscriber.getDomain())) {
                NetworkDomain networkDomain = NetworkDomain.find(subscriber.getDomain());
                if (networkDomain.getRawRecord().isNewRecord()) {
                    throw new RuntimeException("Invalid domain" + subscriber.getDomain());
                }
                networkRole.setNetworkDomainId(networkDomain.getId());
            }
            if (ObjectUtil.isVoid(subscriber.getType())) {
                networkRole.setType(subscriber.getType());
            }
        } else {
            networkRole = roles.get(0);
        }
        return networkRole;
    }

    static List<NetworkRole> all(Subscriber subscriber) {
        Select select = new Select().from(NetworkRole.class);
        Expression expression = new Expression(select.getPool(), Conjunction.AND);
        expression.add(new Expression(select.getPool(), "SUBSCRIBER_ID", Operator.EQ, subscriber.getSubscriberId()));

        if (!ObjectUtil.isVoid(subscriber.getType())) {
            expression.add(new Expression(select.getPool(), "TYPE", Operator.EQ, subscriber.getType()));
        }

        if (!ObjectUtil.isVoid(subscriber.getDomain())) {
            NetworkDomain domain = NetworkDomain.find(subscriber.getDomain());
            if (domain.getRawRecord().isNewRecord()) {
                throw new RuntimeException("Invalid domain");
            }
            expression.add(new Expression(select.getPool(), "NETWORK_DOMAIN_ID", Operator.EQ, domain.getId()));
        }

        return select.where(expression).execute();
    }

    @HIDDEN
    @UNIQUE_KEY(value = "K1")
    @Index
    @IS_NULLABLE(false)
    @PARTICIPANT

    public Long getNetworkParticipantId();

    public void setNetworkParticipantId(Long id);

    public NetworkParticipant getNetworkParticipant();

    @Index
    @PARTICIPANT
    public Long getCreatorUserId();

    @UNIQUE_KEY(value = "K1,K2", allowMultipleRecordsWithNull = false)
    @Index
    public Long getNetworkDomainId();

    public void setNetworkDomainId(Long id);

    public NetworkDomain getNetworkDomain();

    @COLUMN_DEF(value = StandardDefault.SOME_VALUE, args = "v0")
    public String getCoreVersion();

    public void setCoreVersion(String coreVersion);

    @COLUMN_DEF(value = StandardDefault.CURRENT_TIMESTAMP)
    public Date getLastInteraction();

    public void setLastInteraction(Date lastInteraction);

    public static final String SUBSCRIBER_TYPE_BAP = "BAP";
    public static final String SUBSCRIBER_TYPE_BPP = "BPP";
    public static final String SUBSCRIBER_TYPE_LOCAL_REGISTRY = "LREG";
    public static final String SUBSCRIBER_TYPE_COUNTRY_REGISTRY = "CREG";
    public static final String SUBSCRIBER_TYPE_ROOT_REGISTRY = "RREG";
    public static final String SUBSCRIBER_TYPE_BG = "BG";
    public static final String SUBSCRIBER_TYPE_UNKNOWN = " ";

    public static final String SUBSCRIBER_ENUM = SUBSCRIBER_TYPE_UNKNOWN + "," + SUBSCRIBER_TYPE_BAP + "," + SUBSCRIBER_TYPE_BPP + ","
            + SUBSCRIBER_TYPE_LOCAL_REGISTRY + "," + SUBSCRIBER_TYPE_COUNTRY_REGISTRY + "," + SUBSCRIBER_TYPE_ROOT_REGISTRY + "," + SUBSCRIBER_TYPE_BG;

    @Enumeration(SUBSCRIBER_ENUM)
    @UNIQUE_KEY(value = "K1,K2", allowMultipleRecordsWithNull = false)
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

    @Enumeration(SUBSCRIBER_STATUS_INITIATED + "," + SUBSCRIBER_STATUS_UNDER_SUBSCRIPTION + "," + SUBSCRIBER_STATUS_SUBSCRIBED + "," + SUBSCRIBER_STATUS_INVALID_SSL + "," + SUBSCRIBER_STATUS_UNSUBSCRIBED)
    @COLUMN_DEF(value = StandardDefault.SOME_VALUE, args = "INITIATED")
    @Index
    public String getStatus();

    public void setStatus(String status);

}
