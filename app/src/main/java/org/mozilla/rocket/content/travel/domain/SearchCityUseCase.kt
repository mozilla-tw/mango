package org.mozilla.rocket.content.travel.domain

import android.text.Spannable
import android.text.SpannableStringBuilder
import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.Result.Error
import org.mozilla.rocket.content.Result.Success
import org.mozilla.rocket.content.travel.data.City
import org.mozilla.rocket.content.travel.data.TravelRepository
import org.mozilla.rocket.content.travel.ui.CitySearchResultUiModel
import java.util.Locale

class SearchCityUseCase(private val travelRepository: TravelRepository) {

    suspend operator fun invoke(keyword: String): Result<List<CitySearchResultUiModel>> {
        val result = travelRepository.searchCity(keyword)
        if (result is Success) {
            return Success(result.data.map {
                toCitySearchResultUiModel(keyword, it)
            })
        }

        return Error(Exception("Fail to search city"))
    }

    private fun toCitySearchResultUiModel(keyword: String, city: City): CitySearchResultUiModel =
            CitySearchResultUiModel(
                city.id,
                applyStyle(keyword, city.name)
            )

    private fun applyStyle(keyword: String, keywordSerchResult: String): CharSequence {
        val idx = keywordSerchResult.toLowerCase(Locale.getDefault()).indexOf(keyword)
        if (idx != -1) {
            return SpannableStringBuilder(keywordSerchResult).apply {
                setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        idx,
                        idx + keyword.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            return keywordSerchResult
        }
    }
}