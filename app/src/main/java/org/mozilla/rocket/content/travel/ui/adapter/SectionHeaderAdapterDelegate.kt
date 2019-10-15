package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_section_header.*
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel.SectionType
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel.SectionType.Explore
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel.SectionType.TopHotels

class SectionHeaderAdapterDelegate(private val travelCityViewModel: TravelCityViewModel) : AdapterDelegate {

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        SectionHeaderViewHolder(view, travelCityViewModel)
}

class SectionHeaderViewHolder(
    override val containerView: View,
    private val travelCityViewModel: TravelCityViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val header = uiModel as SectionHeaderUiModel
        when (header.type) {
            is Explore -> header_title.text = containerView.context.getString(R.string.travel_detail_section_explore_title, header.type.name)
            is TopHotels -> header_title.text = containerView.context.getString(R.string.travel_detail_section_hotel_title)
        }
    }
}

data class SectionHeaderUiModel(
    val type: SectionType
) : DelegateAdapter.UiModel()
