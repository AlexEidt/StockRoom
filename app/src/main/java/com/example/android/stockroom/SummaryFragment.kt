package com.example.android.stockroom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.android.synthetic.main.fragment_summary.view.summaryPieChart

class SummaryFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = SummaryFragment()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_summary, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("Summary activity started.")

    val groupList: List<Group> = stockRoomViewModel.getGroupsSync()
    val summaryListAdapter = SummaryListAdapter(requireContext(), groupList)

    val summaryList = view.findViewById<RecyclerView>(R.id.summaryList)
    summaryList.adapter = summaryListAdapter
    summaryList.layoutManager = LinearLayoutManager(requireContext())

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItems ->
        summaryListAdapter.updateData(stockItems)
        updatePieData(view, stockItems)
      }
    })
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.updateOnlineDataManually()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sync -> {
        stockRoomViewModel.updateOnlineDataManually()
        stockRoomViewModel.logDebug("Update online data manually for summary data.")
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun updatePieData(
    view: View,
    stockItems: List<StockItem>
  ) {
    val listPie = ArrayList<PieEntry>()
    val listColors = ArrayList<Int>()

    data class AssetSummary(
      val symbol: String,
      val assets: Float,
      val color: Int
    )

    val assetList: MutableList<AssetSummary> = mutableListOf()
    var assetsTotal = 0f
    stockItems.forEach { stockItem ->
      val shares: Float = stockItem.assets.sumByDouble { asset ->
        asset.shares.toDouble()
      }
          .toFloat()
      val assets = shares * stockItem.onlineMarketData.marketPrice
      assetsTotal += assets
      val color = if (stockItem.stockDBdata.groupColor != 0) {
        stockItem.stockDBdata.groupColor
      } else {
        context?.getColor(R.color.backgroundListColor)
      }
      assetList.add(
          AssetSummary(stockItem.stockDBdata.symbol, assets, color!!)
      )
    }

    if (assetsTotal > 0f) {
      assetList.sortedBy { item -> item.assets }
          .takeLast(10)
          .forEach { assetItem ->
            listPie.add(PieEntry(assetItem.assets, assetItem.symbol))
            listColors.add(assetItem.color)
          }
    }

    val pieDataSet = PieDataSet(listPie, "")
    pieDataSet.colors = listColors

    val pieData = PieData(pieDataSet)
    //pieData.setValueTextSize(CommonUtils.convertDpToSp(14))
    view.summaryPieChart.data = pieData

    //view.summaryPieChart.setUsePercentValues(true)
    view.summaryPieChart.isDrawHoleEnabled = false
    view.summaryPieChart.description.isEnabled = false
    view.summaryPieChart.setEntryLabelColor(R.color.design_default_color_background)
    view.summaryPieChart.invalidate()
    //view.summaryPieChart.animateY(1400, Easing.EaseInOutQuad)
  }
}
