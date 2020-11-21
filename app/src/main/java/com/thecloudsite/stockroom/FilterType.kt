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
import com.thecloudsite.stockroom.utils.getAssets
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset

enum class FilterTypeEnum(val value: Int) {
  FilterNullType(0),
  FilterTestType(1),
  FilterTextType(2),
  FilterDoubleType(3),
  FilterLongTermType(4),
  FilterPercentageChangeGreaterThanType(5),
  FilterPercentageChangeLessThanType(6),
  FilterSymbolContainsType(7),
  FilterNoteContainsType(8),
  FilterAssetGreaterThanType(9),
  FilterAssetLessThanType(10),
  FilterProfitGreaterThanType(11),
  FilterProfitLessThanType(12),
  FilterProfitPercentageGreaterThanType(13),
  FilterProfitPercentageLessThanType(14),
  FilterDividendPercentageGreaterThanType(15),
  FilterDividendPercentageLessThanType(16),
}

enum class FilterDataTypeEnum(val value: Int) {
  NoType(0),
  TextType(1),
  DoubleType(2),
}

object FilterFactory {
  fun create(
    type: FilterTypeEnum,
    context: Context
  ): IFilterType =
    when (type) {
      FilterTypeEnum.FilterNullType -> FilterNullType()
      FilterTypeEnum.FilterTestType -> FilterTestType(type)
      FilterTypeEnum.FilterTextType -> FilterTextType(type)
      FilterTypeEnum.FilterDoubleType -> FilterDoubleType(type)
      FilterTypeEnum.FilterLongTermType -> FilterLongTermType(type, context)
      FilterTypeEnum.FilterPercentageChangeGreaterThanType -> FilterPercentageChangeGreaterThanType(
          type, context
      )
      FilterTypeEnum.FilterPercentageChangeLessThanType -> FilterPercentageChangeLessThanType(
          type, context
      )
      FilterTypeEnum.FilterSymbolContainsType -> FilterSymbolContainsType(type, context)
      FilterTypeEnum.FilterNoteContainsType -> FilterNoteContainsType(type, context)
      FilterTypeEnum.FilterAssetGreaterThanType -> FilterAssetGreaterThanType(type, context)
      FilterTypeEnum.FilterAssetLessThanType -> FilterAssetLessThanType(type, context)
      FilterTypeEnum.FilterProfitGreaterThanType -> FilterProfitGreaterThanType(type, context)
      FilterTypeEnum.FilterProfitLessThanType -> FilterProfitLessThanType(type, context)
      FilterTypeEnum.FilterProfitPercentageGreaterThanType -> FilterProfitPercentageGreaterThanType(
          type, context
      )
      FilterTypeEnum.FilterProfitPercentageLessThanType -> FilterProfitPercentageLessThanType(
          type, context
      )
      FilterTypeEnum.FilterDividendPercentageGreaterThanType -> FilterDividendPercentageGreaterThanType(
          type, context
      )
      FilterTypeEnum.FilterDividendPercentageLessThanType -> FilterDividendPercentageLessThanType(
          type, context
      )
    }

  fun create(
    id: String,
    context: Context
  ): IFilterType {
    FilterTypeEnum.values()
        .forEach { filter ->
          val filterType = create(filter, context)
          if (id == filterType.typeId.toString()) {
            return filterType
          }
        }
    return FilterNullType()
  }

  fun create(
    index: Int,
    context: Context
  ): IFilterType =
    // + 1, skip NullFilter
    if (index >= 0 && index + 1 < FilterTypeEnum.values().size) {
      val type: FilterTypeEnum = FilterTypeEnum.values()[index + 1]
      create(type, context)
    } else {
      FilterNullType()
    }
}

fun getFilterNameList(context: Context): List<String> {
  val filterList = mutableListOf<String>()

  FilterTypeEnum.values()
      .filter { type ->
        type != FilterTypeEnum.FilterNullType
      }
      .forEach { filter ->
        filterList.add(FilterFactory.create(filter, context).displayName)
      }

  return filterList
}

private fun strToDouble(str: String): Double {
  var value: Double = 0.0
  try {
    val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    value = numberFormat.parse(str)!!
        .toDouble()
  } catch (e: Exception) {
  }

  return value
}

interface IFilterType {
  fun filter(stockItem: StockItem): Boolean
  val typeId: FilterTypeEnum
  val dataType: FilterDataTypeEnum
  val displayName: String
  var data: String
  var desc: String
}

class FilterNullType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return true
  }

  override val typeId = FilterTypeEnum.FilterNullType
  override val dataType = FilterDataTypeEnum.NoType
  override val displayName = typeId.toString()
  override var data = ""
  override var desc = ""
}

class FilterTestType(override val typeId: FilterTypeEnum) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.isNotEmpty()
  }

  override val dataType = FilterDataTypeEnum.NoType
  override val displayName = typeId.toString()
  override var data = ""
  override var desc = ""
}

