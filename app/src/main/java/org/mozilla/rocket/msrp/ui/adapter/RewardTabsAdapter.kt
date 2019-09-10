package org.mozilla.rocket.msrp.ui.adapter

import android.content.res.Resources
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.mozilla.focus.R
import org.mozilla.rocket.msrp.ui.ChallengeListFragment
import org.mozilla.rocket.msrp.ui.RedeemListFragment

@Suppress("DEPRECATION")
class RewardTabsAdapter(
    fm: FragmentManager,
    val resources: Resources
) : FragmentPagerAdapter(fm) {

    private val items: List<TabItem> = getDefaultTabs()

    override fun getItem(position: Int): Fragment = items[position].fragment

    override fun getCount(): Int = items.size

    override fun getPageTitle(position: Int): CharSequence? = resources.getString(items[position].titleId)

    data class TabItem(
        val fragment: Fragment,
        val titleId: Int
    )

    companion object {
        private fun getDefaultTabs(): List<TabItem> = listOf(
                TabItem(ChallengeListFragment(), R.string.msrp_reward_category_1),
                TabItem(RedeemListFragment(), R.string.msrp_reward_category_2)
        )
    }
}