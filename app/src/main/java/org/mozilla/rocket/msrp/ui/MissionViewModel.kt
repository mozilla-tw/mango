package org.mozilla.rocket.msrp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionProgress
import org.mozilla.rocket.msrp.data.RewardCouponDoc
import org.mozilla.rocket.msrp.domain.HasUnreadMissionsUseCase
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCase
import org.mozilla.rocket.msrp.domain.ReadMissionUseCase
import org.mozilla.rocket.msrp.domain.RedeemUseCase
import org.mozilla.rocket.msrp.ui.adapter.MissionUiModel
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.TimeUtils
import org.mozilla.rocket.util.getNotNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MissionViewModel(
    private val loadMissionsUseCase: LoadMissionsUseCase,
    private val readMissionUseCase: ReadMissionUseCase,
    private val hasUnreadMissionsUseCase: HasUnreadMissionsUseCase,
    private val redeemUseCase: RedeemUseCase
) : ViewModel() {

    val challengeListViewState = MediatorLiveData<State>()
    val redeemListViewState = MediatorLiveData<State>()
    val hasUnreadMissions = MediatorLiveData<Boolean>()
    val redeemResult = MediatorLiveData<RewardCouponDoc>()

    val openMissionDetailPage = SingleLiveEvent<String>()

    private var missionsLiveData: LiveData<Result<Pair<List<Mission>, List<Mission>>, LoadMissionsUseCase.Error>>? = null
    private var challengeList: List<Mission> = emptyList()
    private var redeemList: List<Mission> = emptyList()

    init {
        loadMissions()
    }

    fun onRetryButtonClicked() {
        loadMissions()
    }

    private fun loadMissions() = viewModelScope.launch {
        challengeListViewState.value = State.Loading
        redeemListViewState.value = State.Loading

        val oldSource = missionsLiveData
        val newSource = loadMissionsUseCase()
        missionsLiveData = newSource

        oldSource?.let {
            challengeListViewState.removeSource(it)
            redeemListViewState.removeSource(it)
            hasUnreadMissions.removeSource(it)
        }

        challengeListViewState.addSource(newSource) { result ->
            viewModelScope.launch {
                challengeListViewState.value = parseChallengeListResult(result)
            }
        }
        redeemListViewState.addSource(newSource) { result ->
            viewModelScope.launch {
                redeemListViewState.value = parseRedeemListResult(result)
            }
        }
        hasUnreadMissions.addSource(newSource) { result ->
            val challengeList = result.data?.first ?: emptyList()
            hasUnreadMissions.value = hasUnreadMissionsUseCase(challengeList)
        }
    }

    private suspend fun parseChallengeListResult(
        result: Result<Pair<List<Mission>, List<Mission>>, LoadMissionsUseCase.Error>
    ): State {
        val (challengeList, _) = result.getNotNull { error ->
            return when (error) {
                is LoadMissionsUseCase.Error.NoConnectionError -> State.NoConnectionError
                is LoadMissionsUseCase.Error.UnknownError -> State.UnknownError
            }
        }
        this.challengeList = challengeList
        return if (challengeList.isNotEmpty()) {
            State.Loaded(challengeList.toUiModel())
        } else {
            State.Empty
        }
    }

    private suspend fun parseRedeemListResult(
        result: Result<Pair<List<Mission>, List<Mission>>, LoadMissionsUseCase.Error>
    ): State {
        val (_, redeemList) = result.getNotNull { error ->
            return when (error) {
                is LoadMissionsUseCase.Error.NoConnectionError -> State.NoConnectionError
                is LoadMissionsUseCase.Error.UnknownError -> State.UnknownError
            }
        }
        this.redeemList = redeemList
        return if (redeemList.isNotEmpty()) {
            State.Loaded(redeemList.toUiModel())
        } else {
            State.Empty
        }
    }

    fun onChallengeItemClicked(position: Int) {
        val mission = challengeList[position]
        // TODO: Evan
    }

    fun onRedeemItemClicked(position: Int) {
        val mission = redeemList[position]
        // TODO: Evan
    }

    fun onMissionDetailViewed(missionId: String) = viewModelScope.launch {
        readMissionUseCase(missionId)
    }

    // TODO: Evan
//    fun redeem(redeemUrl: String) = viewModelScope.launch {
//        _redeemResult.value = redeemUseCase.execute(RedeemRequest(redeemUrl)).getNotNull {
//            when (error) {
//                RedeemUseCase.Error.UnknownError -> {
//                    // TODO: Evan
//                }
//            }
//            return@launch
//        }
//    }

    sealed class State {
        data class Loaded(val data: List<MissionUiModel>) : State()
        object Empty : State()
        object Loading : State()
        object NoConnectionError : State()
        object UnknownError : State()
    }
}

private suspend fun List<Mission>.toUiModel(): List<MissionUiModel> = withContext(Dispatchers.Default) {
    map { it.toUiModel() }
}

private fun Mission.toUiModel(): MissionUiModel = when (status) {
    Mission.STATUS_NEW -> MissionUiModel.UnjoinedMission(
        title = title,
        expirationTime = joinEndDate.toDateString(),
        showRedDot = unread,
        imageUrl = imageUrl
    )
    Mission.STATUS_JOINED -> MissionUiModel.JoinedMission(
        title = title,
        expirationTime = expiredDate.toDateString(),
        imageUrl = imageUrl,
        progress = when (missionProgress) {
            is MissionProgress.TypeDaily -> { 100 * missionProgress.currentDay / missionProgress.totalDays }
            null -> error("missionProgress null")
        }
    )
    Mission.STATUS_REDEEMABLE -> {
        val expired = TimeUtils.getTimestampNow() > expiredDate
        if (expired) {
            MissionUiModel.ExpiredMission(
                title = title,
                expirationTime = expiredDate.toDateString()
            )
        } else {
            MissionUiModel.RedeemableMission(
                title = title
            )
        }
    }
    Mission.STATUS_REDEEMED -> MissionUiModel.RedeemedMission(
        title = title,
        expirationTime = redeemedDate.toDateString()
    )
    else -> error("unexpected mission status: $status")
}

private fun Long.toDateString(): String =
        SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault())
            .format(Date(this))