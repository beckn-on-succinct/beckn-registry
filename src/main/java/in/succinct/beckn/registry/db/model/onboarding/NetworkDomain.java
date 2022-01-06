package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.HAS_DESCRIPTION_FIELD;
import com.venky.swf.db.model.Model;


@CONFIGURATION
@HAS_DESCRIPTION_FIELD("DESCRIPTION")
public interface NetworkDomain extends Model {
    @UNIQUE_KEY
    public String getName();
    public void setName(String name);

    public String getDescription();
    public void setDescription(String description);

    public static NetworkDomain find(String name){
        NetworkDomain domain = Database.getTable(NetworkDomain.class).newRecord();
        domain.setName(name);
        domain = Database.getTable(NetworkDomain.class).getRefreshed(domain);
        return domain;
    }
}
