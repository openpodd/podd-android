/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cm.podd.report.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Subclass of {@link android.support.v4.app.ListFragment} which provides automatic support for
 * providing the 'swipe-to-refresh' UX gesture by wrapping the the content view in a
 * {@link android.support.v4.widget.SwipeRefreshLayout}.
 */
abstract public class SwipeRefreshFragment extends Fragment {

    protected SwipeRefreshLayout mSwipeRefreshLayout;

    protected View mFragmentView;
    protected SwipeRefreshLayout.OnRefreshListener mRefreshListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // Now create a SwipeRefreshLayout to wrap the fragment's content view
        mSwipeRefreshLayout = new FragmentSwipeRefreshLayout(container.getContext());

        // Add the fragment's content view to the SwipeRefreshLayout, making sure that it fills
        // the SwipeRefreshLayout
        mSwipeRefreshLayout.addView(mFragmentView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // Make sure that the SwipeRefreshLayout will fill the fragment
        mSwipeRefreshLayout.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        // Set listener if provided.
        if (mRefreshListener != null) {
            setOnRefreshListener(mRefreshListener);
        }

        // Now return the SwipeRefreshLayout as this fragment's content view
        return mSwipeRefreshLayout;
    }

    public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener) {
        mSwipeRefreshLayout.setOnRefreshListener(listener);
    }

    public boolean isRefreshing() {
        return mSwipeRefreshLayout.isRefreshing();
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return mSwipeRefreshLayout;
    }

    protected class FragmentSwipeRefreshLayout extends SwipeRefreshLayout {

        public FragmentSwipeRefreshLayout(Context context) {
            super(context);
        }

        @Override
        public boolean canChildScrollUp() {
            final View view = getView();
            if (view.getVisibility() == View.VISIBLE) {
                return canViewScrollUp(view);
            } else {
                return false;
            }
        }

    }

    protected boolean canViewScrollUp(View view) {
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            // For ICS and above we can call canScrollVertically() to determine this
            return ViewCompat.canScrollVertically(view, -1);
        } else {
            // Pre-ICS we need to manually check the first visible item and the child view's top
            // value
            return (view.getVerticalScrollbarPosition() > 0
                    || view.getTop() < view.getPaddingTop());
        }
    }

}
