package org.mozilla.rocket.msrp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.fragment_missions.*
import kotlinx.android.synthetic.main.msrp_challenge_mission.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.appComponent

class ChallengeListFragment : Fragment() {

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
                    add(ChallengeUiModel::class, R.layout.msrp_challenge_mission, ChallengeAdapterDelegate())
                }
        )
        recycler_view.apply {
            adapter = this@ChallengeListFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun prepareData() {

        val fakeChallenge = ChallengeUiModel(
                title = "7-day challenge for free VPN",
                expiration_text = "Expires 02/08/2019",
                progress = 74,
                image_url = "http://www.gameloft.com/central/upload/Asphalt-9-Legends-Slider-logo-2.jpg",
                show_red_dot = true
        )

        adapter.setData(listOf(fakeChallenge))
    }

    private class ChallengeAdapterDelegate : AdapterDelegate {
        override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
                ChallengeViewHolder(view)
    }

    private class ChallengeViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
        override fun bind(uiModel: DelegateAdapter.UiModel) {
            uiModel as ChallengeUiModel

            challenge_title.text = uiModel.title
            challenge_expiration_text.text = uiModel.expiration_text
            challenge_progress.progress = uiModel.progress
            challenge_percentage_text.text = uiModel.progress.toString() + "%"

            uiModel.image_url.let {
                Glide.with(containerView.context).load(it).apply(requestOptions).into(challenge_image)
            }
        }

        companion object {
            var requestOptions = RequestOptions().apply { transforms(CenterCrop(), RoundedCorners(16)) }
        }
    }

    data class ChallengeUiModel(
        val title: String,
        val expiration_text: String,
        val show_red_dot: Boolean,
        val image_url: String,
        val progress: Int
    ) : DelegateAdapter.UiModel()
}