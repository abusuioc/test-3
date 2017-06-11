package busu.test3.datasource;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.Exceptions;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

/**
 * TODO: add class header
 */

public abstract class EndlessDataSource<Data> {
    private final ArrayList<Data> mData = new ArrayList<>(new DefaultConfig().pageSize());
    private Config mConfig;

    private int mIndexOffset;
    private boolean mHasReachTheEnd;

    private PublishSubject<Work> mRequestTrigger = PublishSubject.create();
    private BehaviorSubject<UpdateEvent> mUpdates = BehaviorSubject.create();

    public EndlessDataSource(Config config) {
        mConfig = config;
        //https://stackoverflow.com/questions/41958423/rxjava-subject-with-backpressure-only-let-the-last-value-emit-once-downstream
        mRequestTrigger
                .doOnNext(__ -> Log.i("DS", "start_req_before"))
                .onBackpressureDrop()
                .doOnNext(__ -> Log.i("DS", "start_req_after_bp"))
                .delay(0, TimeUnit.MILLISECONDS, Schedulers.io())
                .map(work -> {
                    try {
                        List<Data> data = doTheRequest(work.from(), work.count());
                        return WorkResult.create(work, data);
                    } catch (Throwable th) {
                        Exceptions.propagate(th);
                        return null;
                    }
                })
                .delay(0, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .doOnNext(result -> {
                    try {
                        addData(result.data(), result.request().direction(), result.request().count());
                    } catch (Throwable th) {
                        Exceptions.propagate(th);
                    }
                    trim(result.request().direction().opposite());
                })
                .map(result -> UpdateEvent.create(result.request().direction(), null))
                .onErrorReturn(throwable -> UpdateEvent.create(null, throwable))
                .rebatchRequests(1)
                .subscribe(mUpdates);

    }


    public Data getDataAt(int position) {
        Log.i("DS", "" + position);
        if (position >= mIndexOffset + mData.size()) {
            requestMoreData(DIRECTION.END);
            requestMoreData(DIRECTION.END);
            requestMoreData(DIRECTION.END);
            requestMoreData(DIRECTION.END);
            requestMoreData(DIRECTION.END);
            requestMoreData(DIRECTION.END);
        } else if (position < mIndexOffset) {
            requestMoreData(DIRECTION.FRONT);
        } else {
            Log.i("DS", "data");
            return mData.get(position - mIndexOffset);
        }
        Log.i("DS", "null");
        return null;
    }

    public int getItemCount() {
        return mIndexOffset + mData.size();
    }

    private Work createWorkPackage(DIRECTION direction) {
        int count;
        int startingIndex;
        if (direction == DIRECTION.FRONT) {
            count = Math.min(mConfig.pageSize(), mIndexOffset);
            startingIndex = mIndexOffset - count;
        } else {
            startingIndex = mIndexOffset + mData.size();
            count = mConfig.pageSize();
        }
        return Work.create(direction, count, startingIndex);
    }

    /**
     * Assumptions:
     * - dataToAdd elements are copied to the cache without checking their validity
     * - dataToAdd.size() fits if direction is {@link DIRECTION#FRONT}
     *
     * @param dataToAdd
     * @param direction
     */
    protected final void addData(List<Data> dataToAdd, DIRECTION direction, int requestedCount) throws Throwable {
        if (dataToAdd == null || dataToAdd.size() <= 0 || dataToAdd.size() != requestedCount) {
            //no more data
            if (direction == DIRECTION.END) {
                //mark the datasource as terminated
                mHasReachTheEnd = true;
            } else {
                //not enough data at the front, this is an exception
                throw new Throwable("data not there anymore");
            }
        }
        final int dataCount = dataToAdd.size();

        if (direction == DIRECTION.FRONT) {
            if (mIndexOffset - dataCount < 0) {
                throw new Throwable("attempting to add too much data");
            }
            mData.addAll(0, dataToAdd);
            mIndexOffset -= dataCount;
        } else {
            mData.addAll(mData.size(), dataToAdd);
        }
    }

    private void trim(DIRECTION direction) {
        final int trimAmount = mData.size() - mConfig.maxSize();
        if (trimAmount > 0) {
            if (direction == DIRECTION.FRONT) {
                mData.subList(0, trimAmount).clear();
                mIndexOffset += trimAmount;
            } else {
                mData.subList(mData.size() - trimAmount, mData.size()).clear();
                mHasReachTheEnd = false;
            }
        }
    }

//    public final boolean isAlreadyRequestingMoreData(DIRECTION direction) {
//        return isRequesting[direction.value()];
//    }

//    private final void setRequestingStatus(DIRECTION direction, boolean isRequestingStatus) {
//        isRequesting[direction.value()] = isRequestingStatus;
//    }

    public Observable<UpdateEvent> updates() {
        return mUpdates.asObservable();
    }

    /**
     * @param direction
     */
    public void requestMoreData(DIRECTION direction) {
        mRequestTrigger.onNext(createWorkPackage(direction));
        Log.i("DS", "req > " + direction.toString());
    }

    public boolean hasReachTheEnd() {
        return mHasReachTheEnd;
    }

    protected abstract List<Data> doTheRequest(int startingIndex, int count) throws Throwable;

    @Override
    public String toString() {
        return "total:" + mData.size() + ", offset:" + mIndexOffset;
    }

    public static class DefaultConfig implements Config {

        @Override
        public int pageSize() {
            return 6;
        }

        @Override
        public int maxSize() {
            return 20;
        }
    }

    public interface Config {
        int pageSize();

        int maxSize();
    }

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

    @AutoValue
    public static abstract class UpdateEvent {
        public abstract
        @Nullable
        DIRECTION direction();

        public abstract
        @Nullable
        Throwable error();

        public static UpdateEvent create(@Nullable DIRECTION direction, @Nullable Throwable error) {
            return new AutoValue_EndlessDataSource_UpdateEvent(direction, error);

        }
    }

    @AutoValue
    public static abstract class Work {
        public abstract DIRECTION direction();

        public abstract int count();

        public abstract int from();

        public static Work create(DIRECTION direction, int count, int from) {
            return new AutoValue_EndlessDataSource_Work(direction, count, from);
        }
    }

    @AutoValue
    public static abstract class WorkResult<Data> {
        public abstract Work request();

        public abstract List<Data> data();

        public static <Data> WorkResult create(Work request, List<Data> data) {
            return new AutoValue_EndlessDataSource_WorkResult(request, data);
        }
    }
}
