package in.succinct.beckn.registry.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.model.Model;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;

public class BecknModelController<M extends Model> extends ModelController<M> {

    public BecknModelController(Path path) {
        super(path);
    }

    @Override
    @RequireLogin(false)
    public View search() {
        return super.search();
    }

    @Override
    @RequireLogin(false)
    public View search(String strQuery) {
        return super.search(strQuery);
    }

    @Override
    @RequireLogin(false)
    public View index(){
        return super.index();
    }
}
