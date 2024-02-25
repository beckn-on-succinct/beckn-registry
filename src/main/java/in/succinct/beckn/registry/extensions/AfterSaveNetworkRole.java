package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.background.messaging.EventEmitter;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.extensions.AfterSaveParticipantKey.OnSubscribe;

public class AfterSaveNetworkRole extends AfterModelSaveExtension<NetworkRole> {
    static {
        registerExtension(new AfterSaveNetworkRole());
    }

    @Override
    public void afterSave(NetworkRole model) {
        if (ObjectUtil.equals("INITIATED", model.getStatus())) {
            TaskManager.instance().executeAsync(new OnSubscribe(model), false);
        }
        Subscriber subscriber = new Subscriber();
        subscriber.setSubscriberId(model.getSubscriberId());
        if (model.getNetworkDomain() != null) {
            subscriber.setDomain(model.getNetworkDomain().getName());
        }
        subscriber.setType(model.getType());
        EventEmitter.getInstance().emit("on_subscriber_update",subscriber.getInner());
    }



}
