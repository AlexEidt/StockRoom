package com.example.android.stockroom

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

data class StockItem
(
  var onlineMarketData: OnlineMarketData,
  var stockDBdata: StockDBdata,
  var assets: List<Asset>,
  var events: List<Event>
)

class StockMarketDataRepository(private val api: YahooApi) : BaseRepository() {

  private val _data = MutableLiveData<List<OnlineMarketData>>()
  val onlineMarketDataList: LiveData<List<OnlineMarketData>>
    get() = _data

  suspend fun getStockData(symbols: List<String>): MarketState {
    if (symbols.isNotEmpty()) {
      Log.d("Handlers", "stockMarketDataRepository.getStockData()")

      val quoteResponse: YahooResponse? = try {
        safeApiCall(
            call = {
              api.getStockDataAsync(symbols.joinToString(","))
                  .await()
            }, errorMessage = "Error fetching finance data."
        )
      } catch (e: Exception) {
        Log.d("StockMarketDataRepository.getStockData failed", "Exception=${e}")
        null
      }

      // no _data.value because this is a background thread
      val onlineMarketDataResultList: List<OnlineMarketData> = quoteResponse?.quoteResponse?.result
          ?: return MarketState.NO_NETWORK

      val postMarket: Boolean = SharedRepository.postMarket

      if (postMarket) {
        // Transform onlinedata, replace market value with postmarket value
        val onlineMarketDataResultList2 = onlineMarketDataResultList.map { onlineMarketData ->
          val onlineMarketData2: OnlineMarketData = onlineMarketData

          if ((onlineMarketData.marketState == MarketState.POST.value
                  || onlineMarketData.marketState == MarketState.POSTPOST.value
                  || onlineMarketData.marketState == MarketState.PREPRE.value
                  || onlineMarketData.marketState == MarketState.CLOSED.value)
              && onlineMarketData.postMarketPrice > 0f
          ) {
            onlineMarketData2.marketPrice =
              onlineMarketData.postMarketPrice
            onlineMarketData2.marketChange =
              onlineMarketData.postMarketChange
            onlineMarketData2.marketChangePercent =
              onlineMarketData.postMarketChangePercent
          } else
            if ((onlineMarketData.marketState == MarketState.PRE.value)
                && onlineMarketData.preMarketPrice > 0f
            ) {
              onlineMarketData2.marketPrice =
                onlineMarketData.preMarketPrice
              onlineMarketData2.marketChange =
                onlineMarketData.preMarketChange
              onlineMarketData2.marketChangePercent =
                onlineMarketData.preMarketChangePercent
            }

          onlineMarketData2
        }

        _data.postValue(onlineMarketDataResultList2)
      } else {
        _data.postValue(onlineMarketDataResultList)
      }

      // Stocks could be from different markets.
      // Check if stocks are from markets that have regular hours first, then post market,
      // and then pre market hours.
      val marketState: MarketState = when {
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.REGULAR.value
        } != null -> {
          MarketState.REGULAR
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.POST.value
        } != null -> {
          MarketState.POST
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.POSTPOST.value
        } != null -> {
          MarketState.POSTPOST
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.CLOSED.value
        } != null -> {
          MarketState.CLOSED
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.PRE.value
        } != null -> {
          MarketState.PRE
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.PREPRE.value
        } != null -> {
          MarketState.PREPRE
        }
        else -> {
          MarketState.UNKNOWN
        }
      }

      return marketState
    }

    return MarketState.UNKNOWN
  }

  suspend fun getStockData(symbol: String): OnlineMarketData? {
    if (symbol.isNotEmpty()) {

      val quoteResponse = safeApiCall(
          call = {
            api.getStockDataAsync(symbol)
                .await()
          },
          errorMessage = "Error fetching finance data."
      )

      return quoteResponse?.quoteResponse?.result?.firstOrNull()
    }

    return OnlineMarketData(symbol = symbol)
  }
}
