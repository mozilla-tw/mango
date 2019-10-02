package org.mozilla.rocket.content.travel.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_search_city.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.content.travel.ui.adapter.CityAdapterDelegate
import javax.inject.Inject

class TravelCitySearchActivity : AppCompatActivity() {

    @Inject
    lateinit var searchViewModelCreator: Lazy<TravelCitySearchViewModel>
    private lateinit var searchViewModel: TravelCitySearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        searchViewModel = getViewModel(searchViewModelCreator)
        setContentView(R.layout.activity_search_city)
        window?.statusBarColor = Color.WHITE
        search_keyword_edit.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val visibility: Int
                if (s?.isNotEmpty() == true) {
                    visibility = View.VISIBLE
                    searchViewModel.search(s.toString().toLowerCase())
                } else {
                    visibility = View.GONE
                }

                recyclerView.visibility = visibility
                clear.visibility = visibility
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
        clear.setOnClickListener {
            search_keyword_edit.setText("")
        }
        initCityList()
    }

    private fun initCityList() {
        val adapterDelegate = CityAdapterDelegate(searchViewModel)
        val adapterDelegatesManager = AdapterDelegatesManager().apply {
            add(CitySearchResultUiModel::class, R.layout.item_city, adapterDelegate)
        }
        val adapter = DelegateAdapter(adapterDelegatesManager)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TravelCitySearchActivity)
            this.adapter = adapter
        }
        searchViewModel.items.observe(this, Observer {
            if (it != null) {
                adapter.setData(it)
            }
        })
        searchViewModel.openCity.observe(this, Observer {
        })
    }

    companion object {
        fun getStartIntent(context: Context) =
            Intent(context, TravelCitySearchActivity::class.java).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}