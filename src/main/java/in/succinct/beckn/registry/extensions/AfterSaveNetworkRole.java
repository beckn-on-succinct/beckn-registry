package in.succinct.beckn.registry.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.plugins.background.core.TaskManager;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.extensions.AfterSaveParticipantKey.OnSubscribe;

public class AfterSaveNetworkRole extends AfterModelSaveExtension<NetworkRole> {
    static {
        registerExtension(new AfterSaveNetworkRole());
    }

    @Override
    public void afterSave(NetworkRole subscriber) {
        if (ObjectUtil.equals("INITIATED", subscriber.getStatus())) {
            TaskManager.instance().executeAsync(new OnSubscribe(subscriber), false);
        }
    }



}
