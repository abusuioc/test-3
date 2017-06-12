package busu.test3;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
public class MainActivity extends BaseMvvmActivity<MainAVM> {

    private RecyclerView mViewList;
    private BooksListAdapter mAdapter;
    private TextView mCacheStats;
    private SwipeRefreshLayout mRefresh;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initVisuals();
        initList();
        doSomeWiring();
    }

    private void initVisuals() {
        mRefresh = (SwipeRefreshLayout) findViewById(R.id.main_refresh);
        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefresh.setRefreshing(false);
                viewModel().inRefreshList();
            }
        });
        mViewList = (RecyclerView) findViewById(R.id.main_list);
        mCacheStats = (TextView) findViewById(R.id.main_cache_stats);
        //
        ((RadioGroup) findViewById(R.id.main_page_size)).setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton checkedBtn = (RadioButton) group.findViewById(checkedId);
            int pageSize = Integer.parseInt(checkedBtn.getText().toString());
            viewModel().getBooksDataSource().changePageSizeTo(pageSize);
        });
        ((RadioGroup) findViewById(R.id.main_cache_size)).setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton checkedBtn = (RadioButton) group.findViewById(checkedId);
            int cacheSize = Integer.parseInt(checkedBtn.getText().toString());
            viewModel().getBooksDataSource().changeCacheSizeTo(cacheSize);
        });
    }

    private void initList() {
        mAdapter = new BooksListAdapter(viewModel().getBooksDataSource(), bindToLifecycle());
        mViewList.setLayoutManager(new LinearLayoutManager(this));
        mViewList.setAdapter(mAdapter);

        ItemClickSupport.addTo(mViewList)
                .setOnItemClickListener((recyclerView, position, v) -> viewModel().inOpenBookAtPosition(position));
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

        viewModel().outNotifyListChange()
                .compose(bindToLifecycle())
                .subscribe(__ -> mAdapter.notifyDataSetChanged());
    }
}
