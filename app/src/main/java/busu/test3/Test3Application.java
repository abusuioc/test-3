package busu.test3;

import android.app.Application;

import com.squareup.picasso.Picasso;

/**
 * TODO: add a class header comment!
 */

public class Test3Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initPicasso();
    }

    private void initPicasso() {
        Picasso picasso = Picasso.with(this);
        if (BuildConfig.DEBUG) {
            picasso.setIndicatorsEnabled(true);
        }
    }
}
