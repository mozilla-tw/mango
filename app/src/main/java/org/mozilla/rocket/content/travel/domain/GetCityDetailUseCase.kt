package org.mozilla.rocket.content.travel.domain

import android.content.Context
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.travel.data.Hotel
import org.mozilla.rocket.content.travel.data.Ig
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.data.Video
import org.mozilla.rocket.content.travel.data.Wiki
import org.mozilla.rocket.content.travel.ui.ExploreIgUiModel
import org.mozilla.rocket.content.travel.ui.ExploreWikiUiModel
import org.mozilla.rocket.content.travel.ui.HotelUiModel
import org.mozilla.rocket.content.travel.ui.SectionUiModel
import org.mozilla.rocket.content.travel.ui.VideoUiModel

class GetCityDetailUseCase(
    private val appContext: Context,
    private val travelRepository: TravelRepository
) {

    private val data = ArrayList<DelegateAdapter.UiModel>()

    suspend operator fun invoke(name: String): Result<List<DelegateAdapter.UiModel>> {
        data.clear()

        // TODO: add price items

        // add explore
        data.add(
                SectionUiModel(
                    appContext.getString(R.string.travel_detail_section_explore_title, name),
                    ""
                )
        )

        val igResult = travelRepository.getCityIg(name)
        if (igResult is Success) {
            data.add(toExploreIgUiModel(igResult.data))
        }

        val wikiResult = travelRepository.getCityWiki(name)
        if (wikiResult is Success) {
            data.add(toExploreWikiUiModel(wikiResult.data))
        }

        val videoResult = travelRepository.getCityVideos(name)
        if (videoResult is Success) {
            data.addAll(
                    videoResult.data.map {
                        // TODO: handle real read stats
                        toVideoUiModel(it, false)
                    }
            )
        }

        // add hotel
        data.add(
                SectionUiModel(
                    appContext.getString(R.string.travel_detail_section_hotel_title),
                    "https://www.booking.com/searchresults.html?label=gen173nr-1FCAEoggI46AdIMFgEaOcBiAEBmAEwuAEHyAEM2AEB6AEB-AECiAIBqAIDuAK07-DsBcACAQ;sid=f086a0a5fa31aa51435d73202c1f1ebd;tmpl=searchresults;class_interval=1;dest_id=835;dest_type=region;dtdisc=0;from_sf=1;group_adults=2;group_children=0;inac=0;index_postcard=0;label_click=undef;lang=en-us;no_rooms=1;offset=0;postcard=0;room1=A%2CA;sb_price_type=total;shw_aparth=1;slp_r_match=0;soz=1;src=index;src_elem=sb;srpvid=30882d64a1250024;ss=Bali;ss_all=0;ssb=empty;sshis=0;top_ufis=1&"
                )
        )

        val hotelResult = travelRepository.getCityHotels(name)
        if (hotelResult is Success) {
            data.addAll(
                    hotelResult.data.map {
                        toHotelUiModel(it)
                    }
            )
        }

        // TODO: handle error

        return Success(data)
    }

    private fun toExploreIgUiModel(ig: Ig): ExploreIgUiModel =
            ExploreIgUiModel(
                ig.name,
                ig.linkUrl
            )

    private fun toExploreWikiUiModel(wiki: Wiki): ExploreWikiUiModel =
            ExploreWikiUiModel(
                wiki.imageUrl,
                "Wikipedia",
                wiki.introduction,
                wiki.linkUrl
            )

    private fun toVideoUiModel(video: Video, read: Boolean): VideoUiModel =
            VideoUiModel(
                video.id,
                video.imageUrl,
                video.length,
                video.title,
                video.author,
                video.viewCount,
                video.date,
                read,
                video.linkUrl
            )

    private fun toHotelUiModel(hotel: Hotel): HotelUiModel =
            HotelUiModel(
                hotel.imageUrl,
                hotel.source,
                hotel.name,
                hotel.distance,
                hotel.rating,
                hotel.freeWifi,
                hotel.price,
                hotel.currency,
                hotel.freeCancellation,
                hotel.payAtProperty,
                hotel.linkUrl
            )
}