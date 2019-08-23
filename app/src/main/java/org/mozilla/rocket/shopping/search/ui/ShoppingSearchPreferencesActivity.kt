package org.mozilla.rocket.shopping.search.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_shopping_search_preferences.*
import org.mozilla.focus.R
import androidx.recyclerview.widget.ItemTouchHelper
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository

class ShoppingSearchPreferencesActivity : AppCompatActivity() {

    private val viewModelFactory: ShoppingSearchPreferencesViewModel.Factory by lazy {
        ShoppingSearchPreferencesViewModel.Factory(ShoppingSearchSiteRepository())
    }

    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ShoppingSearchPreferencesViewModel::class.java)
    }

    private val adapter by lazy {
        ShoppingSearchPreferencesSiteAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_search_preferences)
        initView()
        setObserver()
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadListData()
    }

    private fun initView() {
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val callback = ItemMoveCallback(adapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setObserver() {
        viewModel.sitesLiveData.observe(this, Observer {
            adapter.setData(it)
        })
    }

    companion object {
        fun getStartIntent(context: Context) = Intent(context, ShoppingSearchPreferencesActivity::class.java)
    }
}