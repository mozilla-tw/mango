package org.mozilla.rocket.content.games.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.games.vo.Game
import org.mozilla.rocket.content.games.vo.GameCategory
import org.json.JSONArray
import org.mozilla.fileutils.FileUtils
import java.util.Scanner
import java.io.ObjectInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.ObjectOutputStream

class GamesRepo {

    companion object {
        private const val PREF_NAME = "games_cache"
        private const val KEY_JSON_RECENTLY_PLAY_GAMES = "recently_play_games"
        private const val KEY_JSON_DOWNLOADED_GAMES = "downloaded_games"
        private const val PLAYTIMES_FILENAME = "playtimes_games"
        private const val GAMES_CACHE_FILENAME = "gameRepo.json"
    }
    private lateinit var context: Context
    private var isInit = false

    var gamePlayTimesMap = mutableMapOf<Long, Int>()

    // mutablelist to cache Gamelist
    private var _browserGameCategoryList = mutableListOf<GameCategory>()
    private var _premiumGameCategoryList = mutableListOf<GameCategory>()

    // mutable LiveData
    private var _premiumBanner: MutableLiveData<List<CarouselBannerAdapter.BannerItem>> = MutableLiveData()
    private var _premiumGameCategories: MutableLiveData<List<GameCategory>> = MutableLiveData()
    private var _browserBanner: MutableLiveData<List<CarouselBannerAdapter.BannerItem>> = MutableLiveData()
    private var _browserGameCategories: MutableLiveData<List<GameCategory>> = MutableLiveData()

    // LiveData to return to viewmodel
    private var premiumBanner: LiveData<List<CarouselBannerAdapter.BannerItem>> = _premiumBanner
    private var premiumGameCategories: LiveData<List<GameCategory>> = _premiumGameCategories
    private var browserBanner: LiveData<List<CarouselBannerAdapter.BannerItem>> = _browserBanner
    private var browserGameCategories: LiveData<List<GameCategory>> = _browserGameCategories

    // Recently play and Installed game list
    private var _downloadedPremiumGamelist = mutableListOf<Game>()
    private var _installedPremiumGamelist = mutableListOf<Game>()
    private var _recentBrowserGamelist = mutableListOf<Game>()

    // API for viewmodel to get data
    fun loadPremiumGames() = premiumGameCategories
    fun loadPremiumBanner() = premiumBanner
    fun loadBrowserGames() = browserGameCategories
    fun loadBrowserBanner() = browserBanner

    fun getDownloadedGameList() = _downloadedPremiumGamelist

    fun cleanInstalledGame() {
        if (_installedPremiumGamelist.count() > 0) {
            _premiumGameCategoryList.removeAt(0)
            _premiumGameCategories.value = _premiumGameCategoryList
        }
        _installedPremiumGamelist.clear()
    }

    fun insertRecentDownloadGame(game: Game) {
        if (game.type == "Premium") {
            if (_downloadedPremiumGamelist.contains(game)) {
                _downloadedPremiumGamelist.remove(game)
            }
            _downloadedPremiumGamelist.add(0, game)
        }
    }

    fun addGameToMyGame(game: Game) {
        if (_downloadedPremiumGamelist.contains(game) && game.type == "Premium") {
            if (_installedPremiumGamelist.count() == 0) {
                _installedPremiumGamelist.add(game)
                _premiumGameCategoryList.add(0, GameCategory("My Games", _installedPremiumGamelist))
            } else if (_installedPremiumGamelist.contains(game) == false) {
                _installedPremiumGamelist.add(game)
            }
            _premiumGameCategories.value = _premiumGameCategoryList
        }
    }

    fun removeGameFromMyGame(game: Game) {
        if (game.type == "Premium" && _installedPremiumGamelist.contains(game)) {
            _installedPremiumGamelist.remove(game)
            _downloadedPremiumGamelist.remove(game)

            if (_installedPremiumGamelist.count() == 0) {
                _premiumGameCategoryList.removeAt(0)
            }
            _premiumGameCategories.value = _premiumGameCategoryList
        }
    }

