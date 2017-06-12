package busu.test3;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volumes;
import com.jakewharton.rxbinding.widget.RxAbsListView;
import com.jakewharton.rxbinding.widget.RxAdapterView;

import busu.mvvm.activity.BaseMvvmActivity;
import busu.mvvm.activity.RequiresActivityViewModel;
import busu.test3.endless.ItemClickSupport;
import busu.test3.gbooks.BooksListAdapter;

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
    }

    private void initVisuals() {
        ((SwipeRefreshLayout) findViewById(R.id.main_refresh)).setOnRefreshListener(this);
        mViewList = (RecyclerView) findViewById(R.id.main_list);
        mCacheStats = (TextView) findViewById(R.id.main_cache_stats);
    }

    private void initList() {
        mAdapter = new BooksListAdapter(viewModel().getBooksDataSource(), bindToLifecycle());
        mViewList.setLayoutManager(new LinearLayoutManager(this));
        mViewList.setAdapter(mAdapter);

        ItemClickSupport.addTo(mViewList)
                .setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                        viewModel().inOpenBookAtPosition(position);
                    }
                });
    }

    private void doSomeWiring() {
        viewModel().getBooksDataSource().updates()
                .compose(bindToLifecycle())
                .subscribe(result -> {
                    mCacheStats.setText("Cache stats > " + viewModel().getBooksDataSource().toString() + (result.isSuccessful() ? "" : " " + result.error()));
                });

        viewModel().outstartAnotherActivity()
                .compose(bindToLifecycle())
                .subscribe(intent -> startActivity(intent));
    }

    @Override
    public void onRefresh() {

    }
}
