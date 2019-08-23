package org.mozilla.rocket.shopping.search.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchPreferencesViewModel.ShoppingSearchPreferencesUiModel
import androidx.recyclerview.widget.ItemTouchHelper
import org.mozilla.focus.R
import org.mozilla.rocket.shopping.search.data.ShoppingSearchSiteRepository
import java.util.Collections

class ShoppingSearchPreferencesSiteAdapter : RecyclerView.Adapter<SiteViewHolder>(), ItemMoveCallback.ItemTouchHelperContract {

    private lateinit var preferencesList: List<ShoppingSearchSiteRepository.PreferenceSite>
    private val switchOnCheckChangeListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
        val index = buttonView.tag as Int
        preferencesList[index].toggleOn = isChecked
        handleAllSwitch()
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val data = preferencesList[position]
        holder.bind(data)
        holder.switch?.tag = position
        holder.switch?.setOnCheckedChangeListener(null)
        holder.switch?.isChecked = data.toggleOn
        holder.switch?.isEnabled = data.toggleEnable
        holder.switch?.setOnCheckedChangeListener(switchOnCheckChangeListener)
    }

    override fun getItemCount(): Int {
        return if (!::preferencesList.isInitialized) {
            0
        } else {
            preferencesList.size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_shopping_search_preference, parent, false)
        return SiteViewHolder(view)
    }

    override fun onRowClear(viewHolder: SiteViewHolder) {
        viewHolder.itemView.setBackgroundColor(Color.WHITE)
    }

    override fun onRowSelected(viewHolder: SiteViewHolder) {
        viewHolder.itemView.setBackgroundColor(Color.GRAY)
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(preferencesList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(preferencesList, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun setData(list: ShoppingSearchPreferencesUiModel) {
        preferencesList = list.preferenceList
        handleAllSwitch()
    }

    private fun handleAllSwitch() {
        var toggleOnCounter = 0
        preferencesList.forEach {
            if (it.toggleOn) {
                toggleOnCounter++
            }
        }

        if (toggleOnCounter <= 2) {
            if (toggleOnCounter < 2) {
                preferencesList[0].toggleOn = true
                preferencesList[1].toggleOn = true
            }

            preferencesList.forEach {
                it.toggleEnable = true
                if (it.toggleOn) {
                    it.toggleEnable = false
                }
            }
        } else {
            preferencesList.forEach {
                it.toggleEnable = true
            }
        }
        notifyDataSetChanged()
    }
}

class SiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var url: TextView? = null
    var name: TextView? = null
    var drag: ImageView? = null
    var switch: SwitchCompat? = null

    init {
        url = itemView.findViewById(R.id.preference_site_url)
        name = itemView.findViewById(R.id.preference_site_name)
        drag = itemView.findViewById(R.id.preference_site_drag)
        switch = itemView.findViewById(R.id.preference_site_switch)
    }

    fun bind(data: ShoppingSearchSiteRepository.PreferenceSite) {
        name?.text = data.title
        url?.text = data.searchUrl
    }
}

class ItemMoveCallback(private val adapter: ShoppingSearchPreferencesSiteAdapter) : ItemTouchHelper.Callback() {

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        adapter.onRowMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
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
                adapter.onRowSelected(viewHolder)
            }
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (viewHolder is SiteViewHolder) {
            adapter.onRowClear(viewHolder)
        }
    }

    interface ItemTouchHelperContract {
        fun onRowMoved(fromPosition: Int, toPosition: Int)
        fun onRowSelected(viewHolder: SiteViewHolder)
        fun onRowClear(viewHolder: SiteViewHolder)
    }
}