package busu.test3.datasource;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

/**
 * Data source for an endless list
 * Supports:
 * - caching of a continuous window of elements - defined by {@link #cacheSize}
 * - discarding of elements that fall outside this window (i.e. older elements that are not visible anymore)
 * - requests new pages {@link #pageSize} of data at the beginning or end of the window
 * - automatic management of cache misses based on calls to {@link EndlessListDataSource#getDataAt(int)} {@see doTheRequest}
 * - manual management of requests for new data based on calls to {@link EndlessListDataSource#requestNewPageOfData(DIRECTION)}
 * - provides a reactive stream of request results (= data source changes)
 */

public abstract class EndlessListDataSource<Data> {

    public final static int DEFAULT_PAGE_SIZE = 20;
    public final static int DEFAULT_CACHE_SIZE = 100;

    private int pageSize = DEFAULT_PAGE_SIZE;
    private int cacheSize = DEFAULT_CACHE_SIZE;

    private final ArrayList<Data> mCache = new ArrayList<>(DEFAULT_CACHE_SIZE);
    /**
     * offset of the current contents of mCache (mCache.get(pos) is in fact : mOffset + pos in the list
     */
    private int mOffset;
    /**
     * if there is no more data that this data source can obtain {@see doTheRequest}
     */
    private boolean mIsDepleted;

    private PublishSubject<WorkRequest> mRequestTrigger = PublishSubject.create();
    private BehaviorSubject<WorkResult> mUpdates = BehaviorSubject.create();

