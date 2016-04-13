package com.coinblesk.client.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Andreas Albrecht on 11.04.16.
 */
public class RecyclerView extends android.support.v7.widget.RecyclerView {

    // view to display if no data available
    private View emptyView;
    // watch adapter for changes and switch on/off the empty view accordingly.
    private final AdapterDataObserver dataObserver = new EmptyViewObserver();

    public RecyclerView(Context context) {
        super(context);
    }

    // Note: 2 and 3 arg constructors are used in the xml IDE view display
    public RecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    /**
     * Set a view to display in case no items are currently displayed
     * @param emptyView
     */
    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (getAdapter() != null) {
            getAdapter().unregisterAdapterDataObserver(dataObserver);
        }
        if (adapter != null) {
            adapter.registerAdapterDataObserver(dataObserver);
        }
        super.setAdapter(adapter);
        updateEmptyView();
    }

    private class EmptyViewObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
            super.onChanged();
            updateEmptyView();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            updateEmptyView();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            updateEmptyView();
        }
    }

    private void updateEmptyView() {
        if (emptyView != null && getAdapter() != null) {
            boolean isEmpty = getAdapter().getItemCount() == 0;
            emptyView.setVisibility(isEmpty ? VISIBLE : GONE);
            setVisibility(isEmpty ? GONE : VISIBLE);
        }
    }
}
