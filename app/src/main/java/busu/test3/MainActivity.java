package busu.test3;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volumes;

import java.util.ArrayList;
import java.util.List;

import busu.mvvm.activity.BaseMvvmActivity;
import busu.mvvm.activity.RequiresActivityViewModel;
import busu.test3.datasource.EndlessDataSource;

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

        EndlessDataSource.Config cacheConfig = new EndlessDataSource.DefaultConfig();
//        EndlessDataSource cache = new EndlessDataSource<String>(cacheConfig) {
//            @Override
//            protected List<String> doTheRequest(int startingIndex, int count) throws Throwable {
//                Thread.sleep(1000);
//                ArrayList<String> data = new ArrayList<>(count);
//                for (int i = 0; i < count; i++) {
//                    data.add("i_" + (startingIndex + i));
//                }
//                return data;
//            }
//        };

        EndlessDataSource dataSource = new EndlessDataSource<String>(cacheConfig) {

            private List<String> elm;

            @Override
            protected List<String> doTheRequest(int startingIndex, int count) throws Throwable {
                Log.i("DS", "do_req");
                if (elm == null) {
                    int size = cacheConfig.maxSize() + cacheConfig.pageSize() * 2;
                    elm = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        elm.add("i_" + i);
                    }
                }
                Thread.sleep(10000);
                ArrayList<String> data = new ArrayList<>(count);
                int howMuchToAdd = Math.min(count, elm.size() - startingIndex);
                for (int i = 0; i < howMuchToAdd; i++) {
                    data.add(elm.get(startingIndex + i));
                }
                return data;
            }
        };

        dataSource.updates().subscribe(o -> mCacheStats.setText("Cache stats > " + dataSource.toString()));

        mAdapter = new BooksListAdapter(dataSource);
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
