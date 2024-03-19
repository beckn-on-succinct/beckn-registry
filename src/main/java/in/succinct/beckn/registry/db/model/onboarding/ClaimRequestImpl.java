package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.table.ModelImpl;
import com.venky.swf.routing.Config;

import java.sql.Timestamp;
import java.util.Hashtable;

public class ClaimRequestImpl extends ModelImpl<ClaimRequest> {
    public ClaimRequestImpl() {
    }

    public ClaimRequestImpl(ClaimRequest proxy) {
        super(proxy);
    }
    public String getTxtName() {
        String hostName = Config.instance().getHostName();
        return String.format("%s.token",hostName);
    }

    /**
     * Check if domain has the txt record.
     */
    public void requestDomainVerification(){
        ClaimRequest claimRequest = getProxy();
        claimRequest.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        claimRequest.save();
    }

    public boolean  isTxtRecordVerified() {
        ClaimRequest claimRequest = getProxy();
        if (claimRequest.isDomainVerified()){
            return true;
        }
        if (claimRequest.getNetworkParticipantId() == null){
            return false;
        }

        String domain = claimRequest.getNetworkParticipant().getParticipantId();
        if (ObjectUtil.isVoid(domain)){
            return false;
        }

        String hostName = claimRequest.getTxtName() ;
        String txtValue = claimRequest.getTxtValue();

        if (ObjectUtil.isVoid(txtValue) ) {
            return false;
        }
        hostName = String.format("%s.%s",hostName,domain);

        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        try {
            javax.naming.   directory.DirContext dirContext
                    = new javax.naming.directory.InitialDirContext(env);
            javax.naming.directory.Attributes attrs
                    = dirContext.getAttributes(hostName, new String[]{"TXT"});
            javax.naming.directory.Attribute attr
                    = attrs.get("TXT");

            String txtRecord = "";

            if (attr != null) {
                txtRecord = attr.get().toString();
            }

            return (ObjectUtil.equals(txtRecord, claimRequest.getTxtValue())) ;
        } catch (javax.naming.NamingException e) {
            return  false;
        }
    }

}
