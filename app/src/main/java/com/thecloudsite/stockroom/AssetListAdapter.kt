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
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getCapitalGainLossText
import com.thecloudsite.stockroom.utils.obsoleteAssetType
import kotlinx.android.synthetic.main.assetview_item.view.textViewAssetDelete
import kotlinx.android.synthetic.main.assetview_item.view.textViewAssetItemsLayout
import java.text.DecimalFormat
import kotlin.math.absoluteValue

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class AssetListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (Asset) -> Unit,
  private val clickListenerDelete: (String?, Asset?) -> Unit
) : RecyclerView.Adapter<AssetListAdapter.AssetViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var assetList = mutableListOf<Asset>()
  private var assetsCopy = listOf<Asset>()
  private var defaultTextColor: Int? = null

  class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindUpdate(
      asset: Asset,
      clickListenerUpdate: (Asset) -> Unit
    ) {
      itemView.textViewAssetItemsLayout.setOnClickListener { clickListenerUpdate(asset) }
    }

    fun bindDelete(
      symbol: String?,
      asset: Asset?,
      clickListenerDelete: (String?, Asset?) -> Unit
    ) {
      itemView.textViewAssetDelete.setOnClickListener { clickListenerDelete(symbol, asset) }
    }

    val itemViewShares: TextView = itemView.findViewById(R.id.textViewAssetShares)
    val itemViewPrice: TextView = itemView.findViewById(R.id.textViewAssetPrice)
    val itemViewTotal: TextView = itemView.findViewById(R.id.textViewAssetTotal)
    val itemViewDelete: TextView = itemView.findViewById(R.id.textViewAssetDelete)
    val assetSummaryView: LinearLayout = itemView.findViewById(R.id.assetSummaryView)
    val assetSummaryTextView: TextView = itemView.findViewById(R.id.assetSummaryTextView)
    val itemViewLayout: ConstraintLayout = itemView.findViewById(R.id.textViewAssetLayout)
    val textViewAssetItemsLayout: LinearLayout =
      itemView.findViewById(R.id.textViewAssetItemsLayout)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): AssetViewHolder {
    val itemView = inflater.inflate(R.layout.assetview_item, parent, false)
    return AssetViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: AssetViewHolder,
    position: Int
  ) {
    val current: Asset = assetList[position]

    if (defaultTextColor == null) {
      defaultTextColor = holder.itemViewShares.currentTextColor
    }

    // First entry is headline.
    if (position == 0) {
      holder.itemViewShares.text = context.getString(R.string.shares)
      holder.itemViewPrice.text = context.getString(R.string.price)
      holder.itemViewTotal.text = context.getString(R.string.value)
      holder.itemViewDelete.visibility = View.GONE
      holder.assetSummaryView.visibility = View.GONE
      holder.itemViewLayout.setBackgroundColor(context.getColor(R.color.backgroundListColor))

      val background = TypedValue()
      holder.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
    } else {
      // Last entry is summary.
      if (position == assetList.size - 1) {
        // handler for delete all
        holder.bindDelete(current.symbol, null, clickListenerDelete)

        val isSum = current.shares > 0.0 && current.price > 0.0

        holder.itemViewShares.text = if (isSum) {
          DecimalFormat("0.####").format(current.shares)
        } else {
          ""
        }

        holder.itemViewPrice.text = if (isSum) {
          DecimalFormat("0.00##").format(current.price / current.shares)
        } else {
          ""
        }
        holder.itemViewTotal.text = DecimalFormat("0.00").format(current.price)

        // no delete icon for empty list, headline + summaryline = 2
        if (assetList.size <= 2) {
          holder.itemViewDelete.visibility = View.GONE
        } else {
          holder.itemViewDelete.visibility = View.VISIBLE
        }

        holder.assetSummaryView.visibility = View.VISIBLE

        if (assetsCopy.isNotEmpty()) {
          var capitalGain = 0.0
          var capitalLoss = 0.0

          val capitalGainLoss = getAssetsCapitalGain(assetsCopy)
          when {
            capitalGainLoss == Double.NEGATIVE_INFINITY -> {
              capitalGain = 0.0
              capitalLoss = 0.0
            }
            capitalGainLoss > 0.0 -> {
              capitalGain += capitalGainLoss
            }
            else -> {
              capitalLoss += -capitalGainLoss
            }
          }

          val capitalGainLossText = getCapitalGainLossText(capitalGain, capitalLoss, context)
          holder.assetSummaryTextView.text = SpannableStringBuilder()
              .append("${context.getString(R.string.summary_capital_gain)} ")
              .append(capitalGainLossText)
              .append(context.getString(R.string.asset_summary_text))
        } else {
          holder.assetSummaryTextView.text = context.getString(R.string.asset_summary_text)
        }

        holder.itemViewLayout.setBackgroundColor(Color.YELLOW)

        val background = TypedValue()
        holder.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
      } else {
        // Asset items
        holder.bindUpdate(current, clickListenerUpdate)
        holder.bindDelete(null, current, clickListenerDelete)

        // Removed and obsolete entries are colored light gray.
        if (current.shares < 0.0 || (current.type and obsoleteAssetType != 0)) {
          holder.itemViewShares.setTextColor(Color.LTGRAY)
          holder.itemViewPrice.setTextColor(Color.LTGRAY)
          holder.itemViewTotal.setTextColor(Color.LTGRAY)
        } else {
          if (defaultTextColor != null) {
            holder.itemViewShares.setTextColor(defaultTextColor!!)
            holder.itemViewPrice.setTextColor(defaultTextColor!!)
            holder.itemViewTotal.setTextColor(defaultTextColor!!)
          }
        }

        holder.itemViewShares.text = DecimalFormat("0.####").format(current.shares)

        if (current.price != 0.0) {
          holder.itemViewPrice.text = DecimalFormat("0.00##").format(current.price)
          holder.itemViewTotal.text =
            DecimalFormat("0.00").format(current.shares.absoluteValue * current.price)
        } else {
          holder.itemViewPrice.text = ""
          holder.itemViewTotal.text = ""
        }

        holder.itemViewDelete.visibility = View.VISIBLE
        holder.assetSummaryView.visibility = View.GONE
        holder.itemViewLayout.background = null

        val background = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        holder.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
      }
    }
  }

  internal fun updateAssets(assets: List<Asset>) {
    assetsCopy = assets

    // Headline placeholder
    assetList = mutableListOf(
        Asset(
            symbol = "",
            shares = 0.0,
            price = 0.0
        )
    )

    // Sort assets in the list by date.
    assetList.addAll(assets.sortedBy { asset ->
      asset.date
    })

    val (totalShares, totalPrice) = getAssets(assetList, obsoleteAssetType)

//    val totalShares = assetList.sumByDouble {
//      it.shares
//    }
//
//    val totalPrice = assetList.sumByDouble {
//      it.shares * it.price
//    }

    // Summary
    val symbol: String = assets.firstOrNull()?.symbol ?: ""
    assetList.add(
        Asset(
            id = null,
            symbol = symbol,
            shares = totalShares,
            price = totalPrice
        )
    )

    notifyDataSetChanged()
  }

  override fun getItemCount() = assetList.size
}
