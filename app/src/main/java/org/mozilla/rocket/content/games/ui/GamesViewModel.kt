package org.mozilla.rocket.content.games.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.games.data.GamesRepo
import org.mozilla.rocket.content.games.ui.adapter.CarouselBanner
import org.mozilla.rocket.content.games.vo.Game
import org.mozilla.rocket.content.games.vo.GameCategory
import org.mozilla.rocket.download.SingleLiveEvent
import java.io.InputStream
import java.net.URL

class GamesViewModel(
    private val gamesRepo: GamesRepo
) : ViewModel() {

    private val uiScope = CoroutineScope(Dispatchers.Main)

    var needToRefreshMyGame = true

    val browserGamesState = MutableLiveData<State>()
    val browserGamesItems = MediatorLiveData<List<DelegateAdapter.UiModel>>()
    val premiumGamesItems = MediatorLiveData<List<DelegateAdapter.UiModel>>()
    var packageManager: PackageManager? = null

    private lateinit var _premiumBanner: LiveData<List<CarouselBannerAdapter.BannerItem>>
    private lateinit var _premiumGames: LiveData<List<GameCategory>>
    private lateinit var _browserBanner: LiveData<List<CarouselBannerAdapter.BannerItem>>
    private lateinit var _browserGames: LiveData<List<GameCategory>>

    var event = SingleLiveEvent<GameAction>()
    var gamePlayTimeEvent = SingleLiveEvent<Unit>()
    var createShortcutEvent = SingleLiveEvent<GameShortcut>()

    lateinit var selectedGame: Game

    fun initGameRepo(context: Context) {
        gamesRepo.setContext(context)
        gamesRepo.initGamesRepo()
    }

    fun addPackageManager(packageManager: PackageManager) {
        this.packageManager = packageManager
    }

    fun canShare(): Boolean {
        return true
    }

    fun canCreateShortCut(): Boolean {
        return selectedGame.type == "Browser"
    }

    fun canRemoveFromList(): Boolean {
        return selectedGame.recentplay == true
    }

    init {
        loadData()
    }

    private fun loadData() {
        launchDataLoad {
            _premiumBanner = gamesRepo.loadPremiumBanner()
            _premiumGames = gamesRepo.loadPremiumGames()
            _browserBanner = gamesRepo.loadBrowserBanner()
            _browserGames = gamesRepo.loadBrowserGames()

            browserGamesItems.addSource(_browserBanner) {
                var tmplist = mutableListOf<DelegateAdapter.UiModel>()
                tmplist.addAll(listOf(CarouselBanner(it)))
                tmplist.addAll(_browserGames.value!!)
                browserGamesItems.value = tmplist
            }

            browserGamesItems.addSource(_browserGames) {
                var tmplist = mutableListOf<DelegateAdapter.UiModel>()
                tmplist.addAll(listOf(CarouselBanner(_browserBanner.value!!)))
                tmplist.addAll(it)
                browserGamesItems.value = tmplist
            }

            premiumGamesItems.addSource(_premiumBanner) {
                var tmplist = mutableListOf<DelegateAdapter.UiModel>()
                tmplist.addAll(listOf(CarouselBanner(it)))
                tmplist.addAll(_premiumGames.value!!)
                premiumGamesItems.value = tmplist
            }

            premiumGamesItems.addSource(_premiumGames) {
                var tmplist = mutableListOf<DelegateAdapter.UiModel>()
                tmplist.addAll(listOf(CarouselBanner(_premiumBanner.value!!)))
                tmplist.addAll(it)
                premiumGamesItems.value = tmplist
            }
        }
    }

    fun onGameItemClicked(gameItem: Game) {
        when (gameItem.type) {
            "Premium" -> {
                event.value = GameAction.Install(gameItem.linkUrl)
                addRecentDownloadGame(gameItem)
            }
            "Browser" -> {
                event.value = GameAction.Play(gameItem.linkUrl)
                Handler().postDelayed({
                    addGameToRecentPlayList(gameItem)
                }, 1000)
            }
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        var found = true

        try {
            packageManager?.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            found = false
        }
        return found
    }

    fun setLatestRecentGameHintShown() {
        gamesRepo.setLatestRecentGameHintShown()
    }

    fun refreshPersonalGameLists(forceUpdateMyGame: Boolean = false) {
        if (needToRefreshMyGame || forceUpdateMyGame) {
            var downloadedGameList = gamesRepo.getDownloadedGameList()
            gamesRepo.cleanInstalledGame()
            for (game in downloadedGameList) {
                if (isPackageInstalled(game.packageName))
                    gamesRepo.addGameToMyGame(game)
            }
            needToRefreshMyGame = false
        }

        if (gamesRepo.isRecentPlayGameMoreThanThreeTimes()) {
            gamePlayTimeEvent.call()
        }
    }

    override fun onCleared() {
        super.onCleared()
        gamesRepo.saveLocalListToDb()
    }

    fun addRecentDownloadGame(gameItem: Game) {
        gamesRepo.insertRecentDownloadGame(gameItem)
    }

    fun addGameToRecentPlayList(gameItem: Game) {
        gamesRepo.insertRecentPlayGame(gameItem)
    }

    fun removeGameFromRecentPlayList(gameItem: Game) {
        gamesRepo.removeRecentPlayGame(gameItem)
    }

    fun onGameItemTouched(gameItem: Game): Boolean {
        selectedGame = gameItem
        return false
    }

    fun onBannerItemClicked(bannerItem: CarouselBannerAdapter.BannerItem) {
        event.value = GameAction.OpenLink(bannerItem.linkUrl)
    }

    fun onRefreshGameListButtonClicked() {
        // TODO: testing code, needs to be removed
        gamesRepo.saveLocalListToDb()
        browserGamesItems.value = emptyList()
        premiumGamesItems.value = emptyList()
        launchDataLoad {
            gamesRepo.refetchGamesRepo()
            refreshPersonalGameLists(forceUpdateMyGame = true)
        }
    }

    fun createShortCut() {
        uiScope.launch {
            val iconBitmap = withContext(Dispatchers.Default) {
                var inputStream = URL(selectedGame.imageUrl).getContent() as InputStream
                BitmapFactory.decodeStream(inputStream)
            }

            createShortcutEvent.value = GameShortcut(selectedGame.name, selectedGame.linkUrl, iconBitmap)
        }
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                browserGamesState.value = State.Loading
                block()
                browserGamesState.value = State.Idle
            } catch (t: Throwable) {
                browserGamesState.value = State.Error(t)
            }
        }
    }

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }

    sealed class GameAction {
        data class Play(val url: String) : GameAction()
        data class Install(val url: String) : GameAction()
        data class OpenLink(val url: String) : GameAction()
    }

    data class GameShortcut(val gameName: String, val gameUrl: String, val gameBitmap: Bitmap)
}