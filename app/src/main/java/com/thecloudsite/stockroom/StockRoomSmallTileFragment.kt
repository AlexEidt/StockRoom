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

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class StockRoomSmallTileFragment : StockRoomBaseFragment() {

  companion object {
    fun newInstance() = StockRoomSmallTileFragment()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val clickListenerSummary = { stockItem: StockItem -> clickListenerSummary(stockItem) }
    val adapter = StockRoomSmallTileAdapter(requireContext(), clickListenerSummary)

    val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview)

    recyclerView.adapter = adapter

    // Set column number depending on screen width.
    val scale = 247
    val spanCount =
      (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

    recyclerView.layoutManager = GridLayoutManager(context,
        Integer.min(Integer.max(spanCount, 1), 10)
    )

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let {
        adapter.setStockItems(it)
      }
    })
  }
}