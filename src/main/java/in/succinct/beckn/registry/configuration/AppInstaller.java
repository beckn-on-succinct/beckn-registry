package in.succinct.beckn.registry.configuration;

import com.venky.core.security.Crypt;
import com.venky.core.string.StringUtil;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.routing.Config;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.City;
import in.succinct.beckn.registry.db.model.onboarding.NetworkParticipant;
import in.succinct.beckn.registry.db.model.onboarding.NetworkRole;
import in.succinct.beckn.registry.db.model.onboarding.ParticipantKey;

import java.security.KeyPair;
import java.sql.Timestamp;

public class AppInstaller implements Installer {

    public void install() {
        insertDefaultCities();
        generateBecknKeys();
    }

    private void insertDefaultCities() {
        Country country = Database.getTable(Country.class).newRecord();
        country.setName("India");
        country.setIsoCode("IND");
        country = Database.getTable(Country.class).getRefreshed(country);
        if (country.getRawRecord().isNewRecord()){
            country.save();
        }
        State state = Database.getTable(State.class).newRecord();
        state.setName("Karnataka");
        state.setCode("KA");
        state.setCountryId(country.getId());
        state = Database.getTable(State.class).getRefreshed(state);
        if (state.getRawRecord().isNewRecord()){
            state.save();
        }
        City city = Database.getTable(City.class).newRecord();
        city.setName("Bengaluru");
        city.setStateId(state.getId());
        city.setCode("std:080");
        city = Database.getTable(City.class).getRefreshed(city);
        if (city.getRawRecord().isNewRecord()) {
            city.save();
        }

    }



    private void generateBecknKeys() {

        CryptoKey key = CryptoKey.find(Config.instance().getHostName() + ".k1",CryptoKey.PURPOSE_SIGNING);
        if (key.getRawRecord().isNewRecord()){
            KeyPair pair = Crypt.getInstance().generateKeyPair(Request.SIGNATURE_ALGO,Request.SIGNATURE_ALGO_KEY_LENGTH);
            key.setAlgorithm(Request.SIGNATURE_ALGO);
            key.setPrivateKey(Crypt.getInstance().getBase64Encoded(pair.getPrivate()));
            key.setPublicKey(Crypt.getInstance().getBase64Encoded(pair.getPublic()));
            key.save();
        }

        CryptoKey encryptionKey = CryptoKey.find(Config.instance().getHostName() + ".k1",CryptoKey.PURPOSE_ENCRYPTION);
        if (encryptionKey.getRawRecord().isNewRecord()){
            KeyPair pair = Crypt.getInstance().generateKeyPair(Request.ENCRYPTION_ALGO,Request.ENCRYPTION_ALGO_KEY_LENGTH);
            encryptionKey.setAlgorithm(Request.ENCRYPTION_ALGO);
            encryptionKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(pair.getPrivate()));
            encryptionKey.setPublicKey(Crypt.getInstance().getBase64Encoded(pair.getPublic()));
            encryptionKey.save();
        }

        NetworkParticipant participant = NetworkParticipant.find(Config.instance().getHostName());
        if (participant.getRawRecord().isNewRecord()) {
            participant.save();
        }

        String keyId = String.format("%s.%s",participant.getParticipantId(),"k1");

        ParticipantKey participantKey = ParticipantKey.find(keyId);
        if (participantKey.getRawRecord().isNewRecord()){
            participantKey.setNetworkParticipantId(participant.getId());
            participantKey.setSigningPublicKey(key.getPublicKey());
            participantKey.setEncrPublicKey(encryptionKey.getPublicKey());
            participantKey.setValidFrom(new Timestamp(System.currentTimeMillis()));
            participantKey.setValidUntil(new Timestamp(participantKey.getValidFrom().getTime() + (long)(10L * 365.25D * 24L * 60L * 60L * 1000L))) ; //10 years
            participantKey.setVerified(true);
            participantKey.save();
        }
        String subscriberId = String.format("%s.%s.%s", StringUtil.valueOf(participant.getParticipantId()),"",NetworkRole.SUBSCRIBER_TYPE_LOCAL_REGISTRY);
        NetworkRole role = NetworkRole.find(subscriberId);
        if (role.getRawRecord().isNewRecord()){
            role.setNetworkParticipantId(participant.getId());
            role.setStatus(NetworkRole.SUBSCRIBER_STATUS_SUBSCRIBED);
            role.setType(NetworkRole.SUBSCRIBER_TYPE_LOCAL_REGISTRY);
            role.setUrl(Config.instance().getServerBaseUrl()+"/subscribers");
            role.save();
        }
    }

}

