package in.succinct.beckn.registry.controller;

import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;
import in.succinct.beckn.registry.db.model.City;
import in.succinct.beckn.registry.db.model.Country;

public class CountriesController extends BecknModelController<Country>{
    public CountriesController(Path path) {
        super(path);
    }


}
