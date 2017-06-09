package busu.test3;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * TODO: add a class header comment!
 */

public class BooksListAdapter extends RecyclerView.Adapter<BooksListAdapter.BaseVH> {

    @Override
    public BaseVH onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(BaseVH holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
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

    public class BookVH extends BaseVH {

        public BookVH(ViewGroup parent) {
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
