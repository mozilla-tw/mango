/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.history;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.ItemClosingPanelFragmentStatusListener;
import org.mozilla.focus.fragment.PanelFragment;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.TopSitesUtils;
import org.mozilla.rocket.chrome.ChromeViewModel;
import org.mozilla.rocket.content.BaseViewModelFactory;
import org.mozilla.rocket.content.ExtentionKt;

import javax.inject.Inject;

import dagger.Lazy;


public class BrowsingHistoryFragment extends PanelFragment implements View.OnClickListener, ItemClosingPanelFragmentStatusListener {

    @Inject
    Lazy<ChromeViewModel> chromeViewModelCreator;

    private RecyclerView mRecyclerView;
    private ViewGroup mContainerEmptyView, mContainerRecyclerView;
    private HistoryItemAdapter mAdapter;

    public static BrowsingHistoryFragment newInstance() {
        return new BrowsingHistoryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        ExtentionKt.appComponent(this).inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_browsing_history, container, false);
        v.findViewById(R.id.browsing_history_btn_clear).setOnClickListener(this);

        mContainerRecyclerView = (ViewGroup) v.findViewById(R.id.browsing_history_recycler_view_container);
        mContainerEmptyView = (ViewGroup) v.findViewById(R.id.empty_view_container);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.browsing_history_recycler_view);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new HistoryItemAdapter(mRecyclerView, getActivity(), this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.browsing_history_btn_clear:
                // if Fragment is detached but AlertDialog still on the screen, we might get null context in callback
                final Context ctx = getContext();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogStyle);
                builder.setTitle(R.string.browsing_history_dialog_confirm_clear_message);
                builder.setPositiveButton(R.string.browsing_history_dialog_btn_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (ctx == null) {
                            return;
                        }
                        mAdapter.clear();
                        TopSitesUtils.getDefaultSitesJsonArrayFromAssets(ctx);
                        ChromeViewModel chromeViewModel = ViewModelProviders.of(requireActivity(), new BaseViewModelFactory<>(chromeViewModelCreator::get)).get(ChromeViewModel.class);
                        chromeViewModel.getClearBrowsingHistory().call();
                        TelemetryWrapper.clearHistory();
                    }
                });
                builder.setNegativeButton(R.string.action_cancel, null);
                builder.create().show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onStatus(@ViewStatus int status) {
        if (VIEW_TYPE_EMPTY == status) {
            mContainerRecyclerView.setVisibility(View.GONE);
            mContainerEmptyView.setVisibility(View.VISIBLE);
        } else if (VIEW_TYPE_NON_EMPTY == status) {
            mContainerRecyclerView.setVisibility(View.VISIBLE);
            mContainerEmptyView.setVisibility(View.GONE);
        } else {
            mContainerRecyclerView.setVisibility(View.GONE);
            mContainerEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClicked() {
        closePanel();
    }

    @Override
    public void tryLoadMore() {
        mAdapter.tryLoadMore();
    }
}
