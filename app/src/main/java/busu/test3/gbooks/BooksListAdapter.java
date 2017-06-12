package busu.test3.gbooks;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.services.books.model.Volume;
import com.squareup.picasso.Picasso;

import busu.test3.R;
import busu.test3.datasource.EndlessListDataSource;
import busu.test3.endless.EndlessListAdapter;
import rx.Observable;

/**
 * TODO: add a class header comment!
 */

public class BooksListAdapter extends EndlessListAdapter<Volume> {

    private final static int TYPE_BOOK = TYPE_LOADING + 1;

    public BooksListAdapter(@NonNull BooksDataSource dataSource, @NonNull Observable.Transformer lifecycleEvents) {
        super(dataSource, lifecycleEvents);
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
        private ImageView mCover;

        public BookVH(ViewGroup parent) {
            super(parent, R.layout.list_item_book);
        }

        @Override
        public void init() {
            mCover = (ImageView) baseView().findViewById(R.id.book_cover);
            mTitle = (TextView) baseView().findViewById(R.id.book_title);
        }

        @Override
        public void onBindVH(int positionInList) {
            Volume.VolumeInfo volumeInfo = mDataSource.getDataAt(positionInList).getVolumeInfo();
            bindTitle(volumeInfo);
            bindCover(volumeInfo);
        }

        private void bindTitle(Volume.VolumeInfo volumeInfo) {
            mTitle.setText(volumeInfo.getTitle());
        }

        private void bindCover(Volume.VolumeInfo volumeInfo) {
            final String coverUrlPath = BooksUtilities.getSuitableImageLink(volumeInfo, true);
            if (coverUrlPath == null) {
                mCover.setImageResource(android.R.drawable.ic_menu_report_image);
            } else
                Picasso.with(null)
                        .load(coverUrlPath)
                        .into(mCover);
        }
    }
}
