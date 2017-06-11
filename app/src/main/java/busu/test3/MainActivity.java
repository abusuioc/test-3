package busu.test3;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volumes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import busu.mvvm.activity.BaseMvvmActivity;
import busu.mvvm.activity.RequiresActivityViewModel;
import busu.test3.datasource.EndlessListDataSource;

@RequiresActivityViewModel(MainAVM.class)
public class MainActivity extends BaseMvvmActivity<MainAVM> implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView mViewList;
    private BooksListAdapter mAdapter;
    private TextView mCacheStats;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initVisuals();
        initList();

        new Thread(new Runnable() {
            @Override
            public void run() {
                makeDemoBooksCall();
            }
        }).start();


    }

    private void initVisuals() {
        ((SwipeRefreshLayout) findViewById(R.id.main_refresh)).setOnRefreshListener(this);
        mViewList = (RecyclerView) findViewById(R.id.main_list);
        mCacheStats = (TextView) findViewById(R.id.main_cache_stats);
    }

    private void initList() {

        EndlessListDataSource.Config cacheConfig = new EndlessListDataSource.DefaultConfig();
        EndlessListDataSource<String> testEndlessDS = new EndlessListDataSource<String>(cacheConfig) {
            @Override
            protected List<String> doTheRequest(@Nonnull WorkRequest request) throws Throwable {
                Thread.sleep(1000);
                ArrayList<String> data = new ArrayList<>(request.count());
                for (int i = 0; i < request.count(); i++) {
                    data.add("endless_" + (request.from() + i));
                }
                return data;
            }
        };

        EndlessListDataSource<String> testFixedSizeDS = new EndlessListDataSource<String>(cacheConfig) {

            private List<String> elm;

            @Override
            protected List<String> doTheRequest(@Nonnull WorkRequest request) throws Throwable {
                if (elm == null) {
                    int size = cacheConfig.keepSize() + cacheConfig.pageSize() * 2;
                    elm = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        elm.add("fixed_" + i);
                    }
                }
                Thread.sleep(1000);
                ArrayList<String> data = new ArrayList<>(request.count());
                int howMuchToAdd = Math.min(request.count(), elm.size() - request.from());
                for (int i = 0; i < howMuchToAdd; i++) {
                    data.add(elm.get(request.from() + i));
                }
                return data;
            }
        };

        EndlessListDataSource<String> testErroringDS = new EndlessListDataSource<String>(cacheConfig) {

            private List<String> elm;

            @Override
            protected List<String> doTheRequest(@Nonnull WorkRequest request) throws Throwable {
                if (elm == null) {
                    int size = cacheConfig.keepSize() + cacheConfig.pageSize() * 2;
                    elm = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        elm.add("erroring_" + i);
                    }
                }
                Thread.sleep(1000);
                ArrayList<String> data = new ArrayList<>(request.count());
                int howMuchToAdd = Math.min(request.count(), elm.size() - request.from());
                for (int i = 0; i < howMuchToAdd; i++) {
                    data.add(elm.get(request.from() + i));
                }
                if (request.from() > cacheConfig.pageSize()) {
                    throw new Throwable("error fetching");
                }
                return data;
            }
        };

        testFixedSizeDS.updates().subscribe(result -> {
            mCacheStats.setText("Cache stats > " + testFixedSizeDS.toString() + (result.isSuccessful() ? "" : " " + result.error()));
        });


        mAdapter = new BooksListAdapter(testFixedSizeDS);
        mViewList.setLayoutManager(new LinearLayoutManager(this));
        mViewList.setAdapter(mAdapter);
    }

    @Override
    public void onRefresh() {

    }


    private void makeDemoBooksCall() {
        try {
            final String APPLICATION_NAME = "Busu-Test3/1.0";
            final String QUERY = "007";

            // Set up Books client.
            final Books books = new Books.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), null)
                    .setApplicationName(APPLICATION_NAME)
                    .setGoogleClientRequestInitializer(new BooksRequestInitializer(BuildConfig.BOOKS_API_KEY))
                    .build();

            Books.Volumes.List volumesList = books.volumes().list(QUERY);
//            volumesList.setFilter("ebooks");

            // Execute the query.
            Volumes volumes = volumesList.execute();
            final int totalItems = volumes.getTotalItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