    fun removeRecentPlayGame(game: Game) {
        if (game.type == "Browser" && _recentBrowserGamelist.contains(game)) {
                        _recentBrowserGamelist.remove(game)
                        if (_recentBrowserGamelist.count() == 0) {
                            _browserGameCategoryList.removeAt(0)
                        }
                _browserGameCategories.value = _browserGameCategoryList
            }
        }

    fun insertRecentPlayGame(_game: Game) {
        var game = Game(_game.id, _game.name, _game.imageUrl, _game.linkUrl, "", _game.type, true)
        if (game.type == "Browser") {
                if (_recentBrowserGamelist.count() == 0) {
                    _recentBrowserGamelist.add(game)
                _browserGameCategoryList.add(0, GameCategory("Recently played", _recentBrowserGamelist))
                } else {
                    if (_recentBrowserGamelist.contains(game)) {
                        _recentBrowserGamelist.remove(game)
                    }
                    _recentBrowserGamelist.add(0, game)
                }
            _browserGameCategories.value = _browserGameCategoryList
        }
        if (_game.id in gamePlayTimesMap) {
            gamePlayTimesMap[_game.id] = gamePlayTimesMap[_game.id]!!.inc()
        } else {
            gamePlayTimesMap[_game.id] = 1
        }
    }

    fun saveLocalListToDb() {
        if (_recentBrowserGamelist.count() > 0) {
            var recentplay = _recentBrowserGamelist.toString()
            getPreferences().edit().putString(GamesRepo.KEY_JSON_RECENTLY_PLAY_GAMES, recentplay).apply()
        }

        if (_downloadedPremiumGamelist.count() > 0) {
            var downloadlist = _downloadedPremiumGamelist.toString()
            getPreferences().edit().putString(GamesRepo.KEY_JSON_DOWNLOADED_GAMES, downloadlist).apply()
        }

        if (gamePlayTimesMap.count() > 0) {
            var dir = context.cacheDir
            val file = File(dir, PLAYTIMES_FILENAME)

            val outputStream = ObjectOutputStream(FileOutputStream(file))
            outputStream.writeObject(gamePlayTimesMap)
            outputStream.flush()
            outputStream.close()
        }
    }

