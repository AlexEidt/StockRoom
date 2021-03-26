/*
 * Copyright (C) 2021
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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.databinding.TimelineGainlossItemBinding

data class GainLossTimelineElement(
  val date: String,
  val totalGainLoss: SpannableStringBuilder,
  val stockGainLossList: List<SpannableStringBuilder>,
)

class GainLossTimelineAdapter(
  private val context: Context
) : RecyclerView.Adapter<GainLossTimelineAdapter.ViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var timelineElementList: List<GainLossTimelineElement> = listOf()

  class ViewHolder(
    val binding: TimelineGainlossItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {

    val binding = TimelineGainlossItemBinding.inflate(inflater, parent, false)
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val timelineElement = timelineElementList[position]

    holder.binding.timelineHeader.text = timelineElement.totalGainLoss

    val gainlossStr = SpannableStringBuilder()

    timelineElement.stockGainLossList.forEach { gainloss ->
      gainlossStr.append(gainloss)
    }

    holder.binding.timelineDetails.text = gainlossStr
  }

  fun updateData(timeline: List<GainLossTimelineElement>) {
    timelineElementList = timeline
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = timelineElementList.size
}