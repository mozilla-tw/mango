package org.mozilla.rocket.msrp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_missions.*
import kotlinx.android.synthetic.main.msrp_redeem_mission.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent

class RedeemListFragment : Fragment() {

    private lateinit var adapter: DelegateAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_missions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        prepareData()
    }

    private fun initRecyclerView() {
        adapter = DelegateAdapter(
                AdapterDelegatesManager().apply {
                    add(RedeemUiModel::class, R.layout.msrp_redeem_mission, RedeemAdapterDelegate())
                }
        )
        recycler_view.apply {
            adapter = this@RedeemListFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun prepareData() {

        val fakeRedeem = RedeemUiModel(
                title = "7-Day challenge for Rs 15,000 shopping coupon",
                description_text = "Waiting for redemption",
                show_redeem_btn = true
        )

        adapter.setData(listOf(fakeRedeem))
    }

    private class RedeemAdapterDelegate : AdapterDelegate {
        override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
                RedeemViewHolder(view)
    }

    private class RedeemViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
        override fun bind(uiModel: DelegateAdapter.UiModel) {
            uiModel as RedeemUiModel

            redeem_title.text = uiModel.title
            redeem_description.text = uiModel.description_text
            redeem_btn.visibility = if (uiModel.show_redeem_btn) View.VISIBLE else View.INVISIBLE
        }
    }

    data class RedeemUiModel(
        val title: String,
        val description_text: String,
        val show_redeem_btn: Boolean
    ) : DelegateAdapter.UiModel()
}