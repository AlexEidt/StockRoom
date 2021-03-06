package com.example.android.stockroom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class StockChartDataViewModel(application: Application) : AndroidViewModel(application) {

  private val stockChartDataRepository: StockChartDataRepository =
    StockChartDataRepository(StockApiFactory.yahooApi)

  val data: LiveData<List<StockDataEntry>>

  init {
    data = stockChartDataRepository.data
  }

  fun fetchYahooChartData(
    symbol: String,
    interval: String,
    range: String
  ) {
    viewModelScope.launch {
      stockChartDataRepository.fetchYahooChartData(symbol, interval, range)
    }
  }
}