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

package com.thecloudsite.stockroom

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.dividend_received_view_item.view.dividendReceivedLinearLayout
import kotlinx.android.synthetic.main.dividend_received_view_item.view.textViewDividendReceivedDelete
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class DividendReceivedListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (Dividend) -> Unit,
  private val clickListenerDelete: (String?, Dividend?) -> Unit
) : RecyclerView.Adapter<DividendReceivedListAdapter.DividendReceivedViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var dividendList = mutableListOf<Dividend>()

  inner class DividendReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindUpdate(
      dividend: Dividend,
      clickListenerUpdate: (Dividend) -> Unit
    ) {
      itemView.dividendReceivedLinearLayout.setOnClickListener { clickListenerUpdate(dividend) }
    }

    fun bindDelete(
      symbol: String?,
      dividend: Dividend?,
      clickListenerDelete: (String?, Dividend?) -> Unit
    ) {
      itemView.textViewDividendReceivedDelete.setOnClickListener { clickListenerDelete(symbol, dividend) }
    }

    val textViewDividendReceivedAmount: TextView = itemView.findViewById(R.id.textViewDividendReceivedAmount)
    val textViewDividendReceivedDate: TextView = itemView.findViewById(R.id.textViewDividendReceivedDate)
    val textViewDividendReceivedDelete: TextView = itemView.findViewById(R.id.textViewDividendReceivedDelete)
    val dividendReceivedSummaryView: LinearLayout = itemView.findViewById(R.id.dividendReceivedSummaryView)
    val dividendReceivedConstraintLayout: ConstraintLayout = itemView.findViewById(R.id.dividendReceivedConstraintLayout)
    val dividendReceivedLinearLayout: LinearLayout = itemView.findViewById(R.id.dividendReceivedLinearLayout)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): DividendReceivedViewHolder {
    val itemView = inflater.inflate(R.layout.dividend_received_view_item, parent, false)
    return DividendReceivedViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: DividendReceivedViewHolder,
    position: Int
  ) {
    val current: Dividend = dividendList[position]

    // First entry is headline.
    if (position == 0) {
      holder.textViewDividendReceivedAmount.text = context.getString(R.string.dividend)
      holder.textViewDividendReceivedDate.text = context.getString(R.string.dividend_date)
      holder.textViewDividendReceivedDelete.visibility = View.GONE
      holder.dividendReceivedSummaryView.visibility = View.GONE
      holder.dividendReceivedConstraintLayout.setBackgroundColor(context.getColor(R.color.backgroundListColor))

      val background = TypedValue()
      holder.dividendReceivedLinearLayout.setBackgroundResource(background.resourceId)
    } else {
      // Last entry is summary.
      if (position == dividendList.size - 1) {
        // handler for delete all
        holder.bindDelete(current.symbol, null, clickListenerDelete)

        holder.textViewDividendReceivedAmount.text =           DecimalFormat("0.##").format(current.amount)
        holder.textViewDividendReceivedDate.text = ""

        // no delete icon for empty list, headline + summaryline = 2
        if (dividendList.size <= 2) {
          holder.textViewDividendReceivedDelete.visibility = View.GONE
        }
        else
        {
          holder.textViewDividendReceivedDelete.visibility = View.VISIBLE
        }

        holder.dividendReceivedSummaryView.visibility = View.VISIBLE
        holder.dividendReceivedConstraintLayout.setBackgroundColor(Color.YELLOW)

        val background = TypedValue()
        holder.dividendReceivedLinearLayout.setBackgroundResource(background.resourceId)
      } else {
        holder.bindUpdate(current, clickListenerUpdate)
        holder.bindDelete(null, current, clickListenerDelete)

        holder.textViewDividendReceivedAmount.text = DecimalFormat("0.##").format(current.amount)
        val datetime: LocalDateTime = LocalDateTime.ofEpochSecond(current.paydate, 0, ZoneOffset.UTC)
        holder.textViewDividendReceivedDate.text = datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))

        holder.textViewDividendReceivedDelete.visibility = View.VISIBLE
        holder.dividendReceivedSummaryView.visibility = View.GONE
        holder.dividendReceivedConstraintLayout.background = null

        val background = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        holder.dividendReceivedLinearLayout.setBackgroundResource(background.resourceId)
      }
    }
  }

  internal fun updateDividends(dividends: Dividends) {
    // Headline placeholder
    dividendList = mutableListOf(Dividend(symbol = "", amount = 0f, exdate = 0L, paydate = 0L, type = 0))
    dividendList.addAll(dividends.dividends)

    val dividendTotal = dividendList.sumByDouble {
      it.amount.toDouble()
    }
        .toFloat()

    // Summary
    val symbol: String = dividendList.firstOrNull()?.symbol ?: ""
    dividendList.add(Dividend(symbol = symbol, amount = dividendTotal, exdate = 0L, paydate = 0L, type = 0))

    notifyDataSetChanged()
  }

  override fun getItemCount() = dividendList.size
}