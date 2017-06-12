package busu.test3;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;

import busu.mvvm.activity.ActivityViewModel;
import busu.test3.datasource.EndlessListDataSource;
import busu.test3.gbooks.BooksDataSource;
import rx.Observable;
import rx.subjects.PublishSubject;

public class MainAVM extends ActivityViewModel<MainActivity> {

    private BooksDataSource mBooksDS;


    public MainAVM(@NonNull Application application) {
        super(application);
        initDataSource();
        doInputToOutputWiring();
    }

    private void initDataSource() {
        EndlessListDataSource.Config cacheConfig = new EndlessListDataSource.DefaultConfig();
        mBooksDS = new BooksDataSource(cacheConfig, "cars");
    }

    private void doInputToOutputWiring() {
        openBookAtPosition
                .compose(bindToLifecycle())
                .map(position ->
                        getBooksDataSource().getDataAt(position))
                .filter(volume -> volume != null)
                .map(volume ->
                        DetailAVM.buildStartingIntent(getApp(), volume.getId()))
                .subscribe(startAnotherActivity);
    }


    //outputs
    public BooksDataSource getBooksDataSource() {
        return mBooksDS;
    }

    private final PublishSubject<Intent> startAnotherActivity = PublishSubject.create();

    public Observable<Intent> outstartAnotherActivity() {
        return startAnotherActivity.asObservable();
    }

    //inputs

    private final PublishSubject<Integer> openBookAtPosition = PublishSubject.create();

    public void inOpenBookAtPosition(int position) {
        openBookAtPosition.onNext(position);
    }


}
