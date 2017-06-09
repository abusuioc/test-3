package busu.mvvm.fragment;

import android.app.Application;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.trello.rxlifecycle.android.FragmentEvent;

import busu.mvvm.FragmentLifecycleType;
import busu.mvvm.activity.ActivityResult;
import rx.Observable;
import rx.subjects.PublishSubject;

public class FragmentViewModel<ViewType extends FragmentLifecycleType> {

    private final Application mApp;
    private final Bundle mArguments;

    public FragmentViewModel(final @NonNull Application application, final @Nullable Bundle arguments) {
        mApp = application;
        mArguments = arguments;
        dropView();
    }

    protected final Application getApp() {
        return mApp;
    }

    protected final Bundle getArguments() {
        return mArguments;
    }

    private final PublishSubject<ViewType> viewChange = PublishSubject.create();
    private final Observable<ViewType> view = viewChange.filter(viewType -> viewType != null);

    private final PublishSubject<Bundle> arguments = PublishSubject.create();

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
        dropView();
    }

    @CallSuper
    protected void onDetach() {
        viewChange.onCompleted();
    }

    private void onTakeView(final @NonNull ViewType view) {
        viewChange.onNext(view);
    }

    private void dropView() {
        viewChange.onNext(null);
    }

    protected final
    @NonNull
    Observable<ViewType> view() {
        return view;
    }

    /**
     * By composing this transformer with an observable you guarantee that every observable in your view model
     * will be properly completed when the view model completes.
     * <p>
     * It is required that *every* observable in a view model do `.compose(bindToLifecycle())` before calling
     * `subscribe`.
     */
    protected final
    @NonNull
    <T> Observable.Transformer<T, T> bindToLifecycle() {
        return source -> source.takeUntil(
                view.switchMap(viewType -> viewType.lifecycle())
                        .filter(fragmentEvent -> FragmentEvent.DETACH.equals(fragmentEvent))
        );
    }

    private final PublishSubject<ActivityResult> activityResult = PublishSubject.create();

    @CallSuper
    public void inActivityResult(final @NonNull ActivityResult activityResult) {
        this.activityResult.onNext(activityResult);
    }

    protected Observable<ActivityResult> activityResult() {
        return activityResult;
    }

}
