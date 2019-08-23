package org.mozilla.rocket.shopping.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_shopping_search_preferences.*
import org.mozilla.focus.R
import androidx.recyclerview.widget.ItemTouchHelper
import dagger.Lazy
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.shopping.search.ui.adapter.ItemMoveCallback
import org.mozilla.rocket.shopping.search.ui.adapter.PreferencesAdapterDelegate
import org.mozilla.rocket.shopping.search.ui.adapter.SiteViewHolder
import javax.inject.Inject

class ShoppingSearchPreferencesActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelCreator: Lazy<ShoppingSearchPreferencesViewModel>

    private lateinit var viewModel: ShoppingSearchPreferencesViewModel

    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_search_preferences)
        viewModel = getViewModel(viewModelCreator)
        initToolBar()
        initPreferenceList()
    }

    private fun initToolBar() {
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun initPreferenceList() {
        val adapterDelegate = PreferencesAdapterDelegate(viewModel)
        adapter = DelegateAdapter(AdapterDelegatesManager().apply {
            add(SiteViewHolder.PreferencesUiModel::class, R.layout.item_shopping_search_preference, adapterDelegate)
        })

        val callback = ItemMoveCallback(adapterDelegate)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.setCallBack(object : ShoppingSearchPreferencesViewModel.ViewModelCallBack {

            override fun notifyItemMoved(fromPosition: Int, toPosition: Int) {
                adapter.notifyItemMoved(fromPosition, toPosition)
            }
        })
        viewModel.sitesLiveData.observe(this, Observer {
            adapter.setData(it)
        })
    }

    companion object {
        fun getStartIntent(context: Context) = Intent(context, ShoppingSearchPreferencesActivity::class.java)
    }
}