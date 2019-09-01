package org.mozilla.rocket.content.games.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.common.adapter.CarouselBannerAdapter
import org.mozilla.rocket.content.games.data.GamesRepo
import org.mozilla.rocket.content.games.ui.adapter.GameItem

class GamesViewModel(
    private val gamesRepo: GamesRepo
) : ViewModel() {

    val browserGamesState = MutableLiveData<State>()
    val browserGamesItems = MutableLiveData<List<DelegateAdapter.UiModel>>()

    private val _event = MutableLiveData<GameAction>()
    val event: LiveData<GameAction> = _event
    lateinit var selectedGame: GameItem

    fun canShare(): Boolean {
        return true
    }

    fun canCreateShortCut(): Boolean {
        return if (selectedGame.type == "Browser") true else false
    }

    fun canRemoveFromList(): Boolean {
        return if (selectedGame.recentplay == true) true else false
    }

    fun canUninstall(): Boolean {
        return if (selectedGame.type == "Premium") true else false
    }

    init {
        loadData()
    }

    private fun loadData() {
        launchDataLoad {
            browserGamesItems.value = gamesRepo.getFakeData()
        }
    }

    fun onGameItemClicked(gameItem: GameItem) {
        _event.value = GameAction.Play(gameItem.linkUrl)
    }

    fun onGameItemLongClicked(gameItem: GameItem): Boolean {
        selectedGame = gameItem
        return false
    }

    fun onBannerItemClicked(bannerItem: CarouselBannerAdapter.BannerItem) {
        _event.value = GameAction.Play(bannerItem.linkUrl)
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
        data class Uninstall(val url: String) : GameAction()
        data class Remove(val id: String) : GameAction()
    }
}