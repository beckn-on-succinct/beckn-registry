package in.succinct.beckn.registry.controller;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.Count;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.FormatHelper.KeyCase;
import com.venky.swf.integration.IntegrationAdaptor;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.CryptoKey;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.beckn.Location;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.City;
import in.succinct.beckn.registry.db.model.Country;
import in.succinct.beckn.registry.db.model.Subscriber;
import in.succinct.beckn.registry.db.model.SubscriberLocation;
import org.apache.lucene.search.Query;
import org.apache.regexp.RE;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SubscribersController extends ModelController<Subscriber> {
    public SubscribersController(Path path) {
        super(path);
    }

    @RequireLogin(false)
    public <T> View subscribe() throws Exception{
        JSONObject object = (JSONObject) JSONValue.parse(new InputStreamReader(getPath().getInputStream()));
        FormatHelper<JSONObject> helper = FormatHelper.instance(object);

        helper.change_key_case(KeyCase.CAMEL);
        String countryCode = helper.getAttribute("Country");
        Country country = null;
        if (countryCode != null){
            country = Database.getTable(Country.class).newRecord();
            country.setCode(countryCode);
            country = Database.getTable(Country.class).getRefreshed(country);
            helper.removeAttribute("Country");
            helper.setAttribute("CountryId",String.valueOf(country.getId()));
        }
        String cityCode = helper.getAttribute("City");
        City city = null;
        if (cityCode != null){
            city = Database.getTable(City.class).newRecord();
            city.setCode(cityCode);
            city = Database.getTable(City.class).getRefreshed(city);
            helper.removeAttribute("City");
            helper.setAttribute("CityId", String.valueOf(city.getId()));
        }

        Subscriber subscriber = ModelIOFactory.getReader(Subscriber.class,JSONObject.class).read(object);

        if (subscriber.getRawRecord().isNewRecord() || subscriber.isDirty() ){
            subscriber.setStatus("INITIATED");
            // My be you need to do ip based validation to prevent DOS attacks.
        }
        subscriber.setUpdated(new Timestamp(System.currentTimeMillis()));
        subscriber.save();

        return getReturnIntegrationAdaptor().createResponse(getPath(),subscriber, Arrays.asList("STATUS"));
    }

    @RequireLogin(false)
    public <T> View lookup() throws Exception{
        JSONObject object = (JSONObject) JSONValue.parse(new InputStreamReader(getPath().getInputStream()));
        FormatHelper<JSONObject> helper = FormatHelper.instance(object);
        helper.change_key_case(KeyCase.CAMEL);

        String countryCode = helper.getAttribute("Country");
        Country country = null;
        if (countryCode != null){
            country = Database.getTable(Country.class).newRecord();
            country.setCode(countryCode);
            country = Database.getTable(Country.class).getRefreshed(country);
            helper.removeAttribute("Country");
            helper.setAttribute("CountryId",String.valueOf(country.getId()));
        }
        String cityCode = helper.getAttribute("City");
        City city = null;
        if (cityCode != null){
            city = Database.getTable(City.class).newRecord();
            city.setCode(cityCode);
            city = Database.getTable(City.class).getRefreshed(city);
            helper.removeAttribute("City");
            helper.setAttribute("CityId", String.valueOf(city.getId()));
        }

        Subscriber criteria = ModelIOFactory.getReader(Subscriber.class,JSONObject.class).read(object);
        List<Subscriber> records = Subscriber.lookup(criteria,MAX_LIST_RECORDS,getWhereClause());

        SeekableByteArrayOutputStream baos = new SeekableByteArrayOutputStream();
        ModelIOFactory.getWriter(getModelClass(),getReturnIntegrationAdaptor().getFormatClass()).write(records,baos,Arrays.asList("SUBSCRIBER_ID","SUBSCRIBER_URL","TYPE","DOMAIN",
                "CITY_ID","COUNTRY_ID","SIGNING_PUBLIC_KEY","ENCR_PUBLIC_KEY","VALID_FROM","VALID_UNTIL","STATUS","CREATED","UPDATED"));

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        FormatHelper<T> outHelper = FormatHelper.instance(getIntegrationAdaptor().getMimeType(),bais);
        outHelper.change_key_case(KeyCase.TITLE);

        List<T> subscribers = outHelper.getArrayElements("subscribers");
        JSONArray array = new JSONArray();
        array.addAll(subscribers);

        return new BytesView(getPath(),array.toString().getBytes(),getReturnIntegrationAdaptor().getMimeType());

    }

    @RequireLogin(false)
    public View generateSignatureKeys(){
        CryptoKey key = Database.getTable(CryptoKey.class).newRecord();

        String[] pair = CryptoKey.generateKeyPair(Request.SIGNATURE_ALGO,Request.SIGNATURE_ALGO_KEY_LENGTH);
        key.setPrivateKey(pair[0]);
        key.setPublicKey(pair[1]);
        return IntegrationAdaptor.instance(CryptoKey.class,getIntegrationAdaptor().getFormatClass()).
                createResponse(getPath(),key, Arrays.asList("PUBLIC_KEY","PRIVATE_KEY"));

    }
    @RequireLogin(false)
    public View generateEncryptionKeys(){
        CryptoKey key = Database.getTable(CryptoKey.class).newRecord();

        String[] pair = CryptoKey.generateKeyPair(Request.ENCRYPTION_ALGO,Request.ENCRYPTION_ALGO_KEY_LENGTH);
        key.setPrivateKey(pair[0]);
        key.setPublicKey(pair[1]);
        return IntegrationAdaptor.instance(CryptoKey.class,getIntegrationAdaptor().getFormatClass()).
                createResponse(getPath(),key, Arrays.asList("PUBLIC_KEY","PRIVATE_KEY"));

    }

    @RequireLogin(false)
    public View register_location() throws  Exception{
        String payload = StringUtil.read(getPath().getInputStream());

        Location location = new Location(payload);

        String subscriberId = null;
        if (Config.instance().getBooleanProperty("beckn.auth.enabled", false)){
            Request request = new Request(payload);
            if (request.verifySignature("Authorization",getPath().getHeaders())){
                Map<String, String> authParams = request.extractAuthorizationParams("Authorization",getPath().getHeaders());
                subscriberId = request.getSubscriberId(authParams);
            }
        }else {
            String  providerLocationID = location.getId();
            int fromIndex = providerLocationID.lastIndexOf("@");
            int toIndex = providerLocationID.lastIndexOf(".provider_location");
            subscriberId = providerLocationID.substring(fromIndex+1,toIndex);
        }
        if (subscriberId == null){
            throw new RuntimeException("Could not identify subscriber!");
        }
        Subscriber criteria = Database.getTable(Subscriber.class).newRecord();
        criteria.setSubscriberId(subscriberId);
        criteria.setCityId(com.venky.swf.plugins.collab.db.model.config.City.fi);
        SubscriberLocation subscriberLocation = Database.getTable(SubscriberLocation.class).newRecord();
        subscriberLocation.setSubscriberId(subscriberId);

        //TODO Register location.
        return null;


    }
}
