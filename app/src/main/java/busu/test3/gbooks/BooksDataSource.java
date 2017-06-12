package busu.test3.gbooks;

import android.support.annotation.NonNull;

import com.google.api.services.books.Books;
import com.google.api.services.books.model.Volume;

import java.util.List;

import busu.test3.datasource.EndlessListDataSource;

/**
 * All the Google Books that contain "a" (lots of them!)
 */

public class BooksDataSource extends EndlessListDataSource<Volume> {

    private final Books mBooks;

    public BooksDataSource(Config dataSourceConfig) {
        super(dataSourceConfig);
        mBooks = BooksUtilities.setupBooksClient();
    }

    @NonNull
    @Override
    protected List<Volume> doTheRequest(@NonNull WorkRequest request) throws Throwable {
        return mBooks
                .volumes()
                .list("a")
                .setStartIndex((long) request.from())
                .setMaxResults((long) request.count())
                .execute()
                .getItems();
    }
}
