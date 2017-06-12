package busu.test3.gbooks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.Books;
import com.google.api.services.books.BooksRequestInitializer;
import com.google.api.services.books.model.Volume;

import busu.test3.BuildConfig;

/**
 * Utilities for Google Books API
 */

public class BooksUtilities {

    /**
     * Scans the provided images and picks the most appropriate based on availability and size
     *
     * @param volumeInfo     the container for the image links
     * @param hasToBeSmalish if the search looks for smaller images rather than larger ones
     * @return
     */
    @Nullable
    public final static String getSuitableImageLink(@NonNull Volume.VolumeInfo volumeInfo, boolean hasToBeSmalish) {
        Volume.VolumeInfo.ImageLinks imageLinks = volumeInfo.getImageLinks();
        if (imageLinks == null) {
            return null;
        }
        String link = null;
        boolean hasMore = true;
        int searchIndex = -1;
        if (hasToBeSmalish) {
            while (link == null && hasMore) {
                searchIndex++;
                switch (searchIndex) {
                    case 0:
                        link = imageLinks.getSmall();
                        break;
                    case 1:
                        link = imageLinks.getThumbnail();
                        break;
                    case 2:
                        link = imageLinks.getSmallThumbnail();
                        break;
                    case 3:
                        link = imageLinks.getMedium();
                        hasMore = false;
                        break;
                }
            }
        } else {
            while (link == null && hasMore) {
                searchIndex++;
                switch (searchIndex) {
                    case 0:
                        link = imageLinks.getLarge();
                        break;
                    case 1:
                        link = imageLinks.getMedium();
                        break;
                    case 2:
                        link = imageLinks.getExtraLarge();
                        hasMore = false;
                        break;
                }
            }
        }
        return link;
    }

    public static Books setupBooksClient() {
        final String APPLICATION_NAME = "Busu-Test3/1.0";

        return new Books.Builder(AndroidHttp.newCompatibleTransport(), JacksonFactory.getDefaultInstance(), null)
                .setApplicationName(APPLICATION_NAME)
                .setGoogleClientRequestInitializer(new BooksRequestInitializer(BuildConfig.BOOKS_API_KEY))
                .build();
    }

    public static String generateBookDetails(@NonNull Volume.VolumeInfo volumeInfo) {
        StringBuffer sb = new StringBuffer(128);
        appendIfNotEmpty(sb, volumeInfo.getTitle());
        appendIfNotEmpty(sb, volumeInfo.getSubtitle());
        appendIfNotEmpty(sb, volumeInfo.getDescription());
        // others...
        return sb.toString();
    }

    private final static void appendIfNotEmpty(@NonNull StringBuffer to, @Nullable String what) {
        if (!TextUtils.isEmpty(what)) {
            to.append(what);
            to.append("\n");
        }
    }
}
