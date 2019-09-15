package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository
import org.mozilla.rocket.msrp.data.RedeemResult

class RedeemUseCase(private val missionRepository: MissionRepository) : UseCase<RedeemRequest, RedeemResult>() {
    override suspend fun execute(parameters: RedeemRequest): RedeemResult {
        return missionRepository.redeem(parameters.userToken, parameters.redeemUrl)
    }
}

class RedeemRequest(val userToken: String, val redeemUrl: String)