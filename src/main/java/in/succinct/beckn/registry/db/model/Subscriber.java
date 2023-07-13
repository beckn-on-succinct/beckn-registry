package in.succinct.beckn.registry.db.model;

import com.venky.core.date.DateUtils;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.geo.GeoCoordinate;
import com.venky.geo.GeoLocation;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.FormatHelper.KeyCase;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.util.BoundingBox;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.Location;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.onboarding.NetworkDomain;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.db.model.onboarding.OperatingRegion;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantKey;
import org.apache.lucene.search.Query;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@IS_VIRTUAL
public interface Subscriber extends Model , GeoLocation {

    public String getSubscriberId();

    public void setSubscriberId(String id);

    public String getSubscriberUrl();

    public void setSubscriberUrl(String url);

    public String getCountry();

    public void setCountry(String country);

    public String getCity();

    public void setCity(String city);

    public String getDomain();

    public void setDomain(String domain);


    @COLUMN_NAME("PUB_KEY_ID")
    public String getUniqueKeyId();
    public void setUniqueKeyId(String keyId);

    public String getPubKeyId();
    public void setPubKeyId(String keyId);



    public String getSigningPublicKey();

    public void setSigningPublicKey(String key);

    public String getEncrPublicKey();

    public void setEncrPublicKey(String key);

    public String getValidFrom();

    public void setValidFrom(String from);

    public String getValidUntil();

    public void setValidUntil(String until);

    public String getStatus();

    public void setStatus(String status);

    public String getType();

    public void setType(String type);

    public Double getRadius();
    public void setRadius(Double radius);

    public String getCreated();
    public void setCreated(String created);

    public void setUpdated(String updated);
    public String getUpdated();

    public static JSONArray toBeckn(List<Subscriber> records, String format, String version) {
        if (!ObjectUtil.isVoid(format) ){
            if (!ObjectUtil.equals("PEM",format.toUpperCase())){
                throw new RuntimeException("Only allowed value to be passed is PEM");
            }
            for (Subscriber s : records){
                s.setSigningPublicKey(Request.getPemSigningKey(s.getSigningPublicKey()));
                s.setEncrPublicKey(Request.getPemEncryptionKey(s.getEncrPublicKey()));
            }
        }


        List<String> fields = Arrays.asList("UNIQUE_KEY_ID", "PUB_KEY_ID","SUBSCRIBER_ID","SUBSCRIBER_URL","TYPE","DOMAIN",
                "CITY","COUNTRY","SIGNING_PUBLIC_KEY","ENCR_PUBLIC_KEY","VALID_FROM","VALID_UNTIL","STATUS","CREATED","UPDATED");

        FormatHelper<JSONObject> outHelper = FormatHelper.instance(MimeType.APPLICATION_JSON,StringUtil.pluralize(Subscriber.class.getSimpleName()),true);
        try {
            ModelIOFactory.getWriter(Subscriber.class,outHelper.getFormatClass()).write(records,outHelper.getRoot(),fields);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        outHelper.change_key_case(KeyCase.SNAKE);
        JSONArray out = new JSONArray();
        out.addAll(outHelper.getArrayElements("subscribers"));
        if (ObjectUtil.equals(version,"v1")){
            for (Object o : out){
                JSONObject s = (JSONObject) o;
                in.succinct.beckn.Subscriber bcSubscriber = new in.succinct.beckn.Subscriber(s);
                if (!ObjectUtil.isVoid(bcSubscriber.getCity())){
                    bcSubscriber.setLocation(new Location());
                    bcSubscriber.getLocation().setCity(new in.succinct.beckn.City());
                    bcSubscriber.getLocation().getCity().setCode(bcSubscriber.getCity());
                    bcSubscriber.setCity(null);
                }
            }
        }
        return out;
    }
    public static List<Subscriber> lookup(Subscriber criteria, int maxRecords) {
        return lookup(criteria, maxRecords, null);
    }


    public static List<OperatingRegion> findSubscribedRegions(Subscriber criteria, List<Long> networkRoleIds){
        List<OperatingRegion> regions = new ArrayList<>();
        if (ObjectUtil.isVoid(criteria.getCity()) && ObjectUtil.isVoid(criteria.getCountry()) &&
                ( ObjectUtil.isVoid(criteria.getLat()) || ObjectUtil.isVoid(criteria.getLng()) )){
            return regions;
        }
        //Check Reqion Queries.
        ModelReflector<OperatingRegion> ref = ModelReflector.instance(OperatingRegion.class);
        Expression where = new Expression(ref.getPool(), Conjunction.AND);
        if (!ref.isVoid(criteria.getCity())) {
            City city = City.findByCode(criteria.getCity());
            if (city != null) {
                Expression cityWhere = new Expression(ref.getPool(), Conjunction.OR);
                cityWhere.add(new Expression(ref.getPool(), "CITY_ID", Operator.EQ, city.getId()));
                cityWhere.add(new Expression(ref.getPool(), "CITY_ID", Operator.EQ));
                where.add(cityWhere);
            }else {
                return null;
            }

        }
        if (!ref.isVoid(criteria.getCountry())) {
            Country country = Country.findByISO(criteria.getCountry());
            if (country != null) {
                where.add(new Expression(ref.getPool(), "COUNTRY_ID", Operator.EQ, country.getId()));
            }else {
                return null;
            }
        }
        if (!ref.isVoid(criteria.getLat()) && !ref.isVoid(criteria.getLng())){
            Expression locationWhere = new Expression(ref.getPool(),Conjunction.OR);
            Expression gridWhere = new Expression(ref.getPool(),Conjunction.AND);
            gridWhere.add(new Expression(ref.getPool(),"MIN_LAT", Operator.LE, criteria.getLat()));
            gridWhere.add(new Expression(ref.getPool(),"MAX_LAT", Operator.GE, criteria.getLat()));
            gridWhere.add(new Expression(ref.getPool(),"MIN_LNG", Operator.LE, criteria.getLng()));
            gridWhere.add(new Expression(ref.getPool(),"MAX_LNG", Operator.GE, criteria.getLng()));
            locationWhere.add(gridWhere);
            locationWhere.add(new Expression(ref.getPool(),"LAT", Operator.EQ));
            locationWhere.add(new Expression(ref.getPool(),"LNG", Operator.EQ));
            if (!ref.isVoid(criteria.getRadius())){
                Expression boundWhere = new BoundingBox(new GeoCoordinate(criteria),1.0, criteria.getRadius()).getWhereClause(OperatingRegion.class);
                locationWhere.add(boundWhere);
            }
            where.add(locationWhere);
        }
        if (networkRoleIds != null) {
            where.add(new Expression(ref.getPool(), "NETWORK_ROLE_ID", Operator.IN, networkRoleIds.toArray()));
        }

        Select sel = new Select("MAX(ID) AS ID","NETWORK_ROLE_ID").from(OperatingRegion.class).where(where).groupBy("NETWORK_ROLE_ID");
        return sel.execute(OperatingRegion.class);
    }
    public static List<Subscriber> lookup(Subscriber criteria, int maxRecords, Expression additionalWhere) {
        ParticipantKey key = ObjectUtil.isVoid(criteria.getPubKeyId())? null : ParticipantKey.find(criteria.getPubKeyId());
        if (key != null && key.getRawRecord().isNewRecord()){
            //invalid key being looked up.
            return new ArrayList<>();
        }


        ModelReflector<NetworkRole> ref = ModelReflector.instance(NetworkRole.class);

        StringBuilder searchQry = new StringBuilder();
        Expression where = new Expression(ref.getPool(), Conjunction.AND);
        if (additionalWhere != null) {
            where.add(additionalWhere);
        }

        if (!ref.isVoid(criteria.getSubscriberId())) {
            searchQry.append("SUBSCRIBER_ID:\"").append(criteria.getSubscriberId()).append("\"");
            where.add(new Expression(ref.getPool(), "SUBSCRIBER_ID", Operator.EQ, criteria.getSubscriberId()));
        }
        if (key != null && !key.getReflector().isVoid(key.getNetworkParticipantId())){
            if (searchQry.length() > 0) {
                searchQry.append(" AND ");
            }
            searchQry.append(" NETWORK_PARTICIPANT_ID:\"").append(key.getNetworkParticipantId()).append("\"");
            where.add(new Expression(ref.getPool(), "NETWORK_PARTICIPANT_ID", Operator.EQ, key.getNetworkParticipantId()));
        }

        if (!ref.isVoid(criteria.getType())) {
            if (searchQry.length() > 0) {
                searchQry.append(" AND ");
            }
            searchQry.append(" ( TYPE:").append(criteria.getType()).append(" OR TYPE:NULL ) ");

            Expression typeWhere = new Expression(ref.getPool(), Conjunction.OR);
            typeWhere.add(new Expression(ref.getPool(), "TYPE", Operator.EQ, criteria.getType()));
            typeWhere.add(new Expression(ref.getPool(), "TYPE", Operator.EQ));
            where.add(typeWhere);
        }
        if (!ref.isVoid(criteria.getDomain())) {
            NetworkDomain domain = NetworkDomain.find(criteria.getDomain());
            if (searchQry.length() > 0) {
                searchQry.append(" AND ");
            }
            searchQry.append(" ( NETWORK_DOMAIN_ID:\"").append(domain.getId()).append("\" OR NETWORK_DOMAIN_ID:NULL )");
            Expression domainWhere = new Expression(ref.getPool(), Conjunction.OR);
            domainWhere.add(new Expression(ref.getPool(), "NETWORK_DOMAIN_ID", Operator.EQ, domain.getId()));
            domainWhere.add(new Expression(ref.getPool(), "NETWORK_DOMAIN_ID", Operator.EQ));
            where.add(domainWhere);
        }
        if (!ref.isVoid(criteria.getStatus())) {
            if (searchQry.length() > 0) {
                searchQry.append(" AND ");
            }
            searchQry.append(" STATUS:\"").append(criteria.getStatus()).append("\"");
            where.add(new Expression(ref.getPool(), "STATUS", Operator.EQ, criteria.getStatus()));
        }
        boolean regionPassed = true;
        if (ObjectUtil.isVoid(criteria.getCity()) && ObjectUtil.isVoid(criteria.getCountry()) &&
                ( ObjectUtil.isVoid(criteria.getLat()) || ObjectUtil.isVoid(criteria.getLng()) )){
            regionPassed = false;
        }

        List<Subscriber> subscribers = new ArrayList<>();
        List<Long> networkRoleIds = null;
        if (searchQry.length() > 0) {
            LuceneIndexer indexer = LuceneIndexer.instance(NetworkRole.class);
            Query q = indexer.constructQuery(searchQry.toString());

            networkRoleIds = indexer.findIds(q, Select.MAX_RECORDS_ALL_RECORDS);
            where.add(Expression.createExpression(ModelReflector.instance(NetworkRole.class).getPool(), "ID", Operator.IN, networkRoleIds.toArray()));
        }

        Select okSelectNetworkRole = new Select().from(NetworkRole.class).where(where);
        if (regionPassed) {
            okSelectNetworkRole.add(" and not exists ( select 1 from operating_regions where network_role_id = network_roles.id) ");
        }
        List<NetworkRole> okroles = okSelectNetworkRole.execute();

        for (NetworkRole role : okroles) {
            Subscriber subscriber = getSubscriber(key,role,null);
            if (subscriber != null) {
                subscribers.add(subscriber);
            }
        }

        if (regionPassed){
            List<OperatingRegion> subscribedRegions = findSubscribedRegions(criteria,networkRoleIds);
            if (subscribedRegions == null){
                subscribers.clear();
            }else {
                Set<Long> finalNetworkRoleIds = subscribedRegions.stream().map(OperatingRegion::getNetworkRoleId).collect(Collectors.toSet());

                List<NetworkRole> regionMatchingSubscriptions = new Select().from(NetworkRole.class).
                        where(new Expression(ModelReflector.instance(NetworkRole.class).getPool(), "ID", Operator.IN,
                                finalNetworkRoleIds.toArray())).execute();

                for (NetworkRole networkRole : regionMatchingSubscriptions) {
                    Subscriber subscriber = getSubscriber(key, networkRole, null);
                    if (subscriber != null) {
                        subscribers.add(subscriber);
                    }
                }
            }
        }


        return subscribers;
    }
    static Subscriber getSubscriber( ParticipantKey criteriaKey , NetworkRole networkRole, OperatingRegion region) {
        ParticipantKey key = null;

        List<ParticipantKey> keys ;
        if (criteriaKey != null){
            keys = new ArrayList<>();
            keys.add(criteriaKey);
        }else {
            keys = networkRole.getNetworkParticipant().getParticipantKeys();
            long now = System.currentTimeMillis();
            keys.removeIf(k -> !k.isVerified() || k.getValidUntil().getTime() < now || k.getValidFrom().getTime() > now);// Remove expired keys and keys not yet become valid.
            keys.sort((k1, k2) -> (int) DateUtils.compareToMillis(k2.getValidFrom(), k1.getValidFrom()));
        }
        if (keys.isEmpty()){
            return null;
        }else {
            key = keys.get(0);
        }
        Subscriber subscriber = Database.getTable(Subscriber.class).newRecord();
        subscriber.setSubscriberId(networkRole.getSubscriberId());
        subscriber.setSubscriberUrl(networkRole.getUrl());
        subscriber.setStatus(networkRole.getStatus());
        NetworkDomain networkDomain = networkRole.getNetworkDomain();
        if ( networkDomain != null ) {
            subscriber.setDomain(networkDomain.getName());
        }
        subscriber.setType(networkRole.getType());
        subscriber.setSigningPublicKey(key.getSigningPublicKey());
        subscriber.setEncrPublicKey(key.getEncrPublicKey());
        subscriber.setPubKeyId(key.getKeyId());
        subscriber.setValidFrom(BecknObject.TIMESTAMP_FORMAT.format(key.getValidFrom())) ;
        subscriber.setValidUntil(BecknObject.TIMESTAMP_FORMAT.format(key.getValidUntil()));
        if (region != null ) {
            City city =  region.getCity();
            if (city != null){
                subscriber.setCity(region.getCity().getName());
            }
            Country country = region.getCountry();
            if (country != null){
                subscriber.setCountry(region.getCountry().getName());
            }
            if (region.getLat() != null && region.getLng() != null && region.getRadius() != null){
                subscriber.setLat(region.getLat());
                subscriber.setLng(region.getLng());
                subscriber.setRadius(region.getRadius());
            }
        }
        subscriber.setCreated(BecknObject.TIMESTAMP_FORMAT.format(networkRole.getCreatedAt()));
        subscriber.setUpdated(BecknObject.TIMESTAMP_FORMAT.format(networkRole.getUpdatedAt()));
        return subscriber;
    }

    public void subscribe();
}
