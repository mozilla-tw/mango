package org.mozilla.rocket.content.travel.ui.adapter

import android.graphics.Bitmap
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import kotlinx.android.synthetic.main.item_hotel.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.travel.ui.HotelUiModel
import org.mozilla.rocket.extension.obtainBackgroundColor
import java.text.DecimalFormat

class HotelAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            HotelViewHolder(view)
}

class HotelViewHolder(
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val hotelUiModel = uiModel as HotelUiModel

        GlideApp.with(itemView.context)
                .asBitmap()
                .load(hotelUiModel.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.placeholder)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any, target: com.bumptech.glide.request.target.Target<Bitmap>, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Bitmap?, model: Any, target: com.bumptech.glide.request.target.Target<Bitmap>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        if (resource != null) {
                            hotel_image.setBackgroundColor(resource.obtainBackgroundColor())
                        }
                        return false
                    }
                })
                .into(hotel_image)

        hotel_source.text = hotelUiModel.source
        hotel_name.text = hotelUiModel.name
        hotel_distance.text = itemView.context.getString(R.string.travel_hotel_distance_to_landmark, hotelUiModel.distance)
        hotel_rating.text = itemView.context.getString(R.string.travel_hotel_rating_total, hotelUiModel.rating)
        hotel_currency.text = hotelUiModel.currency

        val dec = DecimalFormat("#,###.##")
        hotel_price.text = dec.format(hotelUiModel.price)

        hotel_free_wifi.isVisible = hotelUiModel.hasFreeWifi
        hotel_free_cancellation.isVisible = hotelUiModel.hasFreeCancellation
        hotel_pay_at_hotel.isVisible = hotelUiModel.canPayAtProperty

        hotel_separator.isVisible = hotelUiModel.hasFreeCancellation || hotelUiModel.canPayAtProperty
    }
}
