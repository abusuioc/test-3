package busu.test3.gbooks;

import android.support.annotation.NonNull;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;

import java.util.List;

import busu.test3.BuildConfig;
import busu.test3.datasource.EndlessListDataSource;

/**
 * TODO: add class header
 */

public class BooksDataSource extends EndlessListDataSource<Volume> {

    private final Books mBooks;
    private final String mQuery;

    public BooksDataSource(Config dataSourceConfig, String query) {
        super(dataSourceConfig);
        mBooks = setupBooksClient();
        mQuery = query;
    }

    private Books setupBooksClient() {
        final String APPLICATION_NAME = "Busu-Test3/1.0";
        final String QUERY = "007";

        return new Books.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), null)
                .setApplicationName(APPLICATION_NAME)
                .setGoogleClientRequestInitializer(new BooksRequestInitializer(BuildConfig.BOOKS_API_KEY))
                .build();
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
