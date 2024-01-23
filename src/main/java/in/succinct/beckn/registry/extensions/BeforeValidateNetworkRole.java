package in.succinct.beckn.registry.extensions;

import com.venky.core.string.StringUtil;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;

import java.util.HashSet;
import java.util.Set;

public class BeforeValidateNetworkRole extends BeforeModelValidateExtension<NetworkRole> {
    static {
        registerExtension(new BeforeValidateNetworkRole());
    }
    @Override
    public void beforeValidate(NetworkRole model) {
        if (ObjectUtil.isVoid(model.getSubscriberId())){
            String domain = "";
            if (model.getNetworkDomain() != null){
                domain = model.getNetworkDomain().getName();
            }
            String participantId = "";
            if (model.getNetworkParticipantId() != null){
                participantId = model.getNetworkParticipant().getParticipantId();
            }
            model.setSubscriberId(String.format("%s.%s.%s",
                    StringUtil.valueOf(participantId),
                    StringUtil.valueOf(domain),
                    StringUtil.valueOf(model.getType())));
        }
        if (model.getNetworkParticipantId() != null) {
            Select select = new Select().from(NetworkRole.class);
            select.where(new Expression(select.getPool(), "SUBSCRIBER_ID", Operator.EQ, model.getSubscriberId()));

            Set<Long> participants = new HashSet<>();
            participants.add(model.getNetworkParticipantId());
            for (NetworkRole networkRole : select.execute(NetworkRole.class)) {
                participants.add(networkRole.getNetworkParticipantId());
            }
            if (participants.size() > 1){
                throw new RuntimeException("Subscriber cannot belong to multiple Participants ");
            }
        }

    }
}
