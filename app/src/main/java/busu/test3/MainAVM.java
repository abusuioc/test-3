package busu.test3;

import android.app.Application;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import busu.mvvm.activity.ActivityViewModel;
import busu.test3.datasource.EndlessListDataSource;


public class MainAVM extends ActivityViewModel<MainActivity> {

    private EndlessListDataSource<String> mDS;


    public MainAVM(@NonNull Application application) {
        super(application);

        initDataSource();
    }

    private void initDataSource() {
        EndlessListDataSource.Config cacheConfig = new EndlessListDataSource.DefaultConfig();
        mDS = produceTestingLimitedElementsDS(cacheConfig);
    }


    //outputs
    public EndlessListDataSource<String> getDataSource() {
        return mDS;
    }


    //some testing data sources - todo : this is temp
    private EndlessListDataSource<String> produceTestingEndlessDS(
            @NonNull EndlessListDataSource.Config config) {
        return new EndlessListDataSource<String>(config) {
            @Override
            protected List<String> doTheRequest(@Nonnull WorkRequest request) throws Throwable {
                Thread.sleep(2000);
                ArrayList<String> data = new ArrayList<>(request.count());
                for (int i = 0; i < request.count(); i++) {
                    data.add("endless_" + (request.from() + i));
                }
                return data;
            }
        };
    }

    private EndlessListDataSource<String> produceTestingLimitedElementsDS(
            @NonNull EndlessListDataSource.Config config) {
        return new EndlessListDataSource<String>(config) {

            private List<String> elm;

            @Override
            protected List<String> doTheRequest(@Nonnull WorkRequest request) throws Throwable {
                if (elm == null) {
                    int size = config.keepSize() + config.pageSize() * 2;
                    elm = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        elm.add("fixed_" + i);
                    }
                }
                Thread.sleep(2000);
                ArrayList<String> data = new ArrayList<>(request.count());
                int howMuchToAdd = Math.min(request.count(), elm.size() - request.from());
                for (int i = 0; i < howMuchToAdd; i++) {
                    data.add(elm.get(request.from() + i));
                }
                return data;
            }
        };
    }

    private EndlessListDataSource<String> produceTestingErroringDS(
            @NonNull EndlessListDataSource.Config config) {
        return new EndlessListDataSource<String>(config) {

            private List<String> elm;

            @Override
            protected List<String> doTheRequest(@Nonnull WorkRequest request) throws Throwable {
                if (elm == null) {
                    int size = config.keepSize() + config.pageSize() * 2;
                    elm = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        elm.add("erroring_" + i);
                    }
                }
                Thread.sleep(2000);
                ArrayList<String> data = new ArrayList<>(request.count());
                int howMuchToAdd = Math.min(request.count(), elm.size() - request.from());
                for (int i = 0; i < howMuchToAdd; i++) {
                    data.add(elm.get(request.from() + i));
                }
                if (request.from() > config.pageSize()) {
                    throw new Throwable("error fetching");
                }
                return data;
            }
        };
    }
}
