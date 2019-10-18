package org.mozilla.rocket.content.travel.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_travel_bucket_list.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.VerticalSpaceItemDecoration
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityAdapterDelegate
import org.mozilla.rocket.content.travel.ui.adapter.BucketListCityUiModel
import javax.inject.Inject

class TravelBucketListFragment : Fragment() {

    @Inject
    lateinit var travelBucketListViewModelCreator: Lazy<TravelBucketListViewModel>

    private lateinit var travelBucketListViewModel: TravelBucketListViewModel
    private lateinit var bucketListAdapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        travelBucketListViewModel = getActivityViewModel(travelBucketListViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_travel_bucket_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBucketList()
        bindBucketListData()
        bindLoadingState()
        observeBucketListActions()
    }

    private fun initBucketList() {
        bucketListAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(BucketListCityUiModel::class, R.layout.item_bucket_list, BucketListCityAdapterDelegate(travelBucketListViewModel))
            }
        )

        bucket_list_recycler_view.apply {
            val spaceWidth = resources.getDimensionPixelSize(R.dimen.card_space_width)
            addItemDecoration(VerticalSpaceItemDecoration(spaceWidth))

            adapter = bucketListAdapter
        }

        bucket_list_empty_explore.setOnClickListener {
            travelBucketListViewModel.onExploreCityClicked()
        }
    }

    private fun bindBucketListData() {
        travelBucketListViewModel.items.observe(this, Observer {
            bucketListAdapter.setData(it)

            if(it.isEmpty()) {
                showEmptyView()
            } else {
                showContentView()
            }
        })
    }

    private fun bindLoadingState() {
        travelBucketListViewModel.isDataLoading.observe(this@TravelBucketListFragment, Observer { state ->
            when (state) {
                is TravelBucketListViewModel.State.Idle -> showLoadedView()
                is TravelBucketListViewModel.State.Loading -> showLoadingView()
                is TravelBucketListViewModel.State.Error -> showEmptyView()
            }
        })
    }

    private fun observeBucketListActions() {

        travelBucketListViewModel.openCity.observe(this, Observer { name ->
            context?.let {
                startActivity(TravelCityActivity.getStartIntent(it, name))
            }
        })

        travelBucketListViewModel.goSearch.observe(this, Observer {
            // TODO go search
        })
    }

    private fun showLoadingView() {
        spinner.isVisible = true
        bucket_list_content_layout.isVisible = false
    }

    private fun showLoadedView() {
        spinner.isVisible = false
        bucket_list_content_layout.isVisible = true
    }

    private fun showContentView() {
        bucket_list_empty_view.isVisible = false
        bucket_list_recycler_view.isVisible = true
    }

    private fun showEmptyView() {
        bucket_list_empty_view.isVisible = true
        bucket_list_recycler_view.isVisible = false
    }
}