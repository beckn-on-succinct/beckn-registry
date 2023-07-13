package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.db.model.application.Event;
import com.venky.swf.plugins.background.core.DbTask;
import com.venky.swf.plugins.background.core.TaskManager;
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
    public void afterSave(NetworkRole subscriber) {
        if (ObjectUtil.equals("INITIATED", subscriber.getStatus())) {
            TaskManager.instance().executeAsync(new OnSubscribe(subscriber), false);
        }
        TaskManager.instance().executeAsync((DbTask)()->{
            Subscriber subscriber1 = Database.getTable(Subscriber.class).newRecord();
            subscriber1.setSubscriberId(subscriber.getSubscriberId());
            try {
                JSONArray array = Subscriber.toBeckn(Arrays.asList(subscriber1), null, subscriber.getCoreVersion());
                if (array.size() == 1) {
                    Event.find("on_subscriber_update").raise(array.get(0));
                }else if (array.size() > 1){
                    Event.find("on_subscriber_update").raise(array);
                }
            }catch (Exception ex){
                Config.instance().getLogger(NetworkRole.class.getName()).log(Level.WARNING,"",ex);
                throw new RuntimeException(ex);
            }

        });
    }



}
