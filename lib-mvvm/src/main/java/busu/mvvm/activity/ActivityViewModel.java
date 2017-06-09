package busu.mvvm.activity;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.trello.rxlifecycle.android.ActivityEvent;

import rx.Observable;
import rx.subjects.PublishSubject;


public class ActivityViewModel<ViewType extends ActivityLifecycleType> {

    private final Application mApp;

    protected final Application getApp() {
        return mApp;
    }


    private final PublishSubject<ViewType> viewChange = PublishSubject.create();
    private final Observable<ViewType> view = viewChange.filter(v -> v != null);

    private final PublishSubject<ActivityResult> activityResult = PublishSubject.create();

    private final PublishSubject<Intent> intent = PublishSubject.create();

    public ActivityViewModel(final @NonNull Application application) {
        mApp = application;
        dropView();
    }

    /**
     * Takes activity result data from the activity.
     */
    @CallSuper
    public void outActivityResult(final @NonNull ActivityResult activityResult) {
        this.activityResult.onNext(activityResult);
    }

    /**
     * Takes outIntent data from the Activity.
     */
    @CallSuper
    public void outIntent(final @NonNull Intent intent) {
        this.intent.onNext(intent);
    }

    @CallSuper
    protected void onResume(final @NonNull ViewType view) {
        onTakeView(view);
    }

    @CallSuper
    protected void onPause() {
        dropView();
    }

    @CallSuper
    protected void onDestroy() {
        viewChange.onCompleted();
    }

    private void onTakeView(final @NonNull ViewType view) {
        viewChange.onNext(view);
    }

    private void dropView() {
        viewChange.onNext(null);
    }

    /**
     * By composing this transformer with an observable you guarantee that every observable in your view model
     * will be properly completed when the view model completes.
     * <p>
     * It is required that *every* observable in a view model do `.compose(bindToLifecycle())` before calling
     * `subscribe`.
     */
    public
    @NonNull
    <T> Observable.Transformer<T, T> bindToLifecycle() {
        return source -> source.takeUntil(
                view.switchMap(v -> v.lifecycle().map(e -> Pair.create(v, e)))
                        .filter(pair -> pair.second == ActivityEvent.DESTROY && pair.first.isFinishing()));
    }

    //output
    protected
    final Observable<ActivityResult> outActivityResult() {
        return activityResult;
    }

    protected
    final Observable<Intent> outIntent() {
        return intent.filter(intent1 -> intent1 != null);
    }
}
