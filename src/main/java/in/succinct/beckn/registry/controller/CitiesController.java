package in.succinct.beckn.registry.controller;

import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;
import in.succinct.beckn.registry.db.model.City;

public class CitiesController extends BecknModelController<City>{
    public CitiesController(Path path) {
        super(path);
    }


}
