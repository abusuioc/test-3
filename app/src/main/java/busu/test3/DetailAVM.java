package busu.test3;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.api.services.books.Books;
import com.google.api.services.books.model.Volume;

import java.io.IOException;

import busu.mvvm.activity.ActivityViewModel;
import busu.test3.gbooks.BooksUtilities;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class DetailAVM extends ActivityViewModel<DetailActivity> {

    private final static String EXTRA_VOL_ID = "vid";

    public final static Intent buildStartingIntent(Context context, String volumeId) {
        return new Intent(context, DetailActivity.class)
                .putExtra(EXTRA_VOL_ID, volumeId);
    }

    public DetailAVM(@NonNull Application application) {
        super(application);
        doInputToOutputWiring();
    }

    private void doInputToOutputWiring() {
        outIntent()
                .compose(bindToLifecycle())
                .map(intent -> intent.getStringExtra(EXTRA_VOL_ID))
                .filter(volumeId -> volumeId != null)
                .observeOn(Schedulers.io())
                .map(volumeId -> {
                    try {
                        return requestVolume(volumeId);
                    } catch (IOException ioex) {
                        throw Exceptions.propagate(ioex);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Volume>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        error.onNext(e);
                    }

                    @Override
                    public void onNext(Volume volData) {
                        volume.onNext(volData);
                    }
                });
    }

    private Volume requestVolume(@NonNull String volumeId) throws IOException {
        Books books = BooksUtilities.setupBooksClient();
        return books.volumes().get(volumeId).execute();
    }

    //outputs
    private final BehaviorSubject<Volume> volume = BehaviorSubject.create();

    public Observable<Volume> outVolumeLoaded() {
        return volume.asObservable();
    }

    //errors
    private final BehaviorSubject<Throwable> error = BehaviorSubject.create();

    public Observable<Throwable> outError() {
        return error.asObservable();
    }

}
