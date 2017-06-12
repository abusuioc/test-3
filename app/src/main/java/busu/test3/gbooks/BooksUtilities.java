package busu.test3.gbooks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.api.services.books.model.Volume;

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
}
