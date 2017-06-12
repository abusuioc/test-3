package busu.test3.gbooks;

import android.support.annotation.NonNull;

import com.google.api.services.books.Books;
import com.google.api.services.books.model.Volume;

import java.util.List;

import busu.test3.datasource.EndlessListDataSource;

/**
 * TODO: add class header
 */

public class BooksDataSource extends EndlessListDataSource<Volume> {

    private final Books mBooks;
    private final String mQuery;

    public BooksDataSource(Config dataSourceConfig, String query) {
        super(dataSourceConfig);
        mBooks = BooksUtilities.setupBooksClient();
        mQuery = query;
    }

    @NonNull
    @Override
    protected List<Volume> doTheRequest(@NonNull WorkRequest request) throws Throwable {
        return mBooks
                .volumes()
                .list(mQuery)
                .setStartIndex((long) request.from())
                .setMaxResults((long) request.count())
                .execute()
                .getItems();
    }
}
