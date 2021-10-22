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
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import org.apache.lucene.search.Query;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@HAS_DESCRIPTION_FIELD("SUBSCRIBER_ID")
@MENU("Admin")
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
    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    public Long getCityId();
    public void setCityId(Long id);
    public City getCity();

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

    public static final String SUBSCRIBER_STATUS_INITIATED = "INITIATED";
    public static final String SUBSCRIBER_STATUS_UNDER_SUBSCRIPTION = "UNDER_SUBSCRIPTION";
    public static final String SUBSCRIBER_STATUS_SUBSCRIBED = "SUBSCRIBED";
    public static final String SUBSCRIBER_STATUS_INVALID_SSL = "INVALID_SSL";
    public static final String SUBSCRIBER_STATUS_UNSUBSCRIBED = "UNSUBSCRIBED";


    @Enumeration("INITIATED,UNDER_SUBSCRIPTION,SUBSCRIBED,INVALID_SSL,UNSUBSCRIBED")
    @COLUMN_DEF(value = StandardDefault.SOME_VALUE,args = "INITIATED")
    @Index
    public String getStatus();
    public void setStatus(String status);

    public static final String SUBSCRIBER_TYPE_BAP = "bap";
    public static final String SUBSCRIBER_TYPE_BPP = "bpp";
    public static final String SUBSCRIBER_TYPE_LOCAL_REGISTRY = "lreg";
    public static final String SUBSCRIBER_TYPE_BG = "bg";



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

    public static List<Subscriber> lookup(Subscriber criteria, int maxRecords){
        return lookup(criteria,maxRecords,null);
    }
    public static List<Subscriber> lookup(Subscriber criteria, int maxRecords, Expression additionalWhere) {
        return lookup(criteria,maxRecords,additionalWhere,SubscriberWithLocations.BOTH);
    }
    enum SubscriberWithLocations {
        YES,
        NO,
        BOTH
    }
    public static List<Subscriber> lookup(Subscriber criteria, int maxRecords, Expression additionalWhere, SubscriberWithLocations subscriberWithLocations) {
        StringBuilder searchQry = new StringBuilder();
        Expression where = new Expression(criteria.getReflector().getPool(),Conjunction.AND);
        if (additionalWhere != null){
            where.add(additionalWhere);
        }

        if (!criteria.getReflector().isVoid(criteria.getSubscriberId())){
            searchQry.append("SUBSCRIBER_ID:\"").append(criteria.getSubscriberId()).append("\"");
            where.add(new Expression(criteria.getReflector().getPool(), "SUBSCRIBER_ID", Operator.EQ , criteria.getSubscriberId()));
        }
        if (!criteria.getReflector().isVoid(criteria.getCityId())){
            if (searchQry.length() > 0){
                searchQry.append(" AND ");
            }
            searchQry.append(" ( CITY_ID:").append(criteria.getCityId()).append(" OR CITY_ID:NULL ) ");
            Expression cityWhere = new Expression(criteria.getReflector().getPool(),Conjunction.OR);
            cityWhere.add(new Expression(criteria.getReflector().getPool(), "CITY_ID", Operator.EQ , criteria.getCityId()));
            cityWhere.add(new Expression(criteria.getReflector().getPool(),"CITY_ID",Operator.EQ));
            where.add(cityWhere);
        }
        if (!criteria.getReflector().isVoid(criteria.getCountryId())){
            if (searchQry.length() > 0){
                searchQry.append(" AND ");
            }
            where.add(new Expression(criteria.getReflector().getPool(), "COUNTRY_ID", Operator.EQ , criteria.getCountryId()));
            searchQry.append(" COUNTRY_ID:").append(criteria.getCountryId());
        }
        if (!criteria.getReflector().isVoid(criteria.getType())){
            if (searchQry.length() > 0){
                searchQry.append(" AND ");
            }
            searchQry.append(" ( TYPE:").append(criteria.getType()).append( " OR TYPE:NULL ) ");

            Expression typeWhere = new Expression(criteria.getReflector().getPool(),Conjunction.OR);
            typeWhere.add(new Expression(criteria.getReflector().getPool(), "TYPE", Operator.EQ , criteria.getType().toLowerCase()));
            typeWhere.add(new Expression(criteria.getReflector().getPool(),"TYPE",Operator.EQ));
            where.add(typeWhere);
        }
        if (!criteria.getReflector().isVoid(criteria.getDomain())){
            if (searchQry.length() > 0){
                searchQry.append(" AND ");
            }
            searchQry.append(" DOMAIN:\"").append(criteria.getDomain()).append("\"");
            where.add(new Expression(criteria.getReflector().getPool(), "DOMAIN", Operator.EQ , criteria.getDomain()));
        }
        if (!criteria.getReflector().isVoid(criteria.getStatus())){
            if (searchQry.length() > 0){
                searchQry.append(" AND ");
            }
            searchQry.append(" STATUS:\"").append(criteria.getStatus()).append("\"");
            where.add(new Expression(criteria.getReflector().getPool(), "STATUS", Operator.EQ , criteria.getStatus()));
        }
        LuceneIndexer indexer = LuceneIndexer.instance(Subscriber.class);
        Query q = indexer.constructQuery(searchQry.toString());

        List<Long> ids = indexer.findIds(q, Select.MAX_RECORDS_ALL_RECORDS);
        List<Subscriber> records = new ArrayList<>();
        if (!ids.isEmpty()) {
            ModelReflector<Subscriber> ref = ModelReflector.instance(Subscriber.class);
            where.add(Expression.createExpression(ref.getPool(), "ID", Operator.IN, ids.toArray()));
            Select sel = new Select().from(Subscriber.class).where(where);
            switch (subscriberWithLocations){
                case NO:
                    sel.add(" and not exists (select 1 from subscriber_locations where subscriber_id = subscribers.id )");
                    break;
                case YES:
                    sel.add(" and exists (select 1 from subscriber_locations where subscriber_id = subscribers.id )");
                    break;
                default:
                    break;
            }
            sel.orderBy("TYPE DESC , CITY_ID DESC ");
            records = sel.execute(Subscriber.class, maxRecords);
        }
        return records;
    }
}
