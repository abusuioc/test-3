package busu.mvvm.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.android.FragmentEvent;
import com.trello.rxlifecycle.android.RxLifecycleAndroid;

import busu.mvvm.FragmentLifecycleProvider;
import busu.mvvm.FragmentLifecycleType;
import busu.mvvm.activity.ActivityResult;
import rx.Observable;
import rx.subjects.BehaviorSubject;

public abstract class BaseMvvmFragment<ViewModelType extends FragmentViewModel> extends Fragment implements FragmentLifecycleProvider,
        FragmentLifecycleType {

    private boolean mHasBeenStopped = false;
    public boolean isAfterOrientationChange;
    //compensate for the weird bug when fragment is in the backstack and app is twice restored; the only reliable Bundle is the one obtained via onCreate(); use this one.
    private Bundle mSavedInstanceState;

    private final BehaviorSubject<FragmentEvent> lifecycle = BehaviorSubject.create();
    private static final String VIEW_MODEL_KEY = "FragmentViewModel";
    private ViewModelType viewModel;


    /**
     * Can be null if no VM is specified or the calling time is too early.
     * The idea was to check these cases and throw helping exceptions, but it might have an impact on performance and/or readability.
     *
     * @return the VM of this fragment
     */
    @Nullable
    public ViewModelType viewModel() {
        return viewModel;
    }

    /**
     * Returns an observable of the fragment's lifecycle events.
     */
    @Override
    public final
    @NonNull
    Observable<FragmentEvent> lifecycle() {
        return lifecycle.asObservable();
    }

    /**
     * Completes an observable when an {@link FragmentEvent} occurs in the fragment's lifecycle.
     */
    @Override
    public final
    @NonNull
    <T> Observable.Transformer<T, T> bindUntilEvent(final @NonNull FragmentEvent event) {
        return RxLifecycle.bindUntilEvent(lifecycle, event);
    }

    /**
     * Completes an observable when the lifecycle event opposing the current lifecyle event is emitted.
     * For example, if a subscription is made during {@link FragmentEvent#CREATE}, the observable will be completed
     * in {@link FragmentEvent#DESTROY}.
     */
    @Override
    public final
    @NonNull
    <T> Observable.Transformer<T, T> bindToLifecycle() {
        return RxLifecycleAndroid.bindFragment(lifecycle);
    }

    /**
     * Called before `onCreate`, when a fragment is attached to its context.
     */
    @CallSuper
    @Override
    public void onAttach(final @NonNull Context context) {
        super.onAttach(context);
        lifecycle.onNext(FragmentEvent.ATTACH);
    }

    @CallSuper
    @Override
    public void onCreate(final @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSavedInstanceState = savedInstanceState;
        isAfterOrientationChange = !(savedInstanceState == null);
        lifecycle.onNext(FragmentEvent.CREATE);
        assignViewModel(savedInstanceState);
    }

    /**
     * Called when a fragment instantiates its user interface view, between `onCreate` and `onActivityCreated`.
     * Can return null for non-graphical fragments.
     */
    @CallSuper
    @Override
    public
    @Nullable
    View onCreateView(final @NonNull LayoutInflater inflater, final @Nullable ViewGroup container,
                      final @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layoutId(), container, false);
        lifecycle.onNext(FragmentEvent.CREATE_VIEW);
        return view;
    }

    @Override
    public final void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initFragment(mSavedInstanceState);
    }

    @CallSuper
    @Override
    public void onStart() {
        super.onStart();
        if (mHasBeenStopped) {
            //reset the orientation change if the fragment was redisplayed without being recreated
            isAfterOrientationChange = false;
            onCameBack();
        }
        lifecycle.onNext(FragmentEvent.START);
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        lifecycle.onNext(FragmentEvent.RESUME);
        assignViewModel(null);
        if (viewModel != null) {
            viewModel.onResume(this);
        }
    }

    @CallSuper
    @Override
    public void onPause() {
        lifecycle.onNext(FragmentEvent.PAUSE);
        super.onPause();
        if (viewModel != null) {
            viewModel.onPause();
        }
    }

    @CallSuper
    @Override
    public void onStop() {
        lifecycle.onNext(FragmentEvent.STOP);
        super.onStop();
        mHasBeenStopped = true;
    }

    /**
     * Called when the view created by `onCreateView` has been detached from the fragment.
     * The lifecycle subject must be pinged before it is destroyed by the fragment.
     */
    @CallSuper
    @Override
    public void onDestroyView() {
        lifecycle.onNext(FragmentEvent.DESTROY_VIEW);
        super.onDestroyView();
    }

    @CallSuper
    @Override
    public void onDestroy() {
        lifecycle.onNext(FragmentEvent.DESTROY);
        super.onDestroy();
        if (viewModel != null) {
            viewModel.onDestroy();
        }
    }

    /**
     * Called after `onDestroy` when the fragment is no longer attached to its activity.
     */
    @CallSuper
    @Override
    public void onDetach() {
        super.onDetach();
        if (getActivity().isFinishing()) {
            if (viewModel != null) {
                // Order of the next two lines is important: the lifecycle should update before we
                // complete the view publish subject in the view model.
                lifecycle.onNext(FragmentEvent.DETACH);
                viewModel.onDetach();

                FragmentViewModelManager.getInstance().destroy(viewModel);
                viewModel = null;
            }
        }
    }

    @Override
    public final void onSaveInstanceState(final @NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (getView() == null) {
            //forward the bundle (bug fix when this is a background fragment and rotation occurs, just onCreate is called)
            if (mSavedInstanceState != null) {
                outState.putAll(mSavedInstanceState);
            }
        } else {
            saveInstanceState(outState);

            final Bundle viewModelEnvelope = new Bundle();
            if (viewModel != null) {
                FragmentViewModelManager.getInstance().save(viewModel, viewModelEnvelope);
            }

            outState.putBundle(VIEW_MODEL_KEY, viewModelEnvelope);
        }
    }

    private void assignViewModel(final @Nullable Bundle viewModelEnvelope) {
        if (viewModel == null) {
            final RequiresFragmentViewModel annotation = getClass().getAnnotation(RequiresFragmentViewModel.class);
            final Class<ViewModelType> viewModelClass = annotation == null ? null : (Class<ViewModelType>) annotation.value();
            if (viewModelClass != null) {
                viewModel = FragmentViewModelManager.getInstance().fetch(getActivity().getApplication(),
                        viewModelClass, (viewModelEnvelope == null ? null : viewModelEnvelope.getBundle(VIEW_MODEL_KEY)), getArguments());
            }
        }
    }

    @LayoutRes
    protected abstract int layoutId();

    /**
     * This is the only method to call to initialize fragment's view hierarchy obtained with getView() + any other initialization such as viewModel plugging in
     *
     * @param savedInstanceState
     */
    protected abstract void initFragment(Bundle savedInstanceState);

    /**
     * Implement this to save the state.
     *
     * @param outState
     */
    protected abstract void saveInstanceState(Bundle outState);

    protected void onCameBack() {
    }

    /**
     * Can be called by the Activity on a back press. The Fragment can handle this event and tell
     * the activity that it handled it by returning true.
     *
     * @return true if fragment handled the back event and therefore the activity should not execute
     * its back funcionality.
     */

    @CallSuper
    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        viewModel.inActivityResult(ActivityResult.create(requestCode, resultCode, intent));
    }

}

