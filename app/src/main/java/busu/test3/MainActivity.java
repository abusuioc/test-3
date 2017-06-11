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
        doSomeWiring();
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
        mAdapter = new BooksListAdapter(viewModel().getDataSource(), bindToLifecycle());
        mViewList.setLayoutManager(new LinearLayoutManager(this));
        mViewList.setAdapter(mAdapter);
    }

    private void doSomeWiring() {
        viewModel().getDataSource().updates()
                .compose(bindToLifecycle())
                .subscribe(result -> {
                    mCacheStats.setText("Cache stats > " + viewModel().getDataSource().toString() + (result.isSuccessful() ? "" : " " + result.error()));
                });
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
