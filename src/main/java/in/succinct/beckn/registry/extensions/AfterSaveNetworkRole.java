package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.db.model.application.Event;
import com.venky.swf.plugins.background.core.DbTask;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.background.messaging.EventEmitter;
import com.venky.swf.routing.Config;
import in.succinct.beckn.registry.db.model.Subscriber;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.extensions.AfterSaveParticipantKey.OnSubscribe;
import org.json.simple.JSONArray;

import java.util.Arrays;
import java.util.logging.Level;

public class AfterSaveNetworkRole extends AfterModelSaveExtension<NetworkRole> {
    static {
        registerExtension(new AfterSaveNetworkRole());
    }

    @Override
    public void afterSave(NetworkRole model) {
        if (ObjectUtil.equals("INITIATED", model.getStatus())) {
            TaskManager.instance().executeAsync(new OnSubscribe(model), false);
        }
        Subscriber subscriber = Database.getTable(Subscriber.class).newRecord();
        subscriber.setSubscriberId(model.getSubscriberId());
        if (model.getNetworkDomain() != null) {
            subscriber.setDomain(model.getNetworkDomain().getName());
        }
        subscriber.setType(model.getType());

        JSONArray array = Subscriber.toBeckn(Arrays.asList(subscriber), null, model.getCoreVersion());
        if (array.size() == 1) {
            EventEmitter.getInstance().emit("on_subscriber_update",array.get(0));
        }else if (array.size() > 1){
            EventEmitter.getInstance().emit("on_subscriber_update",array);
        }
    }



}
