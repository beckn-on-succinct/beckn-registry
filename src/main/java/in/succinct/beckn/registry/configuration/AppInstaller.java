package in.succinct.beckn.registry.configuration;

import com.venky.core.security.Crypt;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.CryptoKey;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.State;
import com.venky.swf.routing.Config;
import in.succinct.beckn.Request;
import in.succinct.beckn.registry.db.model.City;
import in.succinct.beckn.registry.db.model.Subscriber;

import java.security.KeyPair;
import java.sql.Timestamp;

import static com.venky.swf.plugins.collab.db.model.config.City.findByCountryAndStateAndName;

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

        CryptoKey key = Database.getTable(CryptoKey.class).newRecord();
        key.setAlias(Config.instance().getHostName() + ".k1");
        key = Database.getTable(CryptoKey.class).getRefreshed(key);
        if (key.getRawRecord().isNewRecord()){
            KeyPair pair = Crypt.getInstance().generateKeyPair(Request.SIGNATURE_ALGO,Request.SIGNATURE_ALGO_KEY_LENGTH);
            key.setPrivateKey(Crypt.getInstance().getBase64Encoded(pair.getPrivate()));
            key.setPublicKey(Crypt.getInstance().getBase64Encoded(pair.getPublic()));
            key.save();
        }

        CryptoKey encryptionKey = Database.getTable(CryptoKey.class).newRecord();
        encryptionKey.setAlias(Config.instance().getHostName() + ".encrypt.k1");
        encryptionKey = Database.getTable(CryptoKey.class).getRefreshed(encryptionKey);
        if (encryptionKey.getRawRecord().isNewRecord()){
            KeyPair pair = Crypt.getInstance().generateKeyPair(Request.ENCRYPTION_ALGO,Request.ENCRYPTION_ALGO_KEY_LENGTH);
            encryptionKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(pair.getPrivate()));
            encryptionKey.setPublicKey(Crypt.getInstance().getBase64Encoded(pair.getPublic()));
            encryptionKey.save();
        }

        Subscriber subscriber = Database.getTable(Subscriber.class).newRecord();
        subscriber.setSubscriberId(Config.instance().getHostName());
        subscriber.setCountryId(Country.findByName("India").getId());
        subscriber.setCityId(findByCountryAndStateAndName("India","Karnataka","Bengaluru").getId());
        subscriber = Database.getTable(Subscriber.class).getRefreshed(subscriber);
        if (subscriber.getRawRecord().isNewRecord()){
            subscriber.setType("lreg");
            subscriber.setStatus("SUBSCRIBED");
            subscriber.setSubscriberUrl(Config.instance().getServerBaseUrl()+"/subscribers");
            subscriber.setDomain("nic2004:52110");
            subscriber.setSigningPublicKey(key.getPublicKey());
            subscriber.setEncrPublicKey(encryptionKey.getPublicKey());
            subscriber.setValidFrom(new Timestamp(System.currentTimeMillis()));
            subscriber.setValidUntil(new Timestamp(subscriber.getValidFrom().getTime() + (long)(10L * 365.25D * 24L * 60L * 60L * 1000L))) ; //10 years
            subscriber.save();
        }

    }

}

