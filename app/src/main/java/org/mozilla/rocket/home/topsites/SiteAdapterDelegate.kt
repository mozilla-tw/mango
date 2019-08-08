package org.mozilla.rocket.home.topsites

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.os.StrictMode
import android.view.View
import androidx.core.view.ViewCompat
import kotlinx.android.synthetic.main.item_top_site.content_image
import kotlinx.android.synthetic.main.item_top_site.text
import org.mozilla.focus.utils.DimenUtils
import org.mozilla.icon.FavIconUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.home.HomeViewModel
import org.mozilla.strictmodeviolator.StrictModeViolation

class SiteAdapterDelegate(private val homeViewModel: HomeViewModel) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            SiteViewHolder(view, homeViewModel)
}

class SiteViewHolder(
    override val containerView: View,
    private val homeViewModel: HomeViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        val site = uiModel as Site
        text.text = site.title

        // Tried AsyncTask and other simple offloading, the performance drops significantly.
        // FIXME: 9/21/18 by saving bitmap color, cause FaviconUtils.getDominantColor runs slow.
        // Favicon
        val favicon = StrictModeViolation.tempGrant(
            { obj: StrictMode.ThreadPolicy.Builder -> obj.permitDiskReads() },
            { getFavicon(itemView.context, site) }
        )
        content_image.visibility = View.VISIBLE
        content_image.setImageBitmap(favicon)

        // Background color
        val backgroundColor = calculateBackgroundColor(favicon)
        ViewCompat.setBackgroundTintList(content_image, ColorStateList.valueOf(backgroundColor))

        itemView.setOnClickListener { homeViewModel.onTopSiteClicked(site) }
    }

    private fun getFavicon(context: Context, site: Site): Bitmap {
        val faviconUri = site.iconUri
        var favicon: Bitmap? = null
        if (faviconUri != null) {
            favicon = FavIconUtils.getBitmapFromUri(context, faviconUri)
        }

        return getBestFavicon(context.resources, site.url, favicon)
    }

    private fun getBestFavicon(res: Resources, url: String, favicon: Bitmap?): Bitmap {
        return when {
            favicon == null -> createFavicon(res, url, Color.WHITE)
            DimenUtils.iconTooBlurry(res, favicon.width) -> createFavicon(res, url, FavIconUtils.getDominantColor(favicon))
            else -> favicon
        }
    }

    private fun createFavicon(resources: Resources, url: String, backgroundColor: Int): Bitmap {
        return DimenUtils.getInitialBitmap(resources, FavIconUtils.getRepresentativeCharacter(url),
                backgroundColor)
    }

    private fun calculateBackgroundColor(favicon: Bitmap): Int {
        val dominantColor = FavIconUtils.getDominantColor(favicon)
        val alpha = dominantColor and -0x1000000
        // Add 25% white to dominant Color
        val red = addWhiteToColorCode(dominantColor and 0x00FF0000 shr 16, 0.25f) shl 16
        val green = addWhiteToColorCode(dominantColor and 0x0000FF00 shr 8, 0.25f) shl 8
        val blue = addWhiteToColorCode(dominantColor and 0x000000FF, 0.25f)
        return alpha + red + green + blue
    }

    private fun addWhiteToColorCode(colorCode: Int, percentage: Float): Int {
        var result = (colorCode + 0xFF * percentage / 2).toInt()
        if (result > 0xFF) {
            result = 0xFF
        }
        return result
    }
}

sealed class Site(open val title: String, open val url: String, open val iconUri: String?) : DelegateAdapter.UiModel() {
    data class FixedSite(
        override val title: String,
        override val url: String,
        override val iconUri: String?
    ) : Site(title, url, iconUri)

    data class RemovableSite(
        override val title: String,
        override val url: String,
        override val iconUri: String?
    ) : Site(title, url, iconUri)
}