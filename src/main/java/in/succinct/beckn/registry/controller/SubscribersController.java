package in.succinct.beckn.registry.controller;

import com.venky.core.date.DateUtils;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.Controller;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.beckn.Request;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.Subscriber.Domains;
import in.succinct.beckn.Subscribers;
import in.succinct.beckn.registry.db.model.onboarding.DocumentPurpose;
import in.succinct.beckn.registry.db.model.onboarding.NetworkDomain;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.db.model.onboarding.OperatingRegion;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantKey;
import in.succinct.beckn.registry.db.model.onboarding.SubmittedDocument;
import in.succinct.beckn.registry.db.model.onboarding.VerifiableDocument;
import in.succinct.beckn.registry.extensions.AfterSaveParticipantKey.OnSubscribe;
import in.succinct.json.JSONAwareWrapper;
import org.apache.lucene.search.Query;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class SubscribersController extends Controller {
    public SubscribersController(Path path) {
        super(path);
    }

    @RequireLogin(false)
    public <T> View disable() throws Exception {

        String payload = StringUtil.read(getPath().getInputStream());
        Request request = new Request(payload);
        Map<String,String> params = request.extractAuthorizationParams("X-Gateway-Authorization",getPath().getHeaders());
        if (params.isEmpty()){
            throw new RuntimeException("Signature Verification failed");
        }

        String pub_key_id = params.get("pub_key_id");
        String subscriber_id = params.get("subscriber_id");
        ParticipantKey signedWithKey = ParticipantKey.find(pub_key_id);
        if (!signedWithKey.isVerified()){
            throw new RuntimeException("Your signing key is not verified by the registrar! Please contact registrar or sign with a verified key.");
        }
        if (!request.verifySignature("X-Gateway-Authorization",getPath().getHeaders(),true)){
            throw new RuntimeException("Signature Verification failed");
        }
        NetworkRole role = NetworkRole.find(new Subscriber(){{
            setSubscriberId(subscriber_id);
            setType(Subscriber.SUBSCRIBER_TYPE_BG);
        }});

        if (role == null){
            throw new RuntimeException("Invalid Subscriber : " + subscriber_id) ;
        }


        if (!ObjectUtil.equals(role.getNetworkParticipantId() ,signedWithKey.getNetworkParticipantId())){
            throw new RuntimeException("Key signed with is not registered against you. Please contact registrar");
        }


        Subscribers subscribers = new Subscribers(payload);
        for (Subscriber subscriber :subscribers) {
            NetworkRole disabledRole =  NetworkRole.find(subscriber);
            disabledRole.setStatus(NetworkRole.SUBSCRIBER_STATUS_UNSUBSCRIBED);
            disabledRole.save();
            subscriber.setStatus(disabledRole.getStatus());
        }
        return new BytesView(getPath(),subscribers.getInner().toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
    }


    @RequireLogin(false)
    public  View register() {

        String payload = null ;
        try {
            payload = StringUtil.read(getPath().getInputStream());
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
        JSONAware jsonAware =JSONAwareWrapper.parse(payload);
        Subscribers subscribers = new Subscribers();
        if (jsonAware instanceof JSONObject){
            subscribers.add(new Subscriber((JSONObject) jsonAware));
        }else {
            subscribers.setInner((JSONArray) jsonAware);
        }


        for (Subscriber subscriber :subscribers){
            NetworkParticipant networkParticipant = NetworkParticipant.find(subscriber.getSubscriberId());
            networkParticipant.save();

            NetworkRole role = Database.getTable(NetworkRole.class).newRecord();
            role.setSubscriberId(subscriber.getSubscriberId());
            role.setStatus(NetworkRole.SUBSCRIBER_STATUS_INITIATED);
            role.setNetworkParticipantId(networkParticipant.getId());
            role.setUrl(subscriber.getSubscriberUrl());
            role.setType(subscriber.getType());
            if (!ObjectUtil.isVoid(subscriber.getDomain())) {
                if (subscriber.getDomains() == null) {
                    subscriber.setDomains(new Domains());
                }
                subscriber.getDomains().add(subscriber.getDomain());
            }
            for (String domainName : subscriber.getDomains()){
                NetworkDomain domain = NetworkDomain.find(domainName);
                if (!domain.getRawRecord().isNewRecord()) {
                    role.setNetworkDomainId(domain.getId());
                }else {
                    throw new RuntimeException("Invalid domain " + subscriber.getDomain());
                }
                role.save();
            }
            subscriber.setStatus(role.getStatus());
            ParticipantKey key = Database.getTable(ParticipantKey.class).newRecord();
            key.setKeyId(subscriber.getPubKeyId());
            key.setVerified(true);
            key.setNetworkParticipantId(networkParticipant.getId());
            key.setEncrPublicKey(Request.getRawEncryptionKey(subscriber.getEncrPublicKey()));
            key.setSigningPublicKey(Request.getRawSigningKey(subscriber.getSigningPublicKey()));
            key = Database.getTable(ParticipantKey.class).getRefreshed(key);
            if (!key.getRawRecord().isNewRecord() && key.isDirty()){
                throw  new RuntimeException("Cannot modify key attributes as part of registration");
            }
            key.setValidFrom(new Timestamp(subscriber.getValidFrom().getTime()));
            key.setValidUntil(new Timestamp(subscriber.getValidTo().getTime()));
            key.save();

            loadRegion(subscriber,role);
        }
        if (subscribers.size() == 1){
            return new BytesView(getPath(),subscribers.getInner().get(0).toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
        }else {
            return new BytesView(getPath(),subscribers.getInner().toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
        }
    }

    @RequireLogin(false)
    public <T> View subscribe() throws Exception{
        String payload = StringUtil.read(getPath().getInputStream());
        Request request = new Request(payload);
        Map<String,String> params = request.extractAuthorizationParams("Authorization",getPath().getHeaders());
        if (params.isEmpty()){
            throw new RuntimeException("Signature Verification failed");
        }

        String pub_key_id = params.get("pub_key_id");
        String subscriber_id = params.get("subscriber_id");
        ParticipantKey signedWithKey = ParticipantKey.find(pub_key_id);
        if (!signedWithKey.isVerified()){
            throw new RuntimeException("Your signing key is not verified by the registrar! Please contact registrar or sign with a verified key.");
        }

        NetworkRole role = NetworkRole.find(subscriber_id);
        if (!ObjectUtil.equals(role.getNetworkParticipantId() ,signedWithKey.getNetworkParticipantId())){
            throw new RuntimeException("Key signed with is not registered against you. Please contact registrar");
        }
        if (!request.verifySignature("Authorization",getPath().getHeaders(),true)){
            throw new RuntimeException("Signature Verification failed");
        }

        JSONAware jsonAware =JSONAwareWrapper.parse(payload);
        Subscribers subscribers = new Subscribers();
        if (jsonAware instanceof JSONObject){
            subscribers.add(new Subscriber((JSONObject) jsonAware));
        }else {
            subscribers.setInner((JSONArray) jsonAware);
        }

        if (subscribers.isEmpty()){
            if (!ObjectUtil.equals(role.getStatus(),NetworkRole.SUBSCRIBER_STATUS_SUBSCRIBED)){
                TaskManager.instance().executeAsync(new OnSubscribe(role),false);
            }
            final NetworkRole r = role;
            return new BytesView(getPath(),new Subscriber(){{
                setStatus(r.getStatus());
            }}.toString().getBytes(StandardCharsets.UTF_8),MimeType.APPLICATION_JSON);
        }else {
            Subscribers outSubscribers = new Subscribers();
            for (Subscriber subscriber : subscribers){
                if (subscriber.getDomain() != null && subscriber.getDomains() == null){
                    subscriber.setDomains(new Domains());
                    subscriber.getDomains().add(subscriber.getDomain());
                }
                if (!ObjectUtil.isVoid(subscriber.getSubscriberId())){
                    if (!ObjectUtil.equals(subscriber.getSubscriberId(),role.getSubscriberId())){
                        throw new RuntimeException("Cannot sign for a different subscriber!");
                    }
                }else{
                    subscriber.setSubscriberId(role.getSubscriberId());
                }
                boolean newKeyPassed = false;
                if (!ObjectUtil.isVoid(subscriber.getPubKeyId())){
                    ParticipantKey keyPassed = ParticipantKey.find(subscriber.getPubKeyId());
                    newKeyPassed = keyPassed.getRawRecord().isNewRecord() || !keyPassed.isVerified() ;

                    if (!ObjectUtil.isVoid(subscriber.getSigningPublicKey())) {
                        keyPassed.setSigningPublicKey(subscriber.getSigningPublicKey());
                    }
                    if (!ObjectUtil.isVoid(subscriber.getEncrPublicKey())){
                        keyPassed.setEncrPublicKey(subscriber.getEncrPublicKey());
                    }

                    if (keyPassed.isDirty() && !newKeyPassed ){
                        throw new RuntimeException("Cannot modify a verified registered key. Please create a new key.");
                    }
                    keyPassed.setNetworkParticipantId(role.getNetworkParticipantId());
                    if (!ObjectUtil.isVoid(subscriber.getValidFrom())){
                        keyPassed.setValidFrom(new Timestamp(subscriber.getValidFrom().getTime()));
                    }
                    if (!ObjectUtil.isVoid(subscriber.getValidTo())){
                        keyPassed.setValidUntil(new Timestamp(subscriber.getValidTo().getTime()));
                    } //Canc change validity
                    keyPassed.save(); //After save triggers on_subscribe
                }

                for (String sDomain : subscriber.getDomains()){
                    Subscriber outSubscriber = new Subscriber(subscriber.toString());
                    long networkParticipantId = role.getNetworkParticipantId();
                    role = NetworkRole.find(new Subscriber(){{
                        setSubscriberId(subscriber.getSubscriberId());
                        setDomain(sDomain);
                        setType(subscriber.getType());
                    }});
                    role.setNetworkParticipantId(networkParticipantId);
                    if (!ObjectUtil.isVoid(subscriber.getSubscriberUrl())){
                        role.setUrl(subscriber.getSubscriberUrl());
                    }
                    if (role.isDirty()){
                        if (ObjectUtil.equals(role.getStatus(),NetworkRole.SUBSCRIBER_STATUS_SUBSCRIBED)){
                            if (newKeyPassed){
                                throw new RuntimeException("Cannot create a new  key and modify your subscription in the same call.");
                            }
                        }
                        role.setStatus(NetworkRole.SUBSCRIBER_STATUS_INITIATED);
                        role.save(); // After save triggers "on_subscribe call"
                    }
                    loadRegion(subscriber,role);
                    outSubscriber.setStatus(role.getStatus());
                    outSubscribers.add(outSubscriber);
                }
            }
            if (outSubscribers.size() != 1) {
                return new BytesView(getPath(), outSubscribers.getInner().toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON);
            }else {
                return new BytesView(getPath(), outSubscribers.get(0).getInner().toString().getBytes(StandardCharsets.UTF_8), MimeType.APPLICATION_JSON);
            }
        }
    }
    public void loadRegion(Subscriber subscriber, NetworkRole role){
        OperatingRegion region = Database.getTable(OperatingRegion.class).newRecord();
        region.setNetworkRoleId(role.getId());
        if (!ObjectUtil.isVoid(subscriber.getCity())){
            region.setCityId(City.findByCode(subscriber.getCity()).getId());
            region.setCountryId(region.getCity().getState().getCountryId());
        }else if (!ObjectUtil.isVoid(subscriber.getCountry())){
            region.setCountryId(Country.findByISO(subscriber.getCountry()).getId());
        }else {
            return;
        }

        region = Database.getTable(OperatingRegion.class).getRefreshed(region);
        region.save();

    }

    @RequireLogin(false)
    public View lookup() throws Exception{
        String version = "v0";
        String firstElement = getPath().getPathElements().get(0);
        if (!ObjectUtil.equals(firstElement,getPath().controllerPathElement())){
            version = firstElement;
        }

        Subscriber subscriber = new Subscriber((JSONObject) Subscriber.parse(getPath().getInputStream()));
        if (ObjectUtil.isVoid(subscriber.getCity()) && subscriber.getLocation() != null && subscriber.getLocation().getCity() != null) {
            subscriber.setCity(subscriber.getLocation().getCity().getCode());
            subscriber.setLocation(null);
        }

        String format = getPath().getHeaders().get("pub_key_format");
        if (!ObjectUtil.isVoid(format)){
            if (!ObjectUtil.equals("PEM",format.toUpperCase())){
                throw new RuntimeException("Only allowed value to be passed is PEM");
            }
        }
        Subscribers records = lookup(subscriber,0,s->{
            if (!ObjectUtil.isVoid(format)){
                s.setSigningPublicKey(Request.getPemSigningKey(s.getSigningPublicKey()));
                s.setEncrPublicKey(Request.getPemEncryptionKey(s.getEncrPublicKey()));
            }
        });


        return new BytesView(getPath(),records.getInner().toString().getBytes(),MimeType.APPLICATION_JSON);

    }

    @RequireLogin(false)
    public View generateSignatureKeys(){
        CryptoKey key = Database.getTable(CryptoKey.class).newRecord();

        String[] pair = CryptoKey.generateKeyPair(Request.SIGNATURE_ALGO,Request.SIGNATURE_ALGO_KEY_LENGTH);
        key.setPrivateKey(pair[0]);
        key.setPublicKey(pair[1]);
        return IntegrationAdaptor.instance(CryptoKey.class,FormatHelper.getFormatClass(MimeType.APPLICATION_JSON)).
                createResponse(getPath(),key, Arrays.asList("PUBLIC_KEY","PRIVATE_KEY"));

    }


    @RequireLogin(false)
    public View generateEncryptionKeys(){
        CryptoKey key = Database.getTable(CryptoKey.class).newRecord();

        String[] pair = CryptoKey.generateKeyPair(Request.ENCRYPTION_ALGO,Request.ENCRYPTION_ALGO_KEY_LENGTH);
        key.setPrivateKey(pair[0]);
        key.setPublicKey(pair[1]);
        return IntegrationAdaptor.instance(CryptoKey.class,FormatHelper.getFormatClass(MimeType.APPLICATION_JSON)).
                createResponse(getPath(),key, Arrays.asList("PUBLIC_KEY","PRIVATE_KEY"));

    }


    public static Subscribers lookup(Subscriber criteria, int maxRecords, KeyFormatFixer fixer) {
        return lookup(criteria, maxRecords, null,fixer);
    }


    public static List<OperatingRegion> findSubscribedRegions(Subscriber criteria, List<Long> networkRoleIds){
        List<OperatingRegion> regions = new ArrayList<>();
        if (ObjectUtil.isVoid(criteria.getCity()) && ObjectUtil.isVoid(criteria.getCountry())){
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

        if (networkRoleIds != null) {
            where.add(new Expression(ref.getPool(), "NETWORK_ROLE_ID", Operator.IN, networkRoleIds.toArray()));
        }

        Select sel = new Select("MAX(ID) AS ID","NETWORK_ROLE_ID").from(OperatingRegion.class).where(where).groupBy("NETWORK_ROLE_ID");
        return sel.execute(OperatingRegion.class);
    }
    public static Subscribers lookup(Subscriber criteria, int maxRecords, Expression additionalWhere, KeyFormatFixer fixer) {
        ParticipantKey key = ObjectUtil.isVoid(criteria.getPubKeyId())? null : ParticipantKey.find(criteria.getPubKeyId());
        if (key != null && key.getRawRecord().isNewRecord()){
            //invalid key being looked up.
            return new Subscribers();
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
        if (ObjectUtil.isVoid(criteria.getCity()) && ObjectUtil.isVoid(criteria.getCountry()) ){
            regionPassed = false;
        }

        Subscribers subscribers = new Subscribers();
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
            Subscriber subscriber = getSubscriber(key,role,null,fixer);
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
                    Subscriber subscriber = getSubscriber(key, networkRole, null,fixer);
                    if (subscriber != null) {
                        subscribers.add(subscriber);
                    }
                }
            }
        }


        return subscribers;
    }
    public interface KeyFormatFixer {
        public void fix(Subscriber subscriber);
    }
    static Subscriber getSubscriber(ParticipantKey criteriaKey , NetworkRole networkRole, OperatingRegion region, KeyFormatFixer fixer) {
        ParticipantKey key = null;
        if (Config.instance().getBooleanProperty("beckn.require.kyc",false)){
            if (!networkRole.getNetworkParticipant().isKycComplete()){
                return null;
            }
        }

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
        Subscriber subscriber = getSubscriber(networkRole, region, key);
        fixer.fix(subscriber);
        return subscriber;
    }

    @NotNull
    private static Subscriber getSubscriber(NetworkRole networkRole, OperatingRegion region, ParticipantKey key) {
        Subscriber subscriber = new Subscriber();

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
        subscriber.setValidFrom(key.getValidFrom()) ;
        subscriber.setValidTo(key.getValidUntil());
        if (region != null ) {
            City city =  region.getCity();
            if (city != null){
                subscriber.setCity(region.getCity().getName());
            }
            Country country = region.getCountry();
            if (country != null){
                subscriber.setCountry(region.getCountry().getName());
            }
        }
        subscriber.setCreated(networkRole.getCreatedAt());
        subscriber.setUpdated(networkRole.getUpdatedAt());
        return subscriber;
    }
}
