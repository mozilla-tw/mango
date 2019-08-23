package org.mozilla.rocket.shopping.search.data

import org.json.JSONObject
import org.mozilla.rocket.content.Result

class ShoppingSearchSiteRepository {

    suspend fun fetchSites(): Result<List<Site>> {
        return Result.Success(listOf(
            Site("Bukalapak", "https://www.bukalapak.com/products?utf8=✓&search%5Bkeywords%5D="),
            Site("Tokopedia", "https://www.tokopedia.com/search?st=product&q="),
            Site("JD.ID", "https://www.jd.id/search?keywords=")
        ))
    }

    suspend fun fetchPreferenceSites(): Result<List<PreferenceSite>> {
        return Result.Success(listOf(
            PreferenceSite("Lazada", "https://www.lazada.co.id", "https://www.lazada.co.id", toggleOn = false, toggleEnable = false, order = 5),
            PreferenceSite("Bukalapak", "https://www.bukalapak.com", "https://www.bukalapak.com", toggleOn = false, toggleEnable = false, order = 0),
            PreferenceSite("Tokopedia", "https://www.tokopedia.com/search?st=product&q=%s", "https://www.tokopedia.com", toggleOn = false, toggleEnable = false, order = 1),
            PreferenceSite("JD.ID", "https://www.jd.id/search?keywords=%s", "https://www.jd.id", toggleOn = false, toggleEnable = false, order = 2),
            PreferenceSite("Shopee", "https://shopee.co.id", "https://shopee.co.id", false, toggleEnable = false, order = 3),
            PreferenceSite("BliBli", "https://www.blibli.com", "https://www.blibli.com", false, toggleEnable = false, order = 4)
        ))
    }

    data class PreferenceSite(
        val title: String,
        val searchUrl: String,
        val displayUrl: String,
        var toggleOn: Boolean,
        var toggleEnable: Boolean,
        var order: Int
    ) {
        fun toJson(): JSONObject {
            val obj = JSONObject()
            obj.put("title", title)
            obj.put("searchUrl", searchUrl)
            obj.put("displayUrl", displayUrl)
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