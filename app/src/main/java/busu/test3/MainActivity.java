package busu.test3;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import busu.mvvm.activity.BaseMvvmActivity;
import busu.mvvm.activity.RequiresActivityViewModel;
import busu.test3.datasource.EndlessListDataSource;
import busu.test3.endless.ItemClickSupport;
import busu.test3.gbooks.BooksListAdapter;

@RequiresActivityViewModel(MainAVM.class)
public class MainActivity extends BaseMvvmActivity<MainAVM> {

    private RecyclerView mViewList;
    private BooksListAdapter mAdapter;
    private TextView mCacheStats;
    private SwipeRefreshLayout mRefresh;
    private CheckBox mPreemptiveScroll;

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
        mRefresh.setOnRefreshListener(() -> {
            mRefresh.setRefreshing(false);
            viewModel().inRefreshList();
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
        mPreemptiveScroll = (CheckBox) findViewById(R.id.main_preemptive);
    }

    private void initList() {
        mAdapter = new BooksListAdapter(viewModel().getBooksDataSource(), bindToLifecycle());
        mViewList.setLayoutManager(new LinearLayoutManager(this));
        mViewList.setAdapter(mAdapter);
        ItemClickSupport.addTo(mViewList)
                .setOnItemClickListener((recyclerView, position, v) -> viewModel().inOpenBookAtPosition(position));
        mViewList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mPreemptiveScroll.isChecked()) {
                    int firstVisiblePosition = RecyclerViewUtil.getFirstVisiblePosition(recyclerView);
                    int lastVisiblePosition = RecyclerViewUtil.getLastVisiblePosition(recyclerView);
                    final boolean isScrollingDown = dy > 0;
                    int relevantPosition = firstVisiblePosition;
                    EndlessListDataSource.DIRECTION direction = EndlessListDataSource.DIRECTION.FRONT;
                    if (isScrollingDown) {
                        relevantPosition = lastVisiblePosition;
                        direction = EndlessListDataSource.DIRECTION.END;
                    }
                    viewModel().getBooksDataSource()
                            .requestNewPageOfDataBasedOnCurrentPosition(direction, relevantPosition);
                }
            }
        });
    }

    private void doSomeWiring() {
        viewModel().getBooksDataSource().updates()
                .compose(bindToLifecycle())
                .subscribe(result ->
                        mCacheStats.setText(result.isSuccessful() ? ("Cache stats > " + viewModel().getBooksDataSource().toString()) : ("Error: \n" + result.error())));

        viewModel().outstartAnotherActivity()
                .compose(bindToLifecycle())
                .subscribe(intent -> startActivity(intent));

        viewModel().outNotifyListChange()
                .compose(bindToLifecycle())
                .subscribe(__ -> mAdapter.notifyDataSetChanged());
    }
}
