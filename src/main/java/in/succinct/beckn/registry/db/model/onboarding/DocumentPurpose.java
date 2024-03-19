package in.succinct.beckn.registry.db.model.onboarding;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;


public interface DocumentPurpose extends Model {
    @UNIQUE_KEY
    public String getName();
    public void setName(String name);

    public boolean isRequiredForKyc();
    public void setRequiredForKyc(boolean requiredForKyc);

}
