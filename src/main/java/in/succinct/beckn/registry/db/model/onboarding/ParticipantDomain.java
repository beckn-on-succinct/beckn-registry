package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.model.Model;

public interface ParticipantDomain extends Model {
    @UNIQUE_KEY
    @HIDDEN
    public Long getNetworkRoleId();
    public void setNetworkRoleId(Long id);
    public NetworkRole getNetworkRole();


    @UNIQUE_KEY
    public Long getNetworkDomainId();
    public void setNetworkDomainId(Long id);
    public NetworkDomain getNetworkDomain();


}
