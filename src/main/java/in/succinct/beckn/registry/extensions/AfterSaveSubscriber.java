package in.succinct.beckn.registry.extensions;

import com.venky.core.random.Randomizer;
import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.extensions.AfterModelSaveExtension;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.plugins.background.core.Task;
import com.venky.swf.plugins.background.core.TaskManager;
import com.venky.swf.plugins.collab.db.model.CryptoKey;
import com.venky.swf.routing.Config;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.Subscriber;
import org.json.simple.JSONObject;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Base64;

public class AfterSaveSubscriber extends AfterModelSaveExtension<Subscriber> {
    static {
        registerExtension(new AfterSaveSubscriber());
    }
    @Override
    public void afterSave(Subscriber subscriber) {
        TaskManager.instance().executeAsync(new OnSubscribe(subscriber),false);
    }

    public static class OnSubscribe implements Task {
        Subscriber subscriber ;
        public OnSubscribe(Subscriber subscriber){
            this.subscriber = subscriber;
        }

        @Override
        public void execute() {
            JSONObject input = new JSONObject();
            input.put("subscriber_id",subscriber.getSubscriberId());
            StringBuilder otp = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                otp.append(Randomizer.getRandomNumber(i == 0 ? 1 : 0, 9));
            }
            try {
                PublicKey key = Crypt.getInstance().getPublicKey(Crypt.KEY_ALGO,subscriber.getEncrPublicKey());
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE,key);
                byte[] encrypted = cipher.doFinal(otp.toString().getBytes(StandardCharsets.UTF_8));
                input.put("challenge", Base64.getEncoder().encodeToString(encrypted));


                JSONObject response = new Call<JSONObject>().url(subscriber.getSubscriberUrl() + "/on_subscribe")
                        .method(HttpMethod.POST).inputFormat(InputFormat.JSON).input(input)
                        .header("Content-type", MimeType.APPLICATION_JSON.toString())
                        .header("Signature", Request.generateSignature(input.toString(), CryptoKey.find(Config.instance().getHostName() + ".k1").getPrivateKey())).getResponseAsJson();

                if (ObjectUtil.equals(response.get("answer"),otp.toString())){
                    subscriber.setStatus("SUBSCRIBED");
                }else {
                    subscriber.setStatus("UNSUBSCRIBED");
                }
                subscriber.save();


            }catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }

    }
}
