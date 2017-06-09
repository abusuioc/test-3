package busu.mvvm.activity;

import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ActivityViewModelManager {
    private static final String VIEW_MODEL_ID_KEY = "view_model_id";

    private static final ActivityViewModelManager instance = new ActivityViewModelManager();
    private Map<String, ActivityViewModel> viewModels = new HashMap<>();

    public static
    @NonNull
    ActivityViewModelManager getInstance() {
        return instance;
    }

    public <T extends ActivityViewModel> T fetch(final @NonNull Application app, final @NonNull Class<T> viewModelClass,
                                                 final @Nullable Bundle savedInstanceState) {
        final String id = fetchId(savedInstanceState);
        ActivityViewModel activityViewModel = viewModels.get(id);

        if (activityViewModel == null) {
            activityViewModel = create(app, viewModelClass, id);
        }

        return (T) activityViewModel;
    }

    public void destroy(final @NonNull ActivityViewModel activityViewModel) {
        activityViewModel.onDestroy();

        final Iterator<Map.Entry<String, ActivityViewModel>> iterator = viewModels.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, ActivityViewModel> entry = iterator.next();
            if (activityViewModel.equals(entry.getValue())) {
                iterator.remove();
            }
        }
    }

    public void save(final @NonNull ActivityViewModel activityViewModel, final @NonNull Bundle envelope) {
        envelope.putString(VIEW_MODEL_ID_KEY, findIdForViewModel(activityViewModel));
    }

    private <T extends ActivityViewModel> ActivityViewModel create(final @NonNull Application app, final @NonNull Class<T> viewModelClass, final @NonNull String id) {

        final ActivityViewModel activityViewModel;

        try {
            final Constructor constructor = viewModelClass.getConstructor(Application.class);
            activityViewModel = (ActivityViewModel) constructor.newInstance(app);

            // Need to catch these exceptions separately, otherwise the compiler turns them into `ReflectiveOperationException`.
            // That exception is only available in API19+
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        } catch (InvocationTargetException exception) {
            throw new RuntimeException(exception);
        } catch (InstantiationException exception) {
            throw new RuntimeException(exception);
        } catch (NoSuchMethodException exception) {
            throw new RuntimeException(exception);
        }

        viewModels.put(id, activityViewModel);

        return activityViewModel;
    }

    private String fetchId(final @Nullable Bundle savedInstanceState) {
        return savedInstanceState != null ?
                savedInstanceState.getString(VIEW_MODEL_ID_KEY) :
                UUID.randomUUID().toString();
    }

    private String findIdForViewModel(final @NonNull ActivityViewModel activityViewModel) {
        for (final Map.Entry<String, ActivityViewModel> entry : viewModels.entrySet()) {
            if (activityViewModel.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        throw new RuntimeException("Cannot find view model in map!");
    }
}