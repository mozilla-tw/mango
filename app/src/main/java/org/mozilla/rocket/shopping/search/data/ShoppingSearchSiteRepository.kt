package org.mozilla.rocket.shopping.search.data

import org.json.JSONObject
import org.mozilla.rocket.content.Result

class ShoppingSearchSiteRepository {

    suspend fun fetchSites(): Result<List<Site>> {
        return Result.Success(listOf(
            // Site("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D=%s"),
            Site("Tokopedia", "https://www.tokopedia.com/search?st=product&q=%s"),
            Site("JD.ID", "https://www.jd.id/search?keywords=%s")
        ))
    }

    suspend fun fetchPreferenceSites(): Result<List<PreferenceSite>> {
        return Result.Success(listOf(
            PreferenceSite("Lazada", "https://www.lazada.co.id", toggleOn = false, toggleEnable = false, order = 5),
            PreferenceSite("Bukalapak", "https://www.bukalapak.com", toggleOn = false, toggleEnable = false, order = 0),
            PreferenceSite("Tokopedia", "https://www.tokopedia.com/search?st=product&q=%s", toggleOn = false, toggleEnable = false, order = 1),
            PreferenceSite("JD.ID", "https://www.jd.id/search?keywords=%s", toggleOn = false, toggleEnable = false, order = 2),
            PreferenceSite("Shopee", "https://shopee.co.id", false, toggleEnable = false, order = 3),
            PreferenceSite("BliBli", "https://www.blibli.com", false, toggleEnable = false, order = 4)

        ))
    }

    data class PreferenceSite(
        val title: String,
        val searchUrl: String,
        var toggleOn: Boolean,
        var toggleEnable: Boolean,
        var order: Int
    ) {
        fun toJson(): JSONObject {
            val obj = JSONObject()
            obj.put("title", title)
            obj.put("searchUrl", searchUrl)
            obj.put("toggleOn", toggleOn)
            obj.put("toggleEnable", toggleEnable)
            obj.put("order", order)
            return obj
        }
    }

    data class Site(
        val title: String,
        val searchUrl: String
    )
}