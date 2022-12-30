package in.succinct.beckn.registry.extensions;

import com.venky.core.random.Randomizer;
import com.venky.core.security.Crypt;
import com.venky.core.util.Bucket;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.routing.Config;
import in.succinct.beckn.Request;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantKey;
import org.json.simple.JSONObject;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AfterSaveParticipantKey extends AfterModelSaveExtension<ParticipantKey> {
    static {
        registerExtension(new AfterSaveParticipantKey());
    }

    @Override
    public void afterSave(ParticipantKey participantKey) {
        if (!participantKey.isVerified()) {
            TaskManager.instance().executeAsync(new OnSubscribe(participantKey), false);
        }
    }

    public static class OnSubscribe implements Task {
        NetworkRole role;
        ParticipantKey participantKey ;

        public OnSubscribe(ParticipantKey participantKey) {
            this.participantKey = participantKey;
            this.role = null;
        }

        public OnSubscribe(NetworkRole role) {
            this.role = role;
            this.participantKey = null;
        }

        @Override
        public void execute() {
            if (this.role == null && this.participantKey == null){
                return;
            }
            NetworkParticipant participant = role != null ? role.getNetworkParticipant() : participantKey.getNetworkParticipant();

            List<NetworkRole> subscribers = role != null ? new ArrayList<>(Arrays.asList(role)) : participant.getNetworkRoles();

            List<ParticipantKey> participantKeys = participantKey != null ? new ArrayList<>(Arrays.asList(participantKey)) : participant.getParticipantKeys();

            if (participantKeys.isEmpty()){
                throw new RuntimeException("No Keys configured!");
            }
            for (NetworkRole subscriber : subscribers){
                List<ParticipantKey> keys = new ArrayList<>(participantKeys);
                if (ObjectUtil.equals(subscriber.getStatus(),NetworkRole.SUBSCRIBER_STATUS_SUBSCRIBED)){
                    keys.removeIf(ParticipantKey::isVerified);
                }else {
                    keys.removeIf(k -> !k.isVerified());
                }
                Bucket numResponsesRemaining = new Bucket(keys.size());
                for (ParticipantKey pk : keys) {
                    JSONObject input = new JSONObject();
                    input.put("subscriber_id", subscriber.getSubscriberId());
                    input.put("pub_key_id", pk.getKeyId());

                    StringBuilder otp = new StringBuilder();
                    for (int i = 0; i < 32; i++) {
                        otp.append(Randomizer.getRandomNumber(i == 0 ? 1 : 0, 9));
                    }
                    try {
                        PublicKey key = Request.getEncryptionPublicKey(pk.getEncrPublicKey());
                        CryptoKey cryptoKey = CryptoKey.find(Config.instance().getHostName() + ".k1",CryptoKey.PURPOSE_ENCRYPTION);
                        PrivateKey privateKey = Crypt.getInstance().getPrivateKey(Request.ENCRYPTION_ALGO, cryptoKey.getPrivateKey());
                        NetworkRole registry = NetworkParticipant.find(Config.instance().getHostName()).getNetworkRoles().stream().filter(r->r.getType().equals(Subscriber.SUBSCRIBER_TYPE_LOCAL_REGISTRY)).findFirst().get();


                        KeyAgreement agreement = KeyAgreement.getInstance(Request.ENCRYPTION_ALGO);
                        agreement.init(privateKey);
                        agreement.doPhase(key, true);
                        SecretKey symKey = agreement.generateSecret("TlsPremasterSecret");

                        String encrypted = Crypt.getInstance().encrypt(otp.toString(), "AES", symKey);
                        input.put("challenge", encrypted);

                        JSONObject response = new Call<JSONObject>().url(subscriber.getUrl() + "/on_subscribe")
                                .method(HttpMethod.POST).inputFormat(InputFormat.JSON).input(input)
                                .header("Content-type", MimeType.APPLICATION_JSON.toString())
                                .header("Authorization",new Request(input).generateAuthorizationHeader(registry.getSubscriberId(),Config.instance().getHostName() + ".k1"))
                                .header("Signature", Request.generateSignature(input.toString(), CryptoKey.find(Config.instance().getHostName() + ".k1",CryptoKey.PURPOSE_SIGNING).getPrivateKey())).getResponseAsJson();

                        if (ObjectUtil.equals(response.get("answer"), otp.toString())) {
                            numResponsesRemaining.decrement();
                        }


                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
                if (numResponsesRemaining.intValue() == 0){
                    if (!ObjectUtil.equals(subscriber.getStatus(),NetworkRole.SUBSCRIBER_STATUS_SUBSCRIBED)){
                        subscriber.setStatus(NetworkRole.SUBSCRIBER_STATUS_SUBSCRIBED);
                        subscriber.save();
                    }else {
                        for (ParticipantKey key : keys){
                            key.setVerified(true);
                            key.save();
                        }
                    }
                }
            }
        }

    }

}
