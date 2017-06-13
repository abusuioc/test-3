package busu.test3;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;

import busu.mvvm.activity.ActivityViewModel;
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
        mBooksDS = new BooksDataSource();
        mBooksDS.changePageSizeTo(40);
        mBooksDS.changeCacheSizeTo(200);
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
        refreshList
                .compose(bindToLifecycle())
                .doOnNext(__ -> mBooksDS.clearCache())
                .subscribe(notifyListChange);
    }


    //outputs
    public BooksDataSource getBooksDataSource() {
        return mBooksDS;
    }

    private final PublishSubject<Intent> startAnotherActivity = PublishSubject.create();

    public Observable<Intent> outstartAnotherActivity() {
        return startAnotherActivity.asObservable();
    }

    private final PublishSubject<Void> notifyListChange = PublishSubject.create();

    public Observable<Void> outNotifyListChange() {
        return notifyListChange.asObservable();
    }

    //inputs

    private final PublishSubject<Integer> openBookAtPosition = PublishSubject.create();

    public void inOpenBookAtPosition(int position) {
        openBookAtPosition.onNext(position);
    }

    private final PublishSubject<Void> refreshList = PublishSubject.create();

    public void inRefreshList() {
        refreshList.onNext(null);
    }


}