    public EndlessListDataSource() {
        //the trigger accepts just one request at a time and discards the other ones
        //use rxjava's operators (onBackpressureDrop, delay and rebatchRequests) to elegantly solve the sync
        mRequestTrigger
                .onBackpressureDrop()
                .delay(0, TimeUnit.MILLISECONDS, Schedulers.io())
                .map(work -> {
                    try {
                        final List<Data> data = doTheRequest(work);
                        return WorkResult.createSuccess(work, data);
                    } catch (Throwable th) {
                        return WorkResult.createError(work, th);
                    }
                })
                .delay(0, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .map(result -> {
                    if (result.isSuccessful()) {
                        try {
                            addToCache(result.data(), result.request().direction(), result.request().count());
                        } catch (Throwable th) {
                            return WorkResult.createError(result.request(), th);
                        }
                        trim(result.request().direction().opposite());
                    }
                    return result;
                })
                .rebatchRequests(1)
                .subscribe(mUpdates);

    }

    /**
     * The new request page size that will be applied at the next request for more data
     * ! input is not validated !
     *
     * @param value
     */
    public void changePageSizeTo(int value) {
        pageSize = value;
    }

    /**
     * The new cache size that will be applied at the next trimming operation
     * ! input is not validated !
     *
     * @param value
     */
    public void changeCacheSizeTo(int value) {
        cacheSize = value;
    }

    /**
     * Return the current data at the specified position AND prepares new requests if the data is not in cache
     *
     * @param position
     * @return
     */
    public Data getDataAt(int position) {
        if (position >= mOffset + mCache.size()) {
            requestNewPageOfData(DIRECTION.END);
        } else if (position < mOffset) {
            requestNewPageOfData(DIRECTION.FRONT);
        } else {
            return mCache.get(position - mOffset);
        }
        return null;
    }

    /**
     * Pretends it still holds the entire data, although parts of it might be already de-allocated
     *
     * @return the total number of elements
     */
    public int getTotalCount() {
        return mOffset + mCache.size();
    }

    private WorkRequest createWorkPackage(DIRECTION direction) {
        int count;
        int startingIndex;
        if (direction == DIRECTION.FRONT) {
            count = Math.min(pageSize, mOffset);
            startingIndex = mOffset - count;
        } else {
            startingIndex = mOffset + mCache.size();
            count = pageSize;
        }
        return WorkRequest.create(direction, count, startingIndex);
    }

    /**
     * The result of a data request is added to the cache
     * <p>
     * Assumptions:
     * - dataToAdd elements are copied to the cache without checking their validity
     * - dataToAdd.size() fits if direction is {@link DIRECTION#FRONT}
     * <p>
     * todo : fail gracefully if the assumptions are not met
     *
     * @param data      incoming data
     * @param direction to the fron or end of the cache
     */
    protected final void addToCache(@NonNull List<Data> data, DIRECTION direction, int requestedCount) throws Throwable {
        final int dataCount = data.size();

        if (dataCount != requestedCount) {
            //no more data
            if (direction == DIRECTION.END) {
                //no more data at the end, we're done
                mIsDepleted = true;
            } else {
                //not enough data at the front, this is an exception
                throw new Throwable("data not available anymore");
            }
        }

        if (direction == DIRECTION.FRONT) {
            if (mOffset - dataCount < 0) {
                throw new Throwable("attempting to add too much data");
            }
            mCache.addAll(0, data);
            mOffset -= dataCount;
        } else {
            mCache.addAll(mCache.size(), data);
        }
    }

    /**
     * Removes elements from the cache that are lo longer needed so that the cache size is kept to {@link #cacheSize}
     *
     * @param direction
     */
    private void trim(DIRECTION direction) {
        final int trimAmount = mCache.size() - cacheSize;
        if (trimAmount > 0) {
            if (direction == DIRECTION.FRONT) {
                mCache.subList(0, trimAmount).clear();
                mOffset += trimAmount;
            } else {
                mCache.subList(mCache.size() - trimAmount, mCache.size()).clear();
                //since trimming happened at the end, the datasource is no more reaching the end
                mIsDepleted = false;
            }
        }
    }

    /**
     * The stream of updates
     *
     * @return
     */
    public Observable<WorkResult> updates() {
        return mUpdates.asObservable();
    }

    /**
     * Triggers a request for a new page of data at the front or at the end of the list
     *
     * @param direction
     */
    public void requestNewPageOfData(DIRECTION direction) {
        mRequestTrigger.onNext(createWorkPackage(direction));
    }

    /**
     * If there is no more data that can be requested via {@link EndlessListDataSource#doTheRequest(WorkRequest)}
     *
     * @return
     */
    public boolean isDepleted() {
        return mIsDepleted;
    }

    /**
     * Implementations must provide the functionality to fetch/return a list of fresh data for the specified request
     * <p>
     * TODO: migrate to a Observable (allows more control - e.g. the scheduler on which it runs)
     *
     * @param request the request container (from which position, count)
     * @return a non-null list; if the list has the size = 0, then this means there was no data for the request -> the data source is depleted
     * @throws Throwable the error
     */
    protected abstract
    @NonNull
    List<Data> doTheRequest(@NonNull WorkRequest request) throws Throwable;

    public void clearCache() {
        mIsDepleted = false;
        mOffset = 0;
        mCache.clear();
    }

    /**
     * The current status of the data source
     *
     * @return
     */
    @Override
    public String toString() {
        return "size: " + mCache.size() + ", gap:" + mOffset;
    }


    /**
     * Where the data source loads new items
     */
    public enum DIRECTION {
        FRONT(0),
        END(1);

        private final int mValue;

        DIRECTION(final int newValue) {
            mValue = newValue;
        }

        public int value() {
            return mValue;
        }

        public DIRECTION opposite() {
            return this == FRONT ? END : FRONT;
        }
    }

    /**
     * A unit of work for a fetch new data request
     */
    @AutoValue
    public static abstract class WorkRequest {
        public abstract DIRECTION direction();

        /**
         * @return How many elements to be fetched
         */
        public abstract int count();

        /**
         * @return From what position in the endless list
         */
        public abstract int from();

        public static WorkRequest create(DIRECTION direction, int count, int from) {
            return new AutoValue_EndlessListDataSource_WorkRequest(direction, count, from);
        }
    }

    /**
     * The result of a {@link WorkRequest}
     *
     * @param <Data>
     */
    @AutoValue
    public static abstract class WorkResult<Data> {
        public abstract
        @NonNull
        WorkRequest request();

        public abstract
        @Nullable
        List<Data> data();

        public abstract
        @Nullable
        Throwable error();

        private static <Data> WorkResult create(@NonNull WorkRequest request, @Nullable List<Data> data, @Nullable Throwable error) {
            return new AutoValue_EndlessListDataSource_WorkResult(request, data, error);
        }

        public static WorkResult createError(@NonNull WorkRequest request, @NonNull Throwable error) {
            return create(request, null, error);
        }

        public static <Data> WorkResult createSuccess(@NonNull WorkRequest request, @NonNull List<Data> data) {
            return create(request, data, null);
        }

        public boolean isSuccessful() {
            return error() == null;
        }
    }
}
