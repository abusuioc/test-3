package busu.test3;

import android.app.Application;
import android.support.annotation.NonNull;

import busu.mvvm.activity.ActivityViewModel;

public class DetailAVM extends ActivityViewModel<DetailActivity> {
    public DetailAVM(@NonNull Application application) {
        super(application);
    }
}
