package org.mozilla.rocket.shopping.search.domain

import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.ui.adapter.SiteViewHolder
import java.util.Collections

class UpdatePreferenceShoppingSiteUseCase(val repository: ShoppingSearchSiteRepository) {
    fun update(list: MutableList<SiteViewHolder.PreferencesUiModel>, fromPosition: Int, toPosition: Int) {
        if (!list.isNullOrEmpty()) {

            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(list, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(list, i, i - 1)
                }
            }
        }
    }
}