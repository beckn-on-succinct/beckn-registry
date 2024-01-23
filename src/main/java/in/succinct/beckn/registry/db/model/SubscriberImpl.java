package in.succinct.beckn.registry.db.model;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.table.ModelImpl;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantKey;

public class SubscriberImpl extends ModelImpl<Subscriber> {
    public SubscriberImpl(){
        super();
    }
    public SubscriberImpl(Subscriber proxy){
        super(proxy);
    }

    public void subscribe(){
        Subscriber subscriber = getProxy();
        if (!ObjectUtil.isVoid(subscriber.getUniqueKeyId())){
            ParticipantKey key = ParticipantKey.find(subscriber.getUniqueKeyId());
            if (key.getRawRecord().isNewRecord()){
                key.setVerified(false);
                key.save();
            }
        }else if (!ObjectUtil.isVoid(subscriber.getSubscriberId())){
            NetworkRole role = NetworkRole.find(subscriber.getSubscriberId(),subscriber.getType());
            if (role.getRawRecord().isNewRecord()){
                throw new RuntimeException("On boarding is necessary before subscription.");
            }

        }


    }


}
