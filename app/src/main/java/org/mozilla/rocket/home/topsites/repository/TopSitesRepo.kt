package org.mozilla.rocket.home.topsites.repository

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.preference.PreferenceManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import org.json.JSONArray
import org.json.JSONException
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.history.BrowsingHistoryManager
import org.mozilla.focus.provider.HistoryContract
import org.mozilla.focus.provider.HistoryDatabaseHelper
import org.mozilla.focus.provider.QueryHandler
import org.mozilla.focus.utils.DimenUtils
import org.mozilla.focus.utils.TopSitesUtils
import org.mozilla.icon.FavIconUtils
import org.mozilla.rocket.home.pinsite.PinSiteManager
import org.mozilla.rocket.home.topsites.Site
import org.mozilla.rocket.persistance.History.HistoryDatabase
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Locale
import kotlin.math.min

open class TopSitesRepo(
    private val appContext: Context,
    private val pinSiteManager: PinSiteManager
) {

    private val _topSites = MutableLiveData<List<Site>>()
    val topSites: LiveData<List<Site>>
        get() = _topSites

    private val fixedSites: List<org.mozilla.focus.history.model.Site> by lazy { queryFixedSites() }
    private val uiHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_ID_REFRESH) {
                refreshTopSitesAsync()
            }
        }
    }

    fun updateTopSitesData() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        if (sharedPreferences.contains(TOP_SITES_V2_PREF)) {
            refreshTopSitesAsync()
        } else {
            Thread(MigrateHistoryRunnable(uiHandler, appContext)).start()
        }
    }

    private fun refreshTopSitesAsync() {
        getHistorySitesAsync { historySites ->
            _topSites.value = composeTopSites(
                fixedSites = fixedSites,
                pinnedSites = pinSiteManager.getPinSites(),
                historySites = historySites,
                defaultSites = getDefaultSites()
            )
        }
    }

    private fun composeTopSites(
        fixedSites: List<org.mozilla.focus.history.model.Site>,
        pinnedSites: List<org.mozilla.focus.history.model.Site>,
        historySites: List<org.mozilla.focus.history.model.Site>,
        defaultSites: List<org.mozilla.focus.history.model.Site>
    ): List<Site> {
        var result = fixedSites.toFixedSite() + pinnedSites.toRemovableSite()
        val spaces = TOP_SITES_SIZE - result.size
        result = result + mergeHistoryAndDefaultSites(historySites, defaultSites).let {
            if (it.isEmpty()) {
                it
            } else {
                it.take(min(spaces, it.size))
            }
        }.toRemovableSite()

        return result
    }

    private fun mergeHistoryAndDefaultSites(
        historySites: List<org.mozilla.focus.history.model.Site>,
        defaultSites: List<org.mozilla.focus.history.model.Site>
    ): List<org.mozilla.focus.history.model.Site> {
        val union = historySites + defaultSites
        val merged = union.groupBy { removeUrlPostSlash(it.url).toLowerCase(Locale.getDefault()) }
                .map {
                    val sameSiteGroup = it.value
                    if (sameSiteGroup.size == 1) {
                        sameSiteGroup.first()
                    } else {
                        var viewCount = 0L
                        var lastViewTimestamp = 0L
                        sameSiteGroup.forEach { site ->
                            viewCount += site.viewCount
                            if (site.lastViewTimestamp > lastViewTimestamp) {
                                lastViewTimestamp = site.lastViewTimestamp
                            }
                        }
                        sameSiteGroup.first().apply {
                            setViewCount(viewCount)
                            setLastViewTimestamp(lastViewTimestamp)
                        }
                    }
                }

        return merged.sortedWith(
            compareBy<org.mozilla.focus.history.model.Site> { it.viewCount }.thenBy { it.lastViewTimestamp }
        ).reversed()
    }

    private fun removeUrlPostSlash(url: String): String =
            if (url.isNotEmpty() && url[url.length - 1] == '/') {
                url.dropLast(1)
            } else {
                url
            }

    private fun queryFixedSites(): List<org.mozilla.focus.history.model.Site> {
        // TODO:
        return emptyList()
    }

    private fun getHistorySitesAsync(callback: (List<org.mozilla.focus.history.model.Site>) -> Unit) {
        BrowsingHistoryManager.getInstance()
                .queryTopSites(TOP_SITES_QUERY_LIMIT, TOP_SITES_QUERY_MIN_VIEW_COUNT) {
                    callback(it.filterIsInstance<org.mozilla.focus.history.model.Site>())
                }
    }

    private fun getDefaultSites(): List<org.mozilla.focus.history.model.Site> {
        // use different implementation to provide default top sites.
        val jsonString = getDefaultTopSitesJsonString()

        // if no default sites data in SharedPreferences, load data from assets.
        val jsonArray = if (jsonString != null) {
            try {
                JSONArray(jsonString)
            } catch (e: JSONException) {
                e.printStackTrace()
                null
            }
        } else {
            TopSitesUtils.getDefaultSitesJsonArrayFromAssets(appContext)
        }

        return TopSitesUtils.paresJsonToList(appContext, jsonArray)
    }

    // open for mocking during testing
    open fun getDefaultTopSitesJsonString(): String? {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
                .getString(TOP_SITES_PREF, null)
    }

    fun isPinEnabled(): Boolean = pinSiteManager.isEnabled()

    fun pin(site: Site) {
        pinSiteManager.pin(site.toSiteModel())
        // TODO: notify data changed
    }

    fun remove(site: Site) {
        // TODO:
//        if (site.getId() < 0) {
//            presenter.removeSite(site)
//            removeDefaultSites(site)
//            TopSitesUtils.saveDefaultSites(getContext(), this@HomeFragment.orginalDefaultSites)
//            refreshTopSites()
//            TelemetryWrapper.removeTopSite(true)
//        } else {
//            site.setViewCount(1)
//            BrowsingHistoryManager.getInstance().updateLastEntry(site, topSiteUpdateListener)
//            TelemetryWrapper.removeTopSite(false)
//        }
        pinSiteManager.unpinned(site.toSiteModel())
    }

    private class MigrateHistoryRunnable(handler: Handler, context: Context) : Runnable {

        private val handlerWeakReference: WeakReference<Handler> = WeakReference(handler)
        private val contextWeakReference: WeakReference<Context> = WeakReference(context)

        override fun run() {
            val context = contextWeakReference.get() ?: return

            val helper = HistoryDatabase.getInstance(context).openHelper
            val db = helper.writableDatabase
            // We can't differentiate if this is a new install or upgrade given the db version will
            // already become the latest version here. We create a temp table if no migration is
            // needed and later delete it to prevent crashing.
            db.execSQL(HistoryDatabase.CREATE_LEGACY_IF_NOT_EXIST)
            val builder = SupportSQLiteQueryBuilder.builder(HistoryDatabaseHelper.Tables.BROWSING_HISTORY_LEGACY)
            val columns = arrayOf(HistoryContract.BrowsingHistory._ID, HistoryContract.BrowsingHistory.URL, HistoryContract.BrowsingHistory.FAV_ICON)
            builder.columns(columns)
            val query = builder.create()
            val faviconFolder = FileUtils.getFaviconFolder(context)
            val urls = ArrayList<String>()
            val icons = ArrayList<ByteArray>()
            db.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    parseCursorToSite(cursor, urls, icons)
                }
                while (cursor.moveToNext()) {
                    parseCursorToSite(cursor, urls, icons)
                }
            }
            val handler = handlerWeakReference.get() ?: return
            if (icons.size == 0) {
                scheduleRefresh(handler)
            } else {
                // Refresh is still scheduled implicitly in SaveBitmapsTask
                FavIconUtils.SaveBitmapsTask(faviconFolder, urls, icons, UpdateHistoryWrapper(urls, handlerWeakReference),
                        Bitmap.CompressFormat.PNG, DimenUtils.PNG_QUALITY_DONT_CARE).execute()
            }
            db.execSQL("DROP TABLE " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY_LEGACY)
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(TOP_SITES_V2_PREF, true).apply()
        }

        private fun parseCursorToSite(cursor: Cursor, urls: MutableList<String>, icons: MutableList<ByteArray>) {
            val url = cursor.getString(cursor.getColumnIndex(HistoryContract.BrowsingHistory.URL))
            val icon = cursor.getBlob(cursor.getColumnIndex(HistoryContract.BrowsingHistory.FAV_ICON))
            urls.add(url)
            icons.add(icon)
        }

        private fun scheduleRefresh(handler: Handler) {
            val message = handler.obtainMessage(MSG_ID_REFRESH)
            handler.dispatchMessage(message)
        }
    }

    private class UpdateHistoryWrapper(
        private val urls: List<String>,
        private val handlerWeakReference: WeakReference<Handler>
    ) : FavIconUtils.Consumer<List<String>> {

        override fun accept(fileUris: List<String>) {
            val listener = QueryHandler.AsyncUpdateListener {
                val handler = handlerWeakReference.get() ?: return@AsyncUpdateListener
                scheduleRefresh(handler)
            }
            for (i in fileUris.indices) {
                if (i == fileUris.size - 1) {
                    BrowsingHistoryManager.updateHistory(null, urls[i], fileUris[i], listener)
                } else {
                    BrowsingHistoryManager.updateHistory(null, urls[i], fileUris[i])
                }
            }
        }

        private fun scheduleRefresh(handler: Handler) {
            val message = handler.obtainMessage(MSG_ID_REFRESH)
            handler.dispatchMessage(message)
        }
    }

    private fun List<org.mozilla.focus.history.model.Site>.toFixedSite(): List<Site> =
            map { it.toFixedSite() }

    private fun org.mozilla.focus.history.model.Site.toFixedSite(): Site =
            Site.FixedSite(
                    id = id,
                    title = title,
                    url = url,
                    iconUri = favIconUri,
                    viewCount = viewCount,
                    lastViewTimestamp = lastViewTimestamp
            )

    private fun List<org.mozilla.focus.history.model.Site>.toRemovableSite(): List<Site> =
            map { it.toRemovableSite() }

    private fun org.mozilla.focus.history.model.Site.toRemovableSite(): Site =
            Site.RemovableSite(
                    id = id,
                    title = title,
                    url = url,
                    iconUri = favIconUri,
                    viewCount = viewCount,
                    lastViewTimestamp = lastViewTimestamp,
                    isDefault = isDefault,
                    isPinned = pinSiteManager.isPinned(this)
            )

    private fun Site.toSiteModel(): org.mozilla.focus.history.model.Site =
            org.mozilla.focus.history.model.Site(
                    id,
                    title,
                    url,
                    viewCount,
                    lastViewTimestamp,
                    iconUri
            ).apply {
                isDefault = when (this@toSiteModel) {
                    is Site.FixedSite -> true
                    is Site.RemovableSite -> this@toSiteModel.isDefault
                }
            }

    companion object {
        private const val TOP_SITES_PREF = "topsites_pref"
        private const val TOP_SITES_V2_PREF = "top_sites_v2_complete"
        private const val TOP_SITES_SIZE = 16
        private const val TOP_SITES_QUERY_LIMIT = 12
        private const val TOP_SITES_QUERY_MIN_VIEW_COUNT = 2
        private const val MSG_ID_REFRESH = 8269
    }
}
