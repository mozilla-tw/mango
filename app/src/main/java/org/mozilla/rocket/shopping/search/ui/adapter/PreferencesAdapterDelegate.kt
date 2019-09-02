package org.mozilla.rocket.shopping.search.ui.adapter

import android.graphics.Color
import android.view.View
import android.widget.CompoundButton
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_shopping_search_preference.*
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchPreferencesViewModel

class PreferencesAdapterDelegate(private val viewModel: ShoppingSearchPreferencesViewModel) : AdapterDelegate, ItemMoveCallback.ItemTouchHelperContract {

    override fun onRowClear(viewHolder: SiteViewHolder) {
        viewHolder.itemView.setBackgroundColor(Color.WHITE)
        viewModel.notifyItemDropped()
    }

    override fun onRowSelected(viewHolder: SiteViewHolder) {
        viewHolder.itemView.setBackgroundColor(Color.GRAY)
    }

    override fun onRowMoved(viewHolder: SiteViewHolder, target: SiteViewHolder) {
        val toPosition = target.adapterPosition
        val fromPosition = viewHolder.adapterPosition
        viewHolder.preference_site_switch.tag = toPosition
        target.preference_site_switch?.tag = fromPosition

        viewModel.notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        SiteViewHolder(view, viewModel)
}

class SiteViewHolder(
    override val containerView: View,
    viewModel: ShoppingSearchPreferencesViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    private val switchOnCheckChangeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        val index = buttonView.tag as Int
        viewModel.onItemSwitchChange(index, isChecked)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as PreferencesUiModel
        preference_site_name.text = uiModel.data.title
        preference_site_url.text = uiModel.data.displayUrl
        preference_site_switch.apply {
            tag = adapterPosition
            setOnCheckedChangeListener(null)
            isChecked = uiModel.data.isChecked
            isEnabled = uiModel.isEnabled
            setOnCheckedChangeListener(switchOnCheckChangeListener)
        }
    }

    data class PreferencesUiModel(val data: ShoppingSearchSiteRepository.PreferenceSite) : DelegateAdapter.UiModel() {
        var isEnabled = false
    }
}

class ItemMoveCallback(private val delegate: PreferencesAdapterDelegate) : ItemTouchHelper.Callback() {

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return if (viewHolder is SiteViewHolder && target is SiteViewHolder) {
            delegate.onRowMoved(viewHolder, target)
            true
        } else {
            false
        }
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder is SiteViewHolder) {
                delegate.onRowSelected(viewHolder)
            }
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (viewHolder is SiteViewHolder) {
            delegate.onRowClear(viewHolder)
        }
    }

    interface ItemTouchHelperContract {
        fun onRowMoved(viewHolder: SiteViewHolder, target: SiteViewHolder)
        fun onRowSelected(viewHolder: SiteViewHolder)
        fun onRowClear(viewHolder: SiteViewHolder)
    }
}