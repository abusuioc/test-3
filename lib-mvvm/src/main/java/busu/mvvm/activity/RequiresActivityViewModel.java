package busu.mvvm.activity;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresActivityViewModel {
    Class<? extends ActivityViewModel> value();
}
