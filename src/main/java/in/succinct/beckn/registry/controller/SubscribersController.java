package in.succinct.beckn.registry.controller;

import com.venky.core.io.ByteArrayInputStream;
import com.venky.core.io.SeekableByteArrayOutputStream;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.model.io.ModelIOFactory;
import com.venky.swf.integration.FormatHelper;
import com.venky.swf.integration.FormatHelper.KeyCase;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.lucene.index.LuceneIndexer;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import com.venky.swf.views.BytesView;
import com.venky.swf.views.View;
import in.succinct.beckn.registry.db.model.Subscriber;
import org.apache.lucene.search.Query;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubscribersController extends ModelController<Subscriber> {
    public SubscribersController(Path path) {
        super(path);
    }

    @RequireLogin(false)
    public <T> View subscribe() throws Exception{
        JSONObject object = (JSONObject) JSONValue.parse(new InputStreamReader(getPath().getInputStream()));
        FormatHelper.instance(object).change_key_case(KeyCase.CAMEL);

        Subscriber subscriber = ModelIOFactory.getReader(Subscriber.class,JSONObject.class).read(object);
        if (subscriber.getRawRecord().isNewRecord()){
            subscriber.setStatus("INITIATED");
        }
        subscriber.setUpdated(new Timestamp(System.currentTimeMillis()));
        subscriber.save();

        return getReturnIntegrationAdaptor().createResponse(getPath(),subscriber, Arrays.asList("STATUS"));
    }

    @RequireLogin(false)
    public <T> View lookup() throws Exception{
        JSONObject object = (JSONObject) JSONValue.parse(new InputStreamReader(getPath().getInputStream()));
        FormatHelper.instance(object).change_key_case(KeyCase.CAMEL);

        Subscriber criteria = ModelIOFactory.getReader(Subscriber.class,JSONObject.class).read(object);

        StringBuilder searchQry = new StringBuilder();


        if (!criteria.getReflector().isVoid(criteria.getSubscriberId())){
            searchQry.append("SUBSCRIBER_ID:").append(criteria.getSubscriberId().replace(".","\\."));
        }
        if (!criteria.getReflector().isVoid(criteria.getCityId())){
            if (searchQry.length() > 0){
                searchQry.append(" AND ");
            }
            searchQry.append(" ( CITY_ID:").append(criteria.getCityId()).append(" OR CITY_ID:NULL ) ");
        }
        if (!criteria.getReflector().isVoid(criteria.getCountryId())){
            if (searchQry.length() > 0){
                searchQry.append(" AND ");
            }
            searchQry.append(" COUNTRY_ID:").append(criteria.getCountryId());
        }
        if (!criteria.getReflector().isVoid(criteria.getType())){
            if (searchQry.length() > 0){
                searchQry.append(" AND ");
            }
            searchQry.append(" ( TYPE:").append(criteria.getType()).append( " OR TYPE:NULL ) ");
        }
        if (!criteria.getReflector().isVoid(criteria.getDomain())){
            if (searchQry.length() > 0){
                searchQry.append(" AND ");
            }
            searchQry.append(" DOMAIN:").append(criteria.getDomain());
        }
        LuceneIndexer indexer = LuceneIndexer.instance(getModelClass());
        Query q = indexer.constructQuery(searchQry.toString());

        List<Long> ids = indexer.findIds(q, Select.MAX_RECORDS_ALL_RECORDS);
        List<Subscriber> records = new ArrayList<>();
        if (!ids.isEmpty()) {
            Select sel = new Select().from(getModelClass()).where(new Expression(getReflector().getPool(), Conjunction.AND)
                    .add(Expression.createExpression(getReflector().getPool(), "ID", Operator.IN, ids.toArray()))
                    .add(getWhereClause())).orderBy("TYPE DESC , CITY_ID DESC ");
            records = sel.execute(getModelClass(), MAX_LIST_RECORDS);
        }

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

}
