package org.mozilla.rocket.msrp.domain

import org.mozilla.rocket.msrp.data.MissionRepository

class ReadMissionUseCase(
    private val missionRepository: MissionRepository
) : UseCase<ReadMissionUseCaseParameter, Unit>() {

    override suspend fun execute(parameters: ReadMissionUseCaseParameter) {
        missionRepository.addReadMissionId(parameters.missionId)
    }
}

data class ReadMissionUseCaseParameter(
    val missionId: String
)