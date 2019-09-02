package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository.Site
import org.mozilla.rocket.content.Result
import java.net.URLEncoder

class SearchShoppingSiteUseCase(val repository: ShoppingSearchSiteRepository) {

    suspend operator fun invoke(searchKeyword: String): Result<List<Site>> {
        val sites = repository.fetchSites().value
        if (sites is Result.Success) {
            sites.data.apply {
                return Result.Success(this.map { site ->
                    Site(site.title, site.searchUrl + URLEncoder.encode(searchKeyword, "UTF-8"))
                }.toList())
            }
        }
        return Result.Success(listOf())
    }
}