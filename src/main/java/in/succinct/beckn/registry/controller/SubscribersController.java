package in.succinct.beckn.registry.controller;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.geo.GeoCoordinate;
import com.venky.swf.controller.VirtualModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.FormatHelper.KeyCase;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.routing.Config;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.Location;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.Subscriber;
import in.succinct.beckn.registry.db.model.onboarding.NetworkDomain;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.db.model.onboarding.OperatingRegion;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantKey;
import in.succinct.beckn.registry.extensions.AfterSaveParticipantKey.OnSubscribe;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SubscribersController extends VirtualModelController<Subscriber> {
    public SubscribersController(Path path) {
        super(path);
    }

    @RequireLogin(false)
    public  <T> View register() throws Exception{
        List<Subscriber> subscribers = getIntegrationAdaptor().readRequest(getPath());
        for (Subscriber subscriber :subscribers){
            NetworkParticipant networkParticipant = Database.getTable(NetworkParticipant.class).newRecord();
            networkParticipant.setParticipantId(subscriber.getSubscriberId());
            networkParticipant.save();
            NetworkRole role = Database.getTable(NetworkRole.class).newRecord();
            role.setSubscriberId(subscriber.getSubscriberId());
            role.setStatus(NetworkRole.SUBSCRIBER_STATUS_INITIATED);
            role.setNetworkParticipantId(networkParticipant.getId());
            role.setUrl(subscriber.getSubscriberUrl());
            role.setType(subscriber.getType());
            role.setNetworkDomainId(NetworkDomain.find(subscriber.getDomain()).getId());
            role.save();
            subscriber.setStatus(role.getStatus());

            ParticipantKey key = Database.getTable(ParticipantKey.class).newRecord();
            key.setKeyId(subscriber.getPubKeyId());
            key.setVerified(true);
            key.setNetworkParticipantId(networkParticipant.getId());
            key.setEncrPublicKey(Request.getRawEncryptionKey(subscriber.getEncrPublicKey()));
            key.setSigningPublicKey(Request.getRawSigningKey(subscriber.getSigningPublicKey()));
            key.setValidFrom(new Timestamp(BecknObject.TIMESTAMP_FORMAT.parse(subscriber.getValidFrom()).getTime()));
            key.setValidUntil(new Timestamp(BecknObject.TIMESTAMP_FORMAT.parse(subscriber.getValidUntil()).getTime()));
            key.save();
            loadRegion(subscriber,role);
        }
        if (subscribers.size() == 1){
            return getReturnIntegrationAdaptor().createResponse(getPath(),subscribers.get(0),Arrays.asList("STATUS"));
        }else {
            return getReturnIntegrationAdaptor().createResponse(getPath(), subscribers, Arrays.asList("STATUS"));
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
        NetworkRole role =  NetworkRole.find(subscriber_id);
        ParticipantKey signedWithKey = ParticipantKey.find(pub_key_id);
        if (!signedWithKey.isVerified()){
            throw new RuntimeException("Your signing key is not verified by the registrar! Please contact registrar or sign with a verified key.");
        }
        if (!ObjectUtil.equals(role.getNetworkParticipantId() ,signedWithKey.getNetworkParticipantId())){
            throw new RuntimeException("Key signed with is not registered against you. Please contact registrar");
        }
        if (!request.verifySignature("Authorization",getPath().getHeaders(),true)){
            throw new RuntimeException("Signature Verification failed");
        }

        List<Subscriber> subscribers = getIntegrationAdaptor().readRequest(getPath());
        if (subscribers.isEmpty()){
            if (!ObjectUtil.equals(role.getStatus(),NetworkRole.SUBSCRIBER_STATUS_SUBSCRIBED)){
                TaskManager.instance().executeAsync(new OnSubscribe(role),false);
            }
            Subscriber subscriber = Database.getTable(Subscriber.class).newRecord();
            subscriber.setStatus(role.getStatus());
            return getReturnIntegrationAdaptor().createResponse(getPath(),subscriber,Arrays.asList("STATUS"));
        }else {
            for (Subscriber subscriber : subscribers){
                if (!ObjectUtil.isVoid(subscriber.getSubscriberId())){
                    if (!ObjectUtil.equals(subscriber.getSubscriberId(),role.getSubscriberId())){
                        throw new RuntimeException("Cannot sign for a different subscriber!");
                    }
                }else{
                    subscriber.setSubscriberId(role.getSubscriberId());
                }

                ParticipantKey newKey = null;
                if (!ObjectUtil.isVoid(subscriber.getPubKeyId())){
                    newKey = ParticipantKey.find(subscriber.getPubKeyId());
                }
                if (newKey != null){
                    if (!ObjectUtil.isVoid(subscriber.getSigningPublicKey())) {
                        newKey.setSigningPublicKey(Request.getRawSigningKey(subscriber.getSigningPublicKey()));
                    }
                    if (!ObjectUtil.isVoid(subscriber.getEncrPublicKey())){
                        newKey.setEncrPublicKey(Request.getRawEncryptionKey(subscriber.getEncrPublicKey()));
                    }

                    if (!newKey.getRawRecord().isNewRecord() &&  newKey.isVerified()){
                        if (newKey.isDirty()) {
                            throw new RuntimeException("Cannot modify a verified registered key. Please create a new key.");
                        }
                    }else {
                        newKey.setVerified(false);
                    }
                    newKey.setNetworkParticipantId(role.getNetworkParticipantId());
                    if (!ObjectUtil.isVoid(subscriber.getValidFrom())){
                        newKey.setValidFrom(new Timestamp(BecknObject.TIMESTAMP_FORMAT.parse(subscriber.getValidFrom()).getTime()));
                    }
                    if (!ObjectUtil.isVoid(subscriber.getValidUntil())){
                        newKey.setValidUntil(new Timestamp(BecknObject.TIMESTAMP_FORMAT.parse(subscriber.getValidUntil()).getTime()));
                    }
                    if (newKey.getValidUntil() != null  && newKey.getValidUntil().getTime() <= System.currentTimeMillis()){
                        newKey.setVerified(false);
                    }
                    newKey.save();
                    TaskManager.instance().executeAsync(new OnSubscribe(newKey),false);

                }
                if (!newKey.isVerified()){
                    newKey.setEncrPublicKey(subscriber.getEncrPublicKey());
                    newKey.setSigningPublicKey(subscriber.getSigningPublicKey());
                    newKey.setVerified(false);

                }
                if (!ObjectUtil.isVoid(subscriber.getSubscriberUrl())){
                    role.setUrl(subscriber.getSubscriberUrl());
                }
                if (!ObjectUtil.isVoid(subscriber.getDomain()) && !ObjectUtil.equals(subscriber.getDomain(),role.getNetworkDomain().getName())){
                    throw  new RuntimeException("Cannot change your domain!. you need to register with the registrar for the right domains.");
                }
                if (!ObjectUtil.isVoid(subscriber.getType()) && !ObjectUtil.equals(subscriber.getType(),role.getType())){
                    throw  new RuntimeException("Cannot change your role/type!. you need to register with the registrar for the right roles you wish to participate in your domain.");
                }
                if (role.isDirty()){
                    if (ObjectUtil.equals(role.getStatus(),NetworkRole.SUBSCRIBER_STATUS_SUBSCRIBED)){
                        if (newKey != null){
                            throw new RuntimeException("Cannot create a new  key and modify your subscription in the same call.");
                        }
                    }
                    role.setStatus(NetworkRole.SUBSCRIBER_STATUS_INITIATED);
                    role.save();
                    TaskManager.instance().executeAsync(new OnSubscribe(role),false);
                }
                subscriber.setStatus(role.getStatus());
                loadRegion(subscriber,role);
            }
            if (subscribers.size() == 1){
                return getReturnIntegrationAdaptor().createResponse(getPath(),subscribers.get(0),Arrays.asList("STATUS"));
            }else {
                return getReturnIntegrationAdaptor().createResponse(getPath(), subscribers, Arrays.asList("STATUS"));
            }
        }

    }
    public void loadRegion(Subscriber subscriber,NetworkRole role){
        OperatingRegion region = Database.getTable(OperatingRegion.class).newRecord();
        region.setNetworkRoleId(role.getId());
        if (!ObjectUtil.isVoid(subscriber.getCity())){
            region.setCityId(City.findByCode(subscriber.getCity()).getId());
            region.setCountryId(region.getCity().getState().getCountryId());
        }else if (!ObjectUtil.isVoid(subscriber.getCountry())){
            region.setCountryId(Country.findByISO(subscriber.getCountry()).getId());
        }
        if (!ObjectUtil.isVoid(subscriber.getLat()) && !ObjectUtil.isVoid(subscriber.getLng()) && !ObjectUtil.isVoid(subscriber.getRadius()) ) {
            region.setLat(subscriber.getLat());
            region.setLng(subscriber.getLng());
            region.setRadius(subscriber.getRadius());
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

        FormatHelper<JSONObject> helper = null ;
        {
            // Do input bc.
            in.succinct.beckn.Subscriber bcSubscriber = new in.succinct.beckn.Subscriber((JSONObject) JSONValue.parse(new InputStreamReader(getPath().getInputStream())));
            helper = FormatHelper.instance(bcSubscriber.getInner());
            if (ObjectUtil.isVoid(bcSubscriber.getCity()) && bcSubscriber.getLocation() != null && bcSubscriber.getLocation().getCity() != null) {
                bcSubscriber.setCity(bcSubscriber.getLocation().getCity().getCode());
                bcSubscriber.setLocation(null);
            }
            helper.change_key_case(KeyCase.CAMEL);
        }
        Subscriber subscriber = ModelIOFactory.getReader(Subscriber.class, helper.getFormatClass()).read(helper.getRoot());

        List<Subscriber> records = Subscriber.lookup(subscriber,MAX_LIST_RECORDS,getWhereClause());
        String format = getPath().getHeaders().get("pub_key_format");
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

        FormatHelper<T> outHelper = FormatHelper.instance(helper.getMimeType(),StringUtil.pluralize(getModelClass().getSimpleName()),true);
        ModelIOFactory.getWriter(getModelClass(),helper.getFormatClass()).write(records,outHelper.getRoot(),fields);
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

        return new BytesView(getPath(),out.toString().getBytes(),MimeType.APPLICATION_JSON);

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

    @RequireLogin(false)
    public View register_location() throws  Exception {
        return update_location(true);
    }
    @RequireLogin(false)
    public View deregister_location() throws Exception {
        return update_location(false);
    }
    private View update_location(boolean active) throws Exception {
        String payload = StringUtil.read(getPath().getInputStream());

        Request request = new Request(payload);
        if (Config.instance().getBooleanProperty("beckn.auth.enabled", false) &&
                !request.verifySignature("Authorization",getPath().getHeaders())) {
            throw new RuntimeException("Cannot identify Subscriber");
        }

        Map<String, String> authParams = request.extractAuthorizationParams("Authorization",getPath().getHeaders());
        String subscriberId = Request.getSubscriberId(authParams);
        if (subscriberId == null) {
            throw new RuntimeException("Cannot identify Subscriber");
        }
        NetworkRole networkRole = NetworkRole.find(subscriberId);

        Location location = new Location(payload);

        OperatingRegion region = Database.getTable(OperatingRegion.class).newRecord();
        region.setNetworkRoleId(networkRole.getId());
        if (location.getCountry() != null){
            com.venky.swf.plugins.collab.db.model.config.Country country = com.venky.swf.plugins.collab.db.model.config.Country.findByISO(location.getCountry().getCode());
            if (country != null){
                region.setCountryId(country.getId());
            }
        }
        if (location.getCity() != null) {
            com.venky.swf.plugins.collab.db.model.config.City city =
                    com.venky.swf.plugins.collab.db.model.config.City.findByCode(location.getCity().getCode());
            if (city != null) {
                region.setCityId(city.getId());
                region.setCountryId(city.getState().getCountryId());
            }
        }
        GeoCoordinate coordinate = location.getGps();
        if (coordinate != null){
            region.setLat(coordinate.getLat());
            region.setLng(coordinate.getLng());
        }
        if (location.getCircle() != null){
            region.setRadius(location.getCircle().getRadius().getValue());
        }

        region = Database.getTable(OperatingRegion.class).getRefreshed(region);
        region.save();
        return getIntegrationAdaptor().createStatusResponse(getPath(),null,"Location Information Updated!");
    }
}