    private fun getPreferences(): SharedPreferences {
        return context.getSharedPreferences(GamesRepo.PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setContext(context: Context) {
        this.context = context
    }

    fun isRecentPlayGameMoreThanThreeTimes(): Boolean {
        if (_recentBrowserGamelist.count() > 0 && _recentBrowserGamelist[0].id in gamePlayTimesMap) {
            if (gamePlayTimesMap[_recentBrowserGamelist[0].id]!!.toInt()> 2 && gamePlayTimesMap[_recentBrowserGamelist[0].id]!!.toInt() < 100) {
                return true
            }
        }
        return false
    }

    fun setLatestRecentGameHintShown() {
        if (_recentBrowserGamelist.count() > 0 && _recentBrowserGamelist[0].id in gamePlayTimesMap) {
            gamePlayTimesMap[_recentBrowserGamelist[0].id] = 100
        }
    }

    fun refetchGamesRepo() {
        isInit = false
        initGamesRepo()
    }

    fun initGamesRepo() {
        if (isInit == true) {
            return
        }
        _installedPremiumGamelist = mutableListOf<Game>()

        var dir = context.cacheDir
        var target = File(dir, GamesRepo.GAMES_CACHE_FILENAME)
        lateinit var jsonString: String
        if (!target.exists()) {
        val inputStream = this.javaClass.classLoader!!.getResourceAsStream("res/raw/gamedata.json")
            jsonString = Scanner(inputStream).useDelimiter("\\A").next()
            FileUtils.writeStringToFile(dir, GamesRepo.GAMES_CACHE_FILENAME, jsonString)
        } else {
            jsonString = FileUtils.readStringFromFile(dir, GamesRepo.GAMES_CACHE_FILENAME)
        }

        val jsonArray = JSONArray(jsonString)

        for (i in 0..(jsonArray.length() - 1)) {
            var _bannerList = mutableListOf<CarouselBannerAdapter.BannerItem>()
            var _gameList = mutableListOf<GameCategory>()
            var gamesdb = jsonArray.getJSONObject(i)
            val gameType = gamesdb.optString("type")
            val banners = gamesdb.optJSONArray("banner")
            val gamelists = gamesdb.optJSONArray("gamelist")

            // banners
            for (j in 0..(banners.length() - 1)) {
                val banner = banners.optJSONObject(j)
                _bannerList.add(CarouselBannerAdapter.BannerItem(banner.optString("id"), banner.optString("imageUrl"), banner.optString("linkUrl")))
    }

            // game categories
            for (j in 0..(gamelists.length() - 1)) {
                val gamelist = gamelists.optJSONObject(j)
                val gameCategory = gamelist.optString("type")
                val games = gamelist.optJSONArray("games")
                var _games = mutableListOf<Game>()
                // games
                for (k in 0..(games.length() - 1)) {
                    val game = games.optJSONObject(k)

                    _games.add(Game(game.optLong("id"),
                            game.optString("name"),
                            game.optString("imageUrl"),
                            game.optString("linkUrl"),
                            game.optString("packageName"),
                            gameType,
                            false))
                }
                _gameList.add(GameCategory(gameCategory, _games))
            }

            when (gameType) {
                "Premium" -> {
                    _premiumBanner.value = _bannerList
                    _premiumGameCategoryList = _gameList
                    _premiumGameCategories.value = _premiumGameCategoryList.toList()
                }
                "Browser" -> {
                    _browserBanner.value = _bannerList
                    _browserGameCategoryList = _gameList
                    _browserGameCategories.value = _browserGameCategoryList.toList()
                }
            }
        }

        val recentlyplay = getPreferences()
                .getString(GamesRepo.KEY_JSON_RECENTLY_PLAY_GAMES, "") ?: ""
        if (recentlyplay != "") {
            var recentyplayArray = JSONArray(recentlyplay)
            _recentBrowserGamelist.clear()
            for (i in 0..(recentyplayArray.length() - 1)) {
                val game = recentyplayArray.optJSONObject(i)

                _recentBrowserGamelist.add(Game(game.optLong("id"),
                        game.optString("name"),
                        game.optString("imageUrl"),
                        game.optString("linkUrl"),
                        game.optString("packageName"),
                        "Browser",
                        true))
            }
            if (_recentBrowserGamelist.count() > 0) {
                _browserGameCategoryList.add(0, GameCategory("Recently played", _recentBrowserGamelist))
                _browserGameCategories.value = _browserGameCategoryList
            }
        }

        val downloadlist = getPreferences()
                .getString(GamesRepo.KEY_JSON_DOWNLOADED_GAMES, "") ?: ""

        if (downloadlist != "") {
            var downloadlistArray = JSONArray(downloadlist)
            _downloadedPremiumGamelist.clear()
            for (i in 0..(downloadlistArray.length() - 1)) {
                val game = downloadlistArray.optJSONObject(i)

                _downloadedPremiumGamelist.add(Game(game.optLong("id"),
                        game.optString("name"),
                        game.optString("imageUrl"),
                        game.optString("linkUrl"),
                        game.optString("packageName"),
                        "Premium",
                        false))
            }
        }

        // read playtime map back from cache
        val playtimesCache = File(dir, GamesRepo.PLAYTIMES_FILENAME)
        if (playtimesCache.exists()) {
            val inputStream = ObjectInputStream(FileInputStream(playtimesCache))
            @Suppress("UNCHECKED_CAST")
            gamePlayTimesMap = inputStream.readObject() as MutableMap<Long, Int>
            inputStream.close()
        }

        isInit = true
    }
}
