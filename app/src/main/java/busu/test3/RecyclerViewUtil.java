package busu.test3;

import android.support.v7.widget.RecyclerView;

/**
 * Helper class to get the first and last visible positions of items in a {@link RecyclerView}
 */

public class RecyclerViewUtil {

    public static int getFirstVisiblePosition(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        return layoutManager.getChildCount() == 0
                ? 0 : layoutManager.getPosition(layoutManager.getChildAt(0));
    }

    public static int getLastVisiblePosition(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        return layoutManager.getChildCount() == 0 ? 0 : layoutManager.getPosition(
                layoutManager.getChildAt(layoutManager.getChildCount() - 1));
    }
}