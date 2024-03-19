package in.succinct.beckn.registry.extensions;

import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.beckn.registry.db.model.onboarding.ClaimRequest;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkParticipantExtension extends ParticipantExtension<NetworkParticipant> {
    static {
        registerExtension(new NetworkParticipantExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, NetworkParticipant partiallyFilledModel, String fieldName) {
        if (ObjectUtil.equals(fieldName,"CREATOR_USER_ID")){
            if (user.getRawRecord().getAsProxy(com.venky.swf.plugins.collab.db.model.user.User.class).isStaff()){
                return null;
            }else {
                return Collections.singletonList(user.getId());
            }
        }else if (ObjectUtil.equals(fieldName,"ANY_USER_ID")){
            return null; //Any one can see.
        }else if (ObjectUtil.equals(fieldName,"MY_NETWORK_PARTICIPANT_ID")){
            SequenceSet<Long> ret = new SequenceSet<Long>();
            Select select = new Select().from(ClaimRequest.class);
            select.where(new Expression(select.getPool(), Conjunction.AND).
                    add(new Expression(select.getPool(),"CREATOR_ID", Operator.EQ,user.getId())).
                    add(new Expression(select.getPool(),"DOMAIN_VERIFIED", Operator.EQ,true)));
            List<ClaimRequest> requests = select.execute();
            ret.addAll(requests.stream().map(r->r.getNetworkParticipantId()).collect(Collectors.toList()));
            return ret;
        }
        return null;
    }
}
