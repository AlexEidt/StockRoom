/*
 * Copyright (C) 2020
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thecloudsite.stockroom

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.italic
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.utils.getChangeColor
import com.thecloudsite.stockroom.utils.getMarketValues
import kotlinx.android.synthetic.main.stockroomsmalllist_item.view.smalllist_item_layout

class StockRoomSmallListAdapter internal constructor(
  val context: Context,
  private val clickListenerSummary: (StockItem) -> Unit
) : ListAdapter<StockItem, StockRoomSmallListAdapter.StockRoomViewHolder>(
    StockRoomDiffCallback()
) {
  private val inflater: LayoutInflater = LayoutInflater.from(context)

  class StockRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindSummary(
      stockItem: StockItem,
      clickListener: (StockItem) -> Unit
    ) {
      itemView.smalllist_item_layout.setOnClickListener { clickListener(stockItem) }
    }

    val itemViewSymbol: TextView = itemView.findViewById(R.id.smalllist_textViewSymbol)
    val itemViewMarketPrice: TextView = itemView.findViewById(R.id.smalllist_textViewMarketPrice)
    val itemViewMarketPriceLayout: ConstraintLayout =
      itemView.findViewById(R.id.smalllist_textViewMarketPriceLayout)
    val itemTextViewGroup: TextView = itemView.findViewById(R.id.smalllist_itemview_group)
    val itemSummary: ConstraintLayout = itemView.findViewById(R.id.smalllist_item_layout)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): StockRoomViewHolder {
    val itemView = inflater.inflate(R.layout.stockroomsmalllist_item, parent, false)
    return StockRoomViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: StockRoomViewHolder,
    position: Int
  ) {
    val current = getItem(position)
    if (current != null) {
      holder.bindSummary(current, clickListenerSummary)

      holder.itemSummary.setBackgroundColor(context.getColor(R.color.backgroundListColor))

      holder.itemViewSymbol.text = current.onlineMarketData.symbol

      if (current.onlineMarketData.marketPrice > 0.0) {
        val marketValues = getMarketValues(current.onlineMarketData)
        val marketPriceStr = "${marketValues.first} ${marketValues.second} ${marketValues.third}"

        if (current.onlineMarketData.postMarketData) {
          holder.itemViewMarketPrice.text = SpannableStringBuilder()
              .italic { append(marketPriceStr) }
        } else {
          holder.itemViewMarketPrice.text = marketPriceStr
        }
      } else {
        holder.itemViewMarketPrice.text = ""
      }

      // In one-line view set the background color to the market change instead of the asset change.
      holder.itemViewMarketPriceLayout.setBackgroundColor(
          getChangeColor(current.onlineMarketData.marketChange, context)
      )

      var color = current.stockDBdata.groupColor
      if (color == 0) {
        color = context.getColor(R.color.backgroundListColor)
      }
      setBackgroundColor(holder.itemTextViewGroup, color)
    }
  }

  internal fun setStockItems(stockItemSet: StockItemSet) {
    submitList(stockItemSet.stockItems)
    notifyDataSetChanged()
  }
}