package busu.test3.endless;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import busu.test3.R;
import busu.test3.datasource.EndlessListDataSource;

/**
 * TODO: add class header
 */

public class EndlessListAdapter<Data> extends RecyclerView.Adapter<EndlessListAdapter.BaseVH> {

    protected final EndlessListDataSource<Data> mDataSource;

    public final static int TYPE_LOADING = 1;
    public final static int TYPE_NULL = -1;

    public EndlessListAdapter(@NonNull EndlessListDataSource<Data> dataSource) {
        mDataSource = dataSource;
        mDataSource.updates().subscribe(updateEvent -> {
            notifyDataSetChanged();
        });
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    @CallSuper
    public int getItemViewType(int position) {
        if (mDataSource.getDataAt(position) == null) {
            return TYPE_LOADING;
        }
        return TYPE_NULL;
    }

    /**
     * To allow for automatic loading of new items in the underlying {@link EndlessListDataSource}, pretend there is an extra element at the end of the list.
     * If the data source is depleted, that's no longer needed.
     * @return
     */
    @Override
    public final int getItemCount() {
        final int count = mDataSource.getTotalCount();
        return mDataSource.isDepleted() ? count : count + 1;
    }

    protected final boolean hasToProduceAnotherViewType(int type) {
        return type != TYPE_LOADING;
    }

    @Override
    @CallSuper
    public BaseVH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            return new LoadingVH(parent);
        }
        return null;
    }

    @Override
    @CallSuper
    public void onBindViewHolder(BaseVH holder, int position) {
        holder.onBindVH(position);
    }

    public abstract static class BaseVH extends RecyclerView.ViewHolder {

        private BaseVH(View itemView) {
            super(itemView);
            init();
        }

        public BaseVH(ViewGroup parent, int layoutRes) {
            this(LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false));
        }

        public abstract void init();

        public abstract void onBindVH(int positionInList);

        public final View baseView() {
            return itemView;
        }
    }

    public class LoadingVH extends BaseVH {

        public LoadingVH(ViewGroup parent) {
            super(parent, R.layout.list_item_loading);
        }

        @Override
        public void init() {
        }

        @Override
        public void onBindVH(int positionInList) {
        }
    }
}
