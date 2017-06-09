package busu.test3;

import android.app.Application;
import android.support.annotation.NonNull;

import busu.mvvm.activity.ActivityViewModel;


public class MainAVM extends ActivityViewModel<MainActivity> {
    public MainAVM(@NonNull Application application) {
        super(application);
    }
}