class FilterTextType(override val typeId: FilterTypeEnum) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.isNotEmpty()
  }

  override val dataType = FilterDataTypeEnum.TextType
  override val displayName = typeId.toString()
  override var data = ""
  override var desc = ""
}

class FilterDoubleType(override val typeId: FilterTypeEnum) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.startsWith("A")
  }

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = typeId.toString()
  override var data: String = ""
    get() = field
    set(value) {
      field = value
    }
  override var desc = ""
}

// Stocks are at least one year old.
class FilterLongTermType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val secondsNow = LocalDateTime.now()
        .toEpochSecond(ZoneOffset.UTC)
    val newestAssetDate = stockItem.assets.maxOf { asset ->
      asset.date
    }

    // 365 plus one day
    val secondsPerYear: Long = 366 * 24 * 60 * 60
    return secondsNow > newestAssetDate + secondsPerYear
  }

  override val dataType = FilterDataTypeEnum.NoType
  override var displayName = context.getString(R.string.filter_longterm_name)
  override var data = ""
  override var desc = context.getString(R.string.filter_longterm_desc)
}

// Change percentage greater than
class FilterPercentageChangeGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.onlineMarketData.marketChangePercent > filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_percentagechangegreater_name)
  override var desc = context.getString(R.string.filter_percentagechangegreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
}

// Change percentage less than
class FilterPercentageChangeLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.onlineMarketData.marketChangePercent < filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_percentagechangeless_name)
  override var desc = context.getString(R.string.filter_percentagechangeless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
}

class FilterSymbolContainsType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.contains(data, ignoreCase = true)
  }

  override val dataType = FilterDataTypeEnum.TextType
  override var displayName = context.getString(R.string.filter_symbolcontainstype_name)
  override var desc = context.getString(R.string.filter_symbolcontainstype_desc)
  override var data = ""
}

class FilterNoteContainsType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.note.contains(data, ignoreCase = true)
  }

  override val dataType = FilterDataTypeEnum.TextType
  override var displayName = context.getString(R.string.filter_notecontainstype_name)
  override var desc = context.getString(R.string.filter_notecontainstype_desc)
  override var data = ""
}

// Asset greater than
class FilterAssetGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val asset = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice
    } else {
      totalPrice
    }
    return asset > filterValue
  }

  var filterValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_assetgreater_name)
  override var desc = context.getString(R.string.filter_assetgreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.00").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
}

// Asset less than
class FilterAssetLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val asset = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice
    } else {
      totalPrice
    }
    return asset < filterValue
  }

  var filterValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_assetless_name)
  override var desc = context.getString(R.string.filter_assetless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.00").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
}

// Profit greater than
class FilterProfitGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profit = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice
    } else {
      totalPrice
    }
    return profit > filterValue
  }

  var filterValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_profitgreater_name)
  override var desc = context.getString(R.string.filter_profitgreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.00").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
}

// Profit less than
class FilterProfitLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profit = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice
    } else {
      totalPrice
    }
    return profit < filterValue
  }

  var filterValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_profitless_name)
  override var desc = context.getString(R.string.filter_profitless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.00").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
}

// Profit Percentage greater than
class FilterProfitPercentageGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profitPercentage =
      if (stockItem.onlineMarketData.marketPrice > 0.0 && totalPrice > 0.0) {
        (totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice) / totalPrice
      } else {
        totalPrice
      }
    return profitPercentage > filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_profitpercentagegreater_name)
  override var desc = context.getString(R.string.filter_profitpercentagegreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
}

// Profit Percentage less than
class FilterProfitPercentageLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profitPercentage =
      if (stockItem.onlineMarketData.marketPrice > 0.0 && totalPrice > 0.0) {
        (totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice) / totalPrice
      } else {
        totalPrice
      }
    return profitPercentage < filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_profitpercentageless_name)
  override var desc = context.getString(R.string.filter_profitpercentageless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
}

// Dividend Percentage greater than
class FilterDividendPercentageGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val dividendPercentage =
      if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
        if (stockItem.onlineMarketData.marketPrice > 0.0) {
          stockItem.stockDBdata.annualDividendRate / stockItem.onlineMarketData.marketPrice
        } else {
          0.0
        }
      } else {
        stockItem.onlineMarketData.annualDividendYield
      }
    return dividendPercentage > filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_dividendpercentagegreater_name)
  override var desc = context.getString(R.string.filter_dividendpercentagegreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
}

// Dividend Percentage less than
class FilterDividendPercentageLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val dividendPercentage =
      if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
        if (stockItem.onlineMarketData.marketPrice > 0.0) {
          stockItem.stockDBdata.annualDividendRate / stockItem.onlineMarketData.marketPrice
        } else {
          0.0
        }
      } else {
        stockItem.onlineMarketData.annualDividendYield
      }
    return dividendPercentage < filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_dividendpercentageless_name)
  override var desc = context.getString(R.string.filter_dividendpercentageless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
}
