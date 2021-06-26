package in.succinct.beckn.registry.db.model;

import com.venky.swf.db.annotations.column.COLUMN_NAME;
import com.venky.swf.db.annotations.column.IS_NULLABLE;

public interface Country extends com.venky.swf.plugins.collab.db.model.config.Country {
    @COLUMN_NAME("ISO_CODE")
    @IS_NULLABLE
    public String getCode();
    public void setCode(String code);

}
