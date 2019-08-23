package org.mozilla.rocket.shopping.search.ui.adapter

import android.graphics.Color
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchPreferencesViewModel
import java.util.Collections

class PreferencesAdapterDelegate(private val viewModel: ShoppingSearchPreferencesViewModel) : AdapterDelegate, ItemMoveCallback.ItemTouchHelperContract {

    override fun onRowClear(viewHolder: SiteViewHolder) {
        viewHolder.itemView.setBackgroundColor(Color.WHITE)
    }

    override fun onRowSelected(viewHolder: SiteViewHolder) {
        viewHolder.itemView.setBackgroundColor(Color.GRAY)
    }

    override fun onRowMoved(viewHolder: SiteViewHolder, target: SiteViewHolder) {
        val toPosition = target.adapterPosition
        val fromPosition = viewHolder.adapterPosition
        val list = viewModel.sitesLiveData.value
        if (!list.isNullOrEmpty()) {

            viewHolder.switch?.tag = toPosition
            target.switch?.tag = fromPosition

            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(list, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(list, i, i - 1)
                }
            }
            viewModel.notifyItemMoved.value = ShoppingSearchPreferencesViewModel.Swap(fromPosition, toPosition)
        }
    }

    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
        SiteViewHolder(view, viewModel)
}

class SiteViewHolder(
    override val containerView: View,
    viewModel: ShoppingSearchPreferencesViewModel
) : DelegateAdapter.ViewHolder(containerView) {

    var url: TextView? = null
    var name: TextView? = null
    var drag: ImageView? = null
    var switch: SwitchCompat? = null

    private val switchOnCheckChangeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        val index = buttonView.tag as Int
        viewModel.switchCheckChange.value = ShoppingSearchPreferencesViewModel.SwitchCheck(index, isChecked)
    }

    init {
        url = itemView.findViewById(R.id.preference_site_url)
        name = itemView.findViewById(R.id.preference_site_name)
        drag = itemView.findViewById(R.id.preference_site_drag)
        switch = itemView.findViewById(R.id.preference_site_switch)
    }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as PreferencesUiModel
        name?.text = uiModel.data.title
        url?.text = uiModel.data.displayUrl
        switch?.tag = adapterPosition
        switch?.setOnCheckedChangeListener(null)
        switch?.isChecked = uiModel.data.toggleOn
        switch?.isEnabled = uiModel.data.toggleEnable
        switch?.setOnCheckedChangeListener(switchOnCheckChangeListener)
    }

    data class PreferencesUiModel(val data: ShoppingSearchSiteRepository.PreferenceSite) : DelegateAdapter.UiModel()
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