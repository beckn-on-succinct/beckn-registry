package in.succinct.beckn.registry.db.model;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;

public interface City extends com.venky.swf.plugins.collab.db.model.config.City {
    @IS_NULLABLE
    @UNIQUE_KEY("K3")
    public String getCode();
    public void setCode(String code);

}
