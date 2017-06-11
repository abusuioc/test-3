package busu.test3;

import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.TextView;

import busu.test3.datasource.EndlessListDataSource;
import busu.test3.endless.EndlessListAdapter;
import rx.Observable;

/**
 * TODO: add a class header comment!
 */

public class BooksListAdapter extends EndlessListAdapter<String> {

    private final static int TYPE_BOOK = TYPE_LOADING + 1;

    public BooksListAdapter(@NonNull EndlessListDataSource<String> cache, @NonNull Observable.Transformer lifecycleEvents) {
        super(cache, lifecycleEvents);
    }

    @Override
    public int getItemViewType(int position) {
        final int type = super.getItemViewType(position);
        if (isThisTypeAlreadyKnown(type)) {
            return type;
        } else {
            return TYPE_BOOK;
        }
    }

    @Override
    public BaseVH onCreateViewHolder(ViewGroup parent, int viewType) {
        BaseVH holder = super.onCreateViewHolder(parent, viewType);
        if (holder == null) {
            holder = new BookVH(parent);
        }
        return holder;
    }

    public class BookVH extends BaseVH {

        private TextView mTitle;

        public BookVH(ViewGroup parent) {
            super(parent, android.R.layout.simple_list_item_1);
        }

        @Override
        public void init() {
            mTitle = (TextView) baseView();
        }

        @Override
        public void onBindVH(int positionInList) {
            mTitle.setText(mDataSource.getDataAt(positionInList));
        }
    }
}
