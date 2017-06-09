package busu.mvvm.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;

import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.android.ActivityEvent;
import com.trello.rxlifecycle.android.RxLifecycleAndroid;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class BaseMvvmActivity<ViewModelType extends ActivityViewModel> extends AppCompatActivity implements ActivityLifecycleProvider,
        ActivityLifecycleType {
    private final PublishSubject<Void> back = PublishSubject.create();
    private final BehaviorSubject<ActivityEvent> lifecycle = BehaviorSubject.create();
    private static final String VIEW_MODEL_KEY = "viewModel";
    private ViewModelType viewModel;

    /**
     * Can be null if no VM is specified or the calling time is too early.
     * The idea was to check these cases and throw helping exceptions, but it might have an impact on performance and/or readability.
     *
     * @return the VM of this activity
     */
    public ViewModelType viewModel() {
        return viewModel;
    }

    /**
     * Returns an observable of the activity's lifecycle events.
     */
    public final Observable<ActivityEvent> lifecycle() {
        return lifecycle.asObservable();
    }

    /**
     * Completes an observable when an {@link ActivityEvent} occurs in the activity's lifecycle.
     */
    public final <T> Observable.Transformer<T, T> bindUntilEvent(final ActivityEvent event) {
        return RxLifecycle.bindUntilEvent(lifecycle, event);
    }

    /**
     * Completes an observable when the lifecycle event opposing the current lifecyle event is emitted.
     * For example, if a subscription is made during {@link ActivityEvent#CREATE}, the observable will be completed
     * in {@link ActivityEvent#DESTROY}.
     */
    public final <T> Observable.Transformer<T, T> bindToLifecycle() {
        return RxLifecycleAndroid.bindActivity(lifecycle);
    }

    @CallSuper
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (viewModel != null) {
            viewModel.outActivityResult(ActivityResult.create(requestCode, resultCode, intent));
        }

    }

    @CallSuper
    @Override
    protected void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lifecycle.onNext(ActivityEvent.CREATE);

        assignViewModel(savedInstanceState);

        if (viewModel != null && savedInstanceState == null) {
            viewModel.outIntent(getIntent());
        }
    }

    /**
     * Called when an activity is set to `singleTop` and it is relaunched while at the top of the activity stack.
     */
    @CallSuper
    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        if (viewModel != null) {
            viewModel.outIntent(intent);
        }
    }

    @CallSuper
    @Override
    protected void onStart() {
        super.onStart();
        lifecycle.onNext(ActivityEvent.START);

        back.compose(bindUntilEvent(ActivityEvent.STOP))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(__ -> doWhenBackInvoked());
    }

    @CallSuper
    @Override
    protected void onResume() {
        super.onResume();
        lifecycle.onNext(ActivityEvent.RESUME);

        assignViewModel(null);
        if (viewModel != null) {
            viewModel.onResume(this);
        }
    }

    @CallSuper
    @Override
    protected void onPause() {
        lifecycle.onNext(ActivityEvent.PAUSE);
        super.onPause();

        if (viewModel != null) {
            viewModel.onPause();
        }
    }

    @CallSuper
    @Override
    protected void onStop() {
        lifecycle.onNext(ActivityEvent.STOP);
        super.onStop();
    }

    @CallSuper
    @Override
    protected void onDestroy() {
        lifecycle.onNext(ActivityEvent.DESTROY);
        super.onDestroy();

        if (isFinishing()) {
            if (viewModel != null) {
                ActivityViewModelManager.getInstance().destroy(viewModel);
                viewModel = null;
            }
        } else {
        }
    }

    /**
     * @deprecated Use {@link #back()} instead.
     * <p>
     * In rare situations, onBackPressed can be triggered after {@link #onSaveInstanceState(Bundle)} has been called.
     * This causes an {@link IllegalStateException} in the fragment manager's `checkStateLoss` method, because the
     * UI state has changed after being saved. The sequence of events might look like this:
     * <p>
     * onSaveInstanceState -> onStop -> onBackPressed
     * <p>
     * To avoid that situation, we need to ignore calls to `onBackPressed` after the activity has been saved. Since
     * the activity is stopped after `onSaveInstanceState` is called, we can create an observable of back events,
     * and a subscription that calls super.onBackPressed() only when the activity has not been stopped.
     */
    @Override
    @Deprecated
    public final void onBackPressed() {
        back();
    }

    /**
     * Call when the user triggers a back event, e.g. clicking back in a toolbar or pressing the device back button.
     */
    public void back() {
        back.onNext(null);
    }

    /**
     * Override in subclasses for custom exit transitions. First item in pair is the enter animation,
     * second item in pair is the exit animation.
     */
    protected
    @Nullable
    Pair<Integer, Integer> exitTransition() {
        return null;
    }

    @CallSuper
    @Override
    protected void onSaveInstanceState(final @NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        final Bundle viewModelEnvelope = new Bundle();
        if (viewModel != null) {
            ActivityViewModelManager.getInstance().save(viewModel, viewModelEnvelope);
        }

        outState.putBundle(VIEW_MODEL_KEY, viewModelEnvelope);
    }

    /**
     * Triggers a back press with an optional transition.
     */
    @CallSuper
    protected void doWhenBackInvoked() {
        super.onBackPressed();

        final Pair<Integer, Integer> exitTransitions = exitTransition();
        if (exitTransitions != null) {
            overridePendingTransition(exitTransitions.first, exitTransitions.second);
        }
    }

    private final void assignViewModel(final @Nullable Bundle viewModelEnvelope) {
        if (viewModel == null) {
            final RequiresActivityViewModel annotation = getClass().getAnnotation(RequiresActivityViewModel.class);
            final Class<ViewModelType> viewModelClass = annotation == null ? null : (Class<ViewModelType>) annotation.value();
            if (viewModelClass != null) {
                viewModel = ActivityViewModelManager.getInstance().fetch(getApplication(),
                        viewModelClass,
                        (viewModelEnvelope == null ? null :
                                viewModelEnvelope.getBundle(VIEW_MODEL_KEY)));
            }
        }
    }
}
