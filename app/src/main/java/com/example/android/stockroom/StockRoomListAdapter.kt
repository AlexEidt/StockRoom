/*
 * Copyright (C) 2017 Google Inc.
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

package com.example.android.stockroom

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recyclerview_item.view.item_summary1
import kotlinx.android.synthetic.main.recyclerview_item.view.item_summary2
import kotlinx.android.synthetic.main.recyclerview_item.view.itemview_group
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import kotlin.math.absoluteValue

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

fun setBackgroundColor(
  view: View,
  color: Int
) {
  // Keep the corner radii and only change the background color.
  val gradientDrawable = view.background as GradientDrawable
  gradientDrawable.setColor(color)
  view.background = gradientDrawable
}

class StockRoomListAdapter internal constructor(
  val context: Context,
  private val clickListenerGroup: (StockItem, View) -> Unit,
  private val clickListenerSummary: (StockItem) -> Unit
) : ListAdapter<StockItem, StockRoomListAdapter.StockRoomViewHolder>(StockRoomDiffCallback()) {
  private val inflater: LayoutInflater = LayoutInflater.from(context)

  inner class StockRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindGroup(
      stockItem: StockItem,
      clickListener: (StockItem, View) -> Unit
    ) {
      itemView.itemview_group.setOnClickListener { clickListener(stockItem, itemView) }
    }

    fun bindSummary(
      stockItem: StockItem,
      clickListener: (StockItem) -> Unit
    ) {
      itemView.item_summary1.setOnClickListener { clickListener(stockItem) }
      itemView.item_summary2.setOnClickListener { clickListener(stockItem) }
    }

    val itemViewSymbol: TextView = itemView.findViewById(R.id.textViewSymbol)
    val itemViewName: TextView = itemView.findViewById(R.id.textViewName)
    val itemViewMarketPrice: TextView = itemView.findViewById(R.id.textViewMarketPrice)
    val itemViewChange: TextView = itemView.findViewById(R.id.textViewChange)
    val itemViewChangePercent: TextView = itemView.findViewById(R.id.textViewChangePercent)
    val itemViewAssets: TextView = itemView.findViewById(R.id.textViewAssets)
    val itemTextViewGroup: TextView = itemView.findViewById(R.id.itemview_group)
    val itemSummary: ConstraintLayout = itemView.findViewById(R.id.item_summary1)
    val itemRedGreen: ConstraintLayout = itemView.findViewById(R.id.item_summary2)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): StockRoomViewHolder {
    val itemView = inflater.inflate(R.layout.recyclerview_item, parent, false)
    return StockRoomViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: StockRoomViewHolder,
    position: Int
  ) {
    val current = getItem(position)
    if (current != null) {
      holder.bindGroup(current, clickListenerGroup)
      holder.bindSummary(current, clickListenerSummary)

      holder.itemSummary.setBackgroundColor(context.getColor(R.color.backgroundListColor))

      holder.itemViewSymbol.text = current.onlineMarketData.symbol
      holder.itemViewName.text = current.onlineMarketData.name
      if (current.onlineMarketData.marketPrice > 0f) {
        holder.itemViewMarketPrice.text =
          if (current.onlineMarketData.marketPrice > 5f) {
            DecimalFormat("0.00").format(current.onlineMarketData.marketPrice)
          } else {
            DecimalFormat("0.00##").format(current.onlineMarketData.marketPrice)
          }
        holder.itemViewChange.text =
          DecimalFormat("0.00##").format(current.onlineMarketData.marketChange)
        holder.itemViewChangePercent.text =
          "(${DecimalFormat("0.00").format(
              current.onlineMarketData.marketChangePercent
          )}%)"
      } else {
        holder.itemViewMarketPrice.text = ""
        holder.itemViewChange.text = ""
        holder.itemViewChangePercent.text = ""
        holder.itemViewAssets.text = ""
      }

      val shares = current.assets.sumByDouble {
        it.shares.toDouble()
      }
          .toFloat()

      var assets: String = ""

      var asset: Float = 0f
      var capital: Float = 0f

      if (shares > 0f) {
        asset = current.assets.sumByDouble {
          it.shares.toDouble() * it.price
        }
            .toFloat()

        if (current.onlineMarketData.marketPrice > 0f) {
          capital = current.assets.sumByDouble {
            it.shares.toDouble() * current.onlineMarketData.marketPrice
          }
              .toFloat()

          assets += "${DecimalFormat(
              "0.00"
          ).format(asset)} ${if (capital >= asset) {
            "+"
          } else {
            "-"
          }} ${DecimalFormat("0.00").format(
              (capital - asset).absoluteValue
          )} = ${DecimalFormat(
              "0.00"
          ).format(
              capital
          )}"

          val capitalPercent = (capital - asset) * 100f / asset
          assets += " (${if (capitalPercent >= 0f) {
            "+"
          } else {
            ""
          }}${DecimalFormat("0.00").format(capitalPercent)}%)"

        } else {
          assets += DecimalFormat(
              "0.00"
          ).format(asset)
        }
      }

      when {
        capital > asset -> {
          holder.itemRedGreen.setBackgroundColor(context.getColor(R.color.green))
        }
        capital > 0f && capital < asset -> {
          holder.itemRedGreen.setBackgroundColor(context.getColor(R.color.red))
        }
        else -> {
          holder.itemRedGreen.setBackgroundColor(context.getColor(R.color.backgroundListColor))
        }
      }

      if (current.onlineMarketData.annualDividendRate > 0f) {
        assets +=
          "\n${context.resources.getString(R.string.dividend_in_list)} ${DecimalFormat(
              "0.00"
          ).format(
              current.onlineMarketData.annualDividendRate
          )} (${DecimalFormat("0.00").format(
              current.onlineMarketData.annualDividendYield * 100
          )}%)"
      }

      if (current.stockDBdata.alertAbove > 0f) {
        assets += "\n${context.resources.getString(R.string.alert_above_in_list)} ${DecimalFormat(
            "0.####"
        ).format(current.stockDBdata.alertAbove)}"
      }
      if (current.stockDBdata.alertBelow > 0f) {
        assets += "\n${context.resources.getString(R.string.alert_below_in_list)} ${DecimalFormat(
            "0.####"
        ).format(current.stockDBdata.alertBelow)}"
      }
      if (current.events.isNotEmpty()) {
        val count = current.events.size
        val eventstr = context.resources.getQuantityString(R.plurals.events_in_list, count, count)

        assets += "\n$eventstr:"
        current.events.forEach {
          val localDateTime = LocalDateTime.ofEpochSecond(it.datetime, 0, ZoneOffset.UTC)
          val datetime = localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(MEDIUM))
          assets += "\n${context.resources.getString(
              R.string.event_datetime_format, it.title, datetime
          )}"
        }
      }
      if (current.stockDBdata.notes.isNotEmpty()) {
        assets += "\n${context.resources.getString(
            R.string.notes_in_list
        )} ${current.stockDBdata.notes}"
      }

      holder.itemViewAssets.text = assets

      var color = current.stockDBdata.groupColor
      if (color == 0) {
        color = context.getColor(R.color.backgroundListColor)
      }
      setBackgroundColor(holder.itemTextViewGroup, color)

      /*
      // Keep the corner radii and only change the background color.
      val gradientDrawable = holder.itemLinearLayoutGroup.background as GradientDrawable
      gradientDrawable.setColor(color)
      holder.itemLinearLayoutGroup.background = gradientDrawable
      */
    }
  }

  internal fun setStockItems(stockItems: List<StockItem>) {
    submitList(stockItems)
    notifyDataSetChanged()
  }
}

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#3

class StockRoomDiffCallback : DiffUtil.ItemCallback<StockItem>() {
  override fun areItemsTheSame(
    oldItem: StockItem,
    newItem: StockItem
  ): Boolean {
    return oldItem.onlineMarketData.symbol == newItem.onlineMarketData.symbol
  }

  override fun areContentsTheSame(
    oldItem: StockItem,
    newItem: StockItem
  ): Boolean {
    return oldItem == newItem
  }
}