package org.mozilla.rocket.content.ecommerce.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HorizontalSpaceItemDecoration(private val spaceWidth: Int, private val paddingWidth: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager as LinearLayoutManager
        val position = parent.getChildLayoutPosition(view)
        outRect.left = if (position == 0) paddingWidth else spaceWidth
        outRect.right = if (position == layoutManager.itemCount - 1) paddingWidth else spaceWidth
    }
}