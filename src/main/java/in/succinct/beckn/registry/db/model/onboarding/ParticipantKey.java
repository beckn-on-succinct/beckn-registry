package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;

import java.sql.Timestamp;

public interface ParticipantKey extends Model {
    static ParticipantKey find(String keyId) {
        ParticipantKey key = Database.getTable(ParticipantKey.class).newRecord();
        key.setKeyId(keyId);
        key = Database.getTable(ParticipantKey.class).getRefreshed(key);
        return key;
    }

    @IS_NULLABLE(false)
    @PARTICIPANT
    public Long getNetworkParticipantId();
    public void setNetworkParticipantId(Long id);
    public NetworkParticipant getNetworkParticipant();

    @UNIQUE_KEY
    @IS_NULLABLE(false)
    public String getKeyId();
    public void setKeyId(String keyId);

    @COLUMN_SIZE(4096)
    @IS_NULLABLE(false)
    public String getSigningPublicKey();
    public void setSigningPublicKey(String key);

    @COLUMN_SIZE(4096)
    @IS_NULLABLE(false)
    public String getEncrPublicKey();
    public void setEncrPublicKey(String key);

    @IS_NULLABLE(false)
    public Timestamp getValidFrom();
    public void setValidFrom(Timestamp from);

    @IS_NULLABLE(false)
    public Timestamp getValidUntil();
    public void setValidUntil(Timestamp until);

    @IS_NULLABLE(false)
    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    public boolean isVerified();
    public void setVerified(boolean verified);


    /* Once challenge is resolved , this key would be marked as verified
     or primary first key could be marked verified manually,*/

}
