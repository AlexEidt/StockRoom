package com.example.android.stockroom

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.DatePicker
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.android.synthetic.main.activity_stock.addAssetsButton
import kotlinx.android.synthetic.main.activity_stock.addEventsButton
import kotlinx.android.synthetic.main.activity_stock.alertAboveInputEditText
import kotlinx.android.synthetic.main.activity_stock.alertAboveInputLayout
import kotlinx.android.synthetic.main.activity_stock.alertBelowInputEditText
import kotlinx.android.synthetic.main.activity_stock.alertBelowInputLayout
import kotlinx.android.synthetic.main.activity_stock.assetsView
import kotlinx.android.synthetic.main.activity_stock.buttonFiveDays
import kotlinx.android.synthetic.main.activity_stock.buttonFiveYears
import kotlinx.android.synthetic.main.activity_stock.buttonMax
import kotlinx.android.synthetic.main.activity_stock.buttonOneDay
import kotlinx.android.synthetic.main.activity_stock.buttonOneMonth
import kotlinx.android.synthetic.main.activity_stock.buttonOneYear
import kotlinx.android.synthetic.main.activity_stock.buttonThreeMonth
import kotlinx.android.synthetic.main.activity_stock.buttonYTD
import kotlinx.android.synthetic.main.activity_stock.candleStickChart
import kotlinx.android.synthetic.main.activity_stock.eventsView
import kotlinx.android.synthetic.main.activity_stock.imageButtonIconCandle
import kotlinx.android.synthetic.main.activity_stock.imageButtonIconLine
import kotlinx.android.synthetic.main.activity_stock.lineChart
import kotlinx.android.synthetic.main.activity_stock.linearLayoutGroup
import kotlinx.android.synthetic.main.activity_stock.notesTextView
import kotlinx.android.synthetic.main.activity_stock.onlineDataView
import kotlinx.android.synthetic.main.activity_stock.removeAssetButton
import kotlinx.android.synthetic.main.activity_stock.textViewChange
import kotlinx.android.synthetic.main.activity_stock.textViewGroup
import kotlinx.android.synthetic.main.activity_stock.textViewGroupColor
import kotlinx.android.synthetic.main.activity_stock.textViewMarketPrice
import kotlinx.android.synthetic.main.activity_stock.textViewName
import kotlinx.android.synthetic.main.activity_stock.textViewRange
import kotlinx.android.synthetic.main.activity_stock.textViewSymbol
import kotlinx.android.synthetic.main.activity_stock.updateNotesButton
import okhttp3.internal.toHexString
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.FormatStyle.LONG
import java.time.format.FormatStyle.SHORT
import java.util.Locale

enum class StockViewRange(val value: Int) {
  OneDay(0),
  FiveDays(1),
  OneMonth(2),
  ThreeMonth(3),
  YTD(4),
  Max(5),
  OneYear(6),
  FiveYears(7),
}

enum class StockViewMode(val value: Int) {
  Line(0),
  Candle(1),
}

class StockDataActivity : AppCompatActivity() {
  private lateinit var viewModel: StockChartDataViewModel
  private lateinit var stockRoomViewModel: StockRoomViewModel

  private lateinit var stockDBdata: StockDBdata
  private var stockDataEntries: List<StockDataEntry>? = null
  private var symbol: String = ""

  private var alertAbove: Float = 0f
  private var alertBelow: Float = 0f

  // Settings.
  private val settingStockViewRange = "SettingStockViewRange"
  private var stockViewRange: StockViewRange
    get() {
      val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return StockViewRange.OneDay
      return StockViewRange.values()[sharedPref.getInt(
          settingStockViewRange, StockViewRange.OneDay.value
      )]
    }
    set(value) {
      val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
      with(sharedPref.edit()) {
        putInt(settingStockViewRange, value.value)
        commit()
      }
    }

  private val settingStockViewMode = "SettingStockViewMode"
  private var stockViewMode: StockViewMode
    get() {
      val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return StockViewMode.Line
      return StockViewMode.values()[sharedPref.getInt(
          settingStockViewMode, StockViewMode.Line.value
      )]
    }
    set(value) {
      val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
      with(sharedPref.edit()) {
        putInt(settingStockViewMode, value.value)
        commit()
      }
    }

  private fun assetItemUpdateClicked(asset: Asset) {
    val builder = AlertDialog.Builder(this)
    // Get the layout inflater
    val inflater = LayoutInflater.from(this)

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.add_asset, null)

    val addUpdateSharesHeadlineView =
      dialogView.findViewById<TextView>(R.id.addUpdateSharesHeadline)
    addUpdateSharesHeadlineView.text = getString(R.string.update_asset)
    val addSharesView = dialogView.findViewById<TextView>(R.id.addShares)
    addSharesView.text = DecimalFormat("0.######").format(asset.shares)
    val addPriceView = dialogView.findViewById<TextView>(R.id.addPrice)
    addPriceView.text = DecimalFormat("0.######").format(asset.price)
    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.add
        ) { _, _ ->
          val sharesText = addSharesView.text.toString()
          val priceText = addPriceView.text.toString()
          if (priceText.isNotEmpty() && sharesText.isNotEmpty()) {
            var price = 0f
            var shares = 0f
            var valid = true
            try {
              val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
              price = numberFormat.parse(priceText)!!
                  .toFloat()
            } catch (e: Exception) {
              valid = false
            }
            if (price <= 0f) {
              Toast.makeText(this, getString(R.string.price_not_zero), Toast.LENGTH_LONG)
                  .show()
              valid = false
            }
            try {
              val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
              shares = numberFormat.parse(sharesText)!!
                  .toFloat()
            } catch (e: Exception) {
              valid = false
            }
            if (shares <= 0f) {
              Toast.makeText(this, getString(R.string.shares_not_zero), Toast.LENGTH_LONG)
                  .show()
              valid = false
            }
            if (valid) {
              val assetnew = Asset(symbol = symbol, shares = shares, price = price)
              if (asset.shares != assetnew.shares || asset.price != assetnew.price) {
                // delete old asset
                stockRoomViewModel.deleteAsset(asset)
                // add new asset
                stockRoomViewModel.addAsset(assetnew)
                val count: Int = when {
                  shares == 1f -> {
                    1
                  }
                  shares > 1f -> {
                    shares.toInt() + 1
                  }
                  else -> {
                    0
                  }
                }

                val pluralstr = resources.getQuantityString(
                    R.plurals.asset_updated, count, DecimalFormat("0.##").format(shares),
                    DecimalFormat("0.##").format(price)
                )

                Toast.makeText(
                    this, pluralstr, Toast.LENGTH_LONG
                )
                    .show()
              }
            }
            hideSoftInputFromWindow()
          } else {
            Toast.makeText(this, getString(R.string.invalid_entry), Toast.LENGTH_LONG)
                .show()
          }
        }
        .setNegativeButton(R.string.cancel,
            DialogInterface.OnClickListener { _, _ ->
              //getDialog().cancel()
            })
    builder
        .create()
        .show()
  }

  private fun assetItemDeleteClicked(
    symbol: String?,
    asset: Asset?
  ) {
    // Summary tag?
    if (symbol != null && asset == null) {
      android.app.AlertDialog.Builder(this)
          .setTitle(R.string.delete_all_assets)
          .setMessage(getString(R.string.delete_all_assets_confirm, symbol))
          .setPositiveButton(R.string.delete) { _, _ ->
            stockRoomViewModel.deleteAssets(symbol)
            Toast.makeText(this, getString(R.string.delete_all_assets_msg), Toast.LENGTH_LONG)
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    } else if (asset != null) {
      val count: Int = when {
        asset.shares == 1f -> {
          1
        }
        asset.shares > 1f -> {
          asset.shares.toInt() + 1
        }
        else -> {
          0
        }
      }

      android.app.AlertDialog.Builder(this)
          .setTitle(R.string.delete_asset)
          .setMessage(
              resources.getQuantityString(
                  R.plurals.delete_asset_confirm, count, DecimalFormat("0.##").format(asset.shares),
                  DecimalFormat("0.##").format(asset.price)
              )
          )
          .setPositiveButton(R.string.delete) { _, _ ->
            stockRoomViewModel.deleteAsset(asset)
            val pluralstr = resources.getQuantityString(
                R.plurals.delete_asset_msg, count, DecimalFormat("0.##").format(asset.shares),
                DecimalFormat("0.##").format(asset.price)
            )

            Toast.makeText(
                this, pluralstr, Toast.LENGTH_LONG
            )
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    }
  }

  private fun eventItemUpdateClicked(event: Event) {
    val builder = AlertDialog.Builder(this)
    // Get the layout inflater
    val inflater = LayoutInflater.from(this)

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.add_event, null)

    val eventHeadlineView = dialogView.findViewById<TextView>(R.id.eventHeadline)
    eventHeadlineView.text = getString(R.string.update_event)
    val textInputEditEventTitleView =
      dialogView.findViewById<TextView>(R.id.textInputEditEventTitle)
    textInputEditEventTitleView.text = event.title
    val textInputEditEventNoteView = dialogView.findViewById<TextView>(R.id.textInputEditEventNote)
    textInputEditEventNoteView.text = event.note
    val localDateTime = LocalDateTime.ofEpochSecond(event.datetime, 0, ZoneOffset.UTC)
    val datePickerEventDateView = dialogView.findViewById<DatePicker>(R.id.datePickerEventDate)
    // month is starting from zero
    datePickerEventDateView.updateDate(
        localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
    )
    val datePickerEventTimeView = dialogView.findViewById<TimePicker>(R.id.datePickerEventTime)
    datePickerEventTimeView.hour = localDateTime.hour
    datePickerEventTimeView.minute = localDateTime.minute

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.add
        ) { _, _ ->
          val title = textInputEditEventTitleView.text.toString()
          val note = textInputEditEventNoteView.text.toString()

          val datetime: LocalDateTime = LocalDateTime.of(
              datePickerEventDateView.year, datePickerEventDateView.month + 1,
              datePickerEventDateView.dayOfMonth, datePickerEventTimeView.hour,
              datePickerEventTimeView.minute
          )
          val seconds = datetime.toEpochSecond(ZoneOffset.UTC)
          if (event.title != title || event.note != note || event.datetime != seconds) {
            // delete old event
            stockRoomViewModel.deleteEvent(event)
            // add new event
            stockRoomViewModel.addEvent(
                Event(symbol = symbol, type = 0, title = title, note = note, datetime = seconds)
            )
            Toast.makeText(
                this, getString(
                R.string.event_updated, title, datetime.format(
                DateTimeFormatter.ofLocalizedDateTime(
                    FormatStyle.MEDIUM
                )
            )
            ), Toast.LENGTH_LONG
            )
                .show()
          }
          hideSoftInputFromWindow()
/*
*/
        }
        .setNegativeButton(R.string.cancel,
            DialogInterface.OnClickListener { _, _ ->
            })
    builder
        .create()
        .show()
  }

  private fun eventItemDeleteClicked(event: Event) {
    val localDateTime = LocalDateTime.ofEpochSecond(event.datetime, 0, ZoneOffset.UTC)
    val datetime = localDateTime.format(
        DateTimeFormatter.ofLocalizedDateTime(
            FormatStyle.MEDIUM
        )
    )
    android.app.AlertDialog.Builder(this)
        .setTitle(R.string.delete_event)
        .setMessage(
            getString(
                R.string.delete_event_confirm, event.title, datetime
            )
        )
        .setPositiveButton(R.string.delete) { _, _ ->
          stockRoomViewModel.deleteEvent(event)
          Toast.makeText(
              this, getString(
              R.string.delete_event_msg, event.title, datetime
          ), Toast.LENGTH_LONG
          )
              .show()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_stock)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    symbol = intent.getStringExtra("symbol")
        .toUpperCase(Locale.ROOT)
    textViewSymbol.text = symbol

    val onlineDataAdapter = OnlineDataAdapter(this)
    onlineDataView.adapter = onlineDataAdapter
    onlineDataView.layoutManager = GridLayoutManager(this, 2)

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("Stock data activity for '$symbol' started.")

    stockRoomViewModel.onlineMarketDataList.observe(this, Observer { data ->
      data?.let { onlineMarketDataList ->
        val onlineMarketData = onlineMarketDataList.find { onlineMarketDataItem ->
          onlineMarketDataItem.symbol == symbol
        }
        if (onlineMarketData != null) {
          updateHeader(onlineMarketData)
          onlineDataAdapter.updateData(onlineMarketData)

          /*
          val dividendText =
            if (onlineMarketData.annualDividendRate > 0f && onlineMarketData.annualDividendYield > 0f) {
              "${DecimalFormat("0.00##").format(
                  onlineMarketData.annualDividendRate
              )} (${DecimalFormat("0.00##").format(
                  onlineMarketData.annualDividendYield * 100
              )}%)"
            } else {
              ""
            }

          textViewDividend.text = dividendText
        */
        }
      }
    })

    viewModel = ViewModelProvider(this).get(StockChartDataViewModel::class.java)

    viewModel.data.observe(this, Observer { data ->
      stockDataEntries = data
      setupCharts(stockViewRange, stockViewMode)
      loadCharts(symbol, stockViewRange, stockViewMode)
    })

    updateStockViewRange(stockViewRange)

    // Assets
    val assetClickListenerUpdate =
      { asset: Asset -> assetItemUpdateClicked(asset) }
    val assetClickListenerDelete =
      { symbol: String?, asset: Asset? -> assetItemDeleteClicked(symbol, asset) }
    val assetAdapter =
      AssetListAdapter(this, assetClickListenerUpdate, assetClickListenerDelete)
    assetsView.adapter = assetAdapter
    assetsView.layoutManager = LinearLayoutManager(this)

    // Update the current asset list.
    val assetsLiveData: LiveData<Assets> = stockRoomViewModel.getAssetsLiveData(symbol)
    assetsLiveData.observe(this, Observer { data ->
      if (data != null) {
        assetAdapter.updateAssets(data.assets)
      }
    })

    // Events
    val eventClickListenerUpdate =
      { event: Event -> eventItemUpdateClicked(event) }
    val eventClickListenerDelete =
      { event: Event -> eventItemDeleteClicked(event) }
    val eventAdapter =
      EventListAdapter(this, eventClickListenerUpdate, eventClickListenerDelete)
    eventsView.adapter = eventAdapter
    eventsView.layoutManager = LinearLayoutManager(this)

    // Update the current event list.
    val eventsLiveData: LiveData<Events> = stockRoomViewModel.getEventsLiveData(symbol)
    eventsLiveData.observe(this, Observer { data ->
      if (data != null) {
        eventAdapter.updateEvents(data.events)
      }
    })

    stockDBdata = stockRoomViewModel.getStockDBdataSync(symbol)
    val notes = stockDBdata.notes

    // color = 0 is not stored in the DB
    var color = stockDBdata.groupColor
    if (color == 0) {
      color = application.getColor(R.color.backgroundListColor)
    }
    setBackgroundColor(textViewGroupColor, color)

    textViewGroup.text = if (stockDBdata.groupColor == 0) {
      getString(R.string.standard_group)
    } else {
      val group = stockRoomViewModel.getGroupSync(stockDBdata.groupColor)
      if (group.name.isEmpty()) {
        "Color code=0x${color.toHexString()}"
      } else {
        group.name
      }
    }

    linearLayoutGroup.setOnClickListener { view ->
      val popupMenu = PopupMenu(this, view)

      var menuIndex: Int = Menu.FIRST
      stockRoomViewModel.getGroupsMenuList(getString(R.string.standard_group))
          .forEach {
            popupMenu.menu.add(0, menuIndex++, Menu.NONE, it)
          }

      popupMenu.show()

      val groups: List<Group> = stockRoomViewModel.getGroupsSync()
      popupMenu.setOnMenuItemClickListener { menuitem ->
        val i: Int = menuitem.itemId - 1
        val clr: Int
        val clrDB: Int
        val name: String

        if (i >= groups.size) {
          clr = application.getColor(R.color.backgroundListColor)
          clrDB = 0
          name = getString(R.string.standard_group)
        } else {
          clr = groups[i].color
          clrDB = clr
          name = groups[i].name
        }

        // Set the preview color in the activity.
        setBackgroundColor(textViewGroupColor, clr)
        textViewGroup.text = name

        // Store the selection.
        stockRoomViewModel.setGroup(symbol, name, clrDB)
        true
      }
    }

    alertAbove = stockDBdata.alertAbove
    alertBelow = stockDBdata.alertBelow

    if (alertAbove > 0f && alertBelow > 0f && alertBelow >= alertAbove) {
      alertAbove = 0f
      alertBelow = 0f
    }

    alertAboveInputEditText.setText(
        if (alertAbove > 0f) {
          DecimalFormat("0.####").format(alertAbove)
        } else {
          ""
        }
    )
    alertBelowInputEditText.setText(
        if (alertBelow > 0f) {
          DecimalFormat("0.####").format(alertBelow)
        } else {
          ""
        }
    )

    notesTextView.text = stockDBdata.notes

    buttonOneDay.setOnClickListener {
      updateStockViewRange(StockViewRange.OneDay)
    }
    buttonFiveDays.setOnClickListener {
      updateStockViewRange(StockViewRange.FiveDays)
    }
    buttonOneMonth.setOnClickListener {
      updateStockViewRange(StockViewRange.OneMonth)
    }
    buttonThreeMonth.setOnClickListener {
      updateStockViewRange(StockViewRange.ThreeMonth)
    }
    buttonYTD.setOnClickListener {
      updateStockViewRange(StockViewRange.YTD)
    }
    buttonOneYear.setOnClickListener {
      updateStockViewRange(StockViewRange.OneYear)
    }
    buttonFiveYears.setOnClickListener {
      updateStockViewRange(StockViewRange.FiveYears)
    }
    buttonMax.setOnClickListener {
      updateStockViewRange(StockViewRange.Max)
    }

    imageButtonIconLine.setOnClickListener {
      updateStockViewMode(StockViewMode.Candle)
    }
    imageButtonIconCandle.setOnClickListener {
      updateStockViewMode(StockViewMode.Line)
    }

    addAssetsButton.setOnClickListener {
      val builder = AlertDialog.Builder(this)
      // Get the layout inflater
      val inflater = LayoutInflater.from(this)

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(R.layout.add_asset, null)
      val addUpdateSharesHeadlineView =
        dialogView.findViewById<TextView>(R.id.addUpdateSharesHeadline)
      addUpdateSharesHeadlineView.text = getString(R.string.add_asset)
      val addSharesView = dialogView.findViewById<TextView>(R.id.addShares)
      val addPriceView = dialogView.findViewById<TextView>(R.id.addPrice)
      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.add
          ) { _, _ ->
            val sharesText = addSharesView.text.toString()
            val priceText = addPriceView.text.toString()
            if (priceText.isNotEmpty() && sharesText.isNotEmpty()) {
              var price = 0f
              var shares = 0f
              var valid = true
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                price = numberFormat.parse(priceText)!!
                    .toFloat()
              } catch (e: Exception) {
                valid = false
              }
              if (price <= 0f) {
                Toast.makeText(this, getString(R.string.price_not_zero), Toast.LENGTH_LONG)
                    .show()
                valid = false
              }
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                shares = numberFormat.parse(sharesText)!!
                    .toFloat()
              } catch (e: Exception) {
                valid = false
              }
              if (shares <= 0f) {
                Toast.makeText(this, getString(R.string.shares_not_zero), Toast.LENGTH_LONG)
                    .show()
                valid = false
              }
              if (valid) {
                stockRoomViewModel.addAsset(Asset(symbol = symbol, shares = shares, price = price))
                val count: Int = when {
                  shares == 1f -> {
                    1
                  }
                  shares > 1f -> {
                    shares.toInt() + 1
                  }
                  else -> {
                    0
                  }
                }

                val pluralstr = resources.getQuantityString(
                    R.plurals.asset_added, count, DecimalFormat("0.##").format(shares),
                    DecimalFormat("0.##").format(price)
                )

                Toast.makeText(this, pluralstr, Toast.LENGTH_LONG)
                    .show()
              }
              hideSoftInputFromWindow()
            } else {
              Toast.makeText(this, getString(R.string.invalid_entry), Toast.LENGTH_LONG)
                  .show()
            }
          }
          .setNegativeButton(R.string.cancel,
              DialogInterface.OnClickListener { _, _ ->
              })
      builder
          .create()
          .show()
    }

    removeAssetButton.setOnClickListener {
      val builder = AlertDialog.Builder(this)
      // Get the layout inflater
      val inflater = LayoutInflater.from(this)

      val dialogView = inflater.inflate(R.layout.remove_asset, null)
      val removeSharesView = dialogView.findViewById<TextView>(R.id.removeShares)

      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.remove
          ) { _, _ ->
            val removeSharesText = removeSharesView.text.toString()
            if (removeSharesText.isNotEmpty()) {
              var shares = 0f
              var valid = true
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                shares = numberFormat.parse(removeSharesText)!!
                    .toFloat()
              } catch (e: Exception) {
                valid = false
              }

              if (valid) {
                val assetsAdjusted = stockRoomViewModel.getAssetsSync(symbol)
                if (assetsAdjusted != null) {
                  // Avoid wrong data due to rounding errors.
                  val epsilon = 0.000001f
                  val totalShares = assetsAdjusted.assets.sumByDouble { it.shares.toDouble() }
                      .toFloat()

                  if (totalShares > 0f) {
                    val totalPaidPrice = assetsAdjusted.assets.sumByDouble {
                      it.shares.toDouble() * it.price
                          .toDouble()
                    }
                        .toFloat()
                    val averagePrice = totalPaidPrice / totalShares

                    if (shares > (totalShares + epsilon)) {
                      Toast.makeText(
                          this, getString(R.string.remove_shares_exceeds_total_shares),
                          Toast.LENGTH_LONG
                      )
                          .show()
                    } else {
                      //assetSummary.removeAllViews()
                      val newTotal: Float = totalPaidPrice - shares * averagePrice
                      val shareAdjustment: Float = newTotal / totalPaidPrice

                      assetsAdjusted.assets.forEach { asset ->
                        asset.shares *= shareAdjustment
                      }
                      stockRoomViewModel.updateAssets(symbol = symbol, assets = assetsAdjusted.assets)
                    }
                  } else {
                    Toast.makeText(this, getString(R.string.no_total_shares), Toast.LENGTH_LONG)
                        .show()
                  }
                }
              }
              hideSoftInputFromWindow()
            } else {
              Toast.makeText(this, getString(R.string.invalid_entry), Toast.LENGTH_LONG)
                  .show()
            }
          }
          .setNegativeButton(R.string.cancel,
              DialogInterface.OnClickListener
              { _, _ ->
              })
      builder
          .create()
          .show()
    }

    addEventsButton.setOnClickListener {
      val builder = AlertDialog.Builder(this)
      // Get the layout inflater
      val inflater = LayoutInflater.from(this)

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(R.layout.add_event, null)
      val eventHeadlineView = dialogView.findViewById<TextView>(R.id.eventHeadline)
      eventHeadlineView.text = getString(R.string.add_event)
      val textInputEditEventTitleView =
        dialogView.findViewById<TextView>(R.id.textInputEditEventTitle)
      val textInputEditEventNoteView =
        dialogView.findViewById<TextView>(R.id.textInputEditEventNote)
      val datePickerEventDateView = dialogView.findViewById<DatePicker>(R.id.datePickerEventDate)
      val datePickerEventTimeView = dialogView.findViewById<TimePicker>(R.id.datePickerEventTime)

      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.add
          ) { _, _ ->
            val title = textInputEditEventTitleView.text.toString()
            val note = textInputEditEventNoteView.text.toString()
            val datetime: LocalDateTime = LocalDateTime.of(
                datePickerEventDateView.year, datePickerEventDateView.month + 1,
                datePickerEventDateView.dayOfMonth, datePickerEventTimeView.hour,
                datePickerEventTimeView.minute
            )
            val seconds = datetime.toEpochSecond(ZoneOffset.UTC)
            // add new event
            stockRoomViewModel.addEvent(
                Event(symbol = symbol, type = 0, title = title, note = note, datetime = seconds)
            )
            Toast.makeText(
                this, getString(
                R.string.event_added, title, datetime.format(
                DateTimeFormatter.ofLocalizedDateTime(
                    FormatStyle.MEDIUM
                )
            )
            ), Toast.LENGTH_LONG
            )
                .show()
          }
          .setNegativeButton(
              R.string.cancel
          ) { _, _ ->
          }
      builder
          .create()
          .show()
    }

    updateNotesButton.setOnClickListener {
      updateNotes()
    }
    notesTextView.setOnClickListener {
      updateNotes()
    }

    alertAboveInputEditText.addTextChangedListener(
        object : TextWatcher {
          override fun afterTextChanged(s: Editable?) {
            alertAboveInputLayout.error = ""
            var valid: Boolean = true
            if (s != null) {
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                alertAbove = numberFormat.parse(s.toString())
                    .toFloat()
              } catch (e: NumberFormatException) {
                alertAboveInputLayout.error = getString(R.string.invalid_number)
                valid = false
              } catch (e: Exception) {
                valid = false
              }

              if (valid && alertAbove == 0f) {
                alertAboveInputLayout.error = getString(R.string.invalid_number)
                valid = false
              }
              if (valid && alertAbove > 0f && alertBelow > 0f) {
                if (valid && alertBelow >= alertAbove) {
                  alertAboveInputLayout.error = getString(R.string.alert_below_error)
                  valid = false
                }
              }

              if (!valid) {
                alertAbove = 0f
              }
            }
          }

          override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
          ) {
          }

          override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int
          ) {
          }
        })

    alertBelowInputEditText.addTextChangedListener(
        object : TextWatcher {
          override fun afterTextChanged(s: Editable?) {
            alertBelowInputLayout.error = ""
            var valid: Boolean = true
            if (s != null) {
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                alertBelow = numberFormat.parse(s.toString())
                    .toFloat()
              } catch (e: NumberFormatException) {
                alertBelowInputLayout.error = getString(R.string.invalid_number)
                valid = false
              } catch (e: Exception) {
                valid = false
              }
            }

            if (valid && alertBelow == 0f) {
              alertBelowInputLayout.error = getString(R.string.invalid_number)
              valid = false
            }
            if (valid && alertAbove > 0f && alertBelow > 0f) {
              if (valid && alertBelow >= alertAbove) {
                alertBelowInputLayout.error = getString(R.string.alert_above_error)
                valid = false
              }
            }

            if (!valid) {
              alertBelow = 0f
            }
          }

          override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
          ) {
          }

          override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int
          ) {
          }
        })
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.updateOnlineDataManually()
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  override fun onBackPressed() {
    updateAlerts()
    super.onBackPressed()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.stock_data_menu, menu)
    return true
  }

  fun onSync(item: MenuItem) {
    stockRoomViewModel.updateOnlineDataManually()
    stockRoomViewModel.logDebug("Update online data manually for stock data.")
  }

  fun onDelete(item: MenuItem) {
    AlertDialog.Builder(this)
        .setTitle(R.string.delete)
        .setMessage(getString(R.string.delete_stock, symbol))
        .setPositiveButton(R.string.delete) { dialog, _ ->
          Storage.deleteStockHandler.postValue(symbol)
          dialog.dismiss()
          finish()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
//    Toast.makeText(
//        applicationContext,
//        "delete: ${symbol}",
//        Toast.LENGTH_LONG
//    )
//        .show()
  }

  private fun hideSoftInputFromWindow() {
    val view = currentFocus
    if (view is TextView) {
      val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
  }

  private fun updateNotes() {
    val builder = AlertDialog.Builder(this)
    // Get the layout inflater
    val inflater = LayoutInflater.from(this)

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.add_note, null)
    val textInputEditNoteView =
      dialogView.findViewById<TextView>(R.id.textInputEditEventNote)

    val note = notesTextView.text
    textInputEditNoteView.text = note

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.add
        ) { _, _ ->
          val noteText = textInputEditNoteView.text.toString()

          if (noteText != note) {
            notesTextView.text = noteText
            stockRoomViewModel.updateNotes(symbol, noteText)

            if (noteText.isEmpty()) {
              Toast.makeText(
                  this, getString(R.string.note_deleted), Toast.LENGTH_LONG
              )
                  .show()
            } else {
              Toast.makeText(
                  this, getString(
                  R.string.note_added, noteText
              ), Toast.LENGTH_LONG
              )
                  .show()
            }
          }
        }
        .setNegativeButton(
            R.string.cancel
        ) { _, _ ->
        }
    builder
        .create()
        .show()
  }

  private fun updateHeader(onlineMarketData: OnlineMarketData?) {
    var name: String = ""
    var marketPrice: Float = 0f
    var marketChange: String = ""

    if (onlineMarketData != null) {
      name = onlineMarketData.name
      marketPrice = onlineMarketData.marketPrice
      marketChange = "${DecimalFormat("0.00##").format(
          onlineMarketData.marketChange
      )} (${DecimalFormat(
          "0.00##"
      ).format(onlineMarketData.marketChangePercent)}%)"
    }

    textViewName.text = name
    textViewMarketPrice.text = DecimalFormat("0.00##").format(marketPrice)
    textViewChange.text = marketChange
  }

  private fun updateAlerts() {
    // If both are set, below value must be smaller than the above value.
    if (!(alertAbove > 0f && alertBelow > 0f && alertBelow >= alertAbove)) {
      if (stockDBdata.alertAbove != alertAbove) {
        stockRoomViewModel.updateAlertAbove(symbol, alertAbove)
      }
      if (stockDBdata.alertBelow != alertBelow) {
        stockRoomViewModel.updateAlertBelow(symbol, alertBelow)
      }
    }
  }

  private fun updateStockViewRange(_stockViewRange: StockViewRange) {
    stockDataEntries = null
    updateStockView(_stockViewRange, stockViewMode)
  }

  private fun updateStockViewMode(
    _stockViewMode: StockViewMode
  ) {
    updateStockView(stockViewRange, _stockViewMode)
  }

  private fun updateStockView(
    _stockViewRange: StockViewRange,
    _stockViewMode: StockViewMode
  ) {
    stockViewMode = _stockViewMode
    //AppPreferences.INSTANCE.stockViewMode = stockViewMode

    stockViewRange = _stockViewRange
    //AppPreferences.INSTANCE.stockViewRange = stockViewRange

    setupCharts(stockViewRange, stockViewMode)
    fetchStockView(stockViewRange, stockViewMode)
  }

  private fun fetchStockView(
    stockViewRange: StockViewRange,
    stockViewMode: StockViewMode
  ) {
    if (stockDataEntries == null) {
      fetchData(stockViewRange)
    } else {
      loadCharts(symbol, stockViewRange, stockViewMode)
    }
  }

  private fun fetchData(stockViewRange: StockViewRange) {
    // Valid intervals: [1m, 2m, 5m, 15m, 30m, 60m, 90m, 1h, 1d, 5d, 1wk, 1mo, 3mo]
    // Valid ranges: ["1d","5d","1mo","3mo","6mo","1y","2y","5y","ytd","max"]
    when (stockViewRange) {
      StockViewRange.OneDay -> {
        viewModel.fetchYahooChartData(symbol, "5m", "1d")
      }
      StockViewRange.FiveDays -> {
        viewModel.fetchYahooChartData(symbol, "15m", "5d")
      }
      StockViewRange.OneMonth -> {
        viewModel.fetchYahooChartData(symbol, "90m", "1mo")
      }
      StockViewRange.ThreeMonth -> {
        viewModel.fetchYahooChartData(symbol, "1d", "3mo")
      }
      StockViewRange.YTD -> {
        viewModel.fetchYahooChartData(symbol, "1d", "ytd")
      }
      StockViewRange.OneYear -> {
        viewModel.fetchYahooChartData(symbol, "1d", "1y")
      }
      StockViewRange.FiveYears -> {
        viewModel.fetchYahooChartData(symbol, "1d", "5y")
      }
      StockViewRange.Max -> {
        viewModel.fetchYahooChartData(symbol, "1d", "max")
      }
    }
  }

  private fun setupCharts(
    stockViewRange: StockViewRange,
    stockViewMode: StockViewMode
  ) {
    updateButtons(stockViewRange, stockViewMode)

    when (stockViewMode) {
      StockViewMode.Line -> {
        setupLineChart(stockViewRange)
      }
      StockViewMode.Candle -> {
        setupCandleStickChart(stockViewRange)
      }
    }
  }

  private fun loadCharts(
    ticker: String,
    stockViewRange: StockViewRange,
    stockViewMode: StockViewMode
  ) {
    when (stockViewMode) {
      StockViewMode.Line -> {
        loadLineChart(ticker, stockViewRange)
      }
      StockViewMode.Candle -> {
        loadCandleStickChart(ticker, stockViewRange)
      }
    }
  }

  private val rangeButtons: List<Button> by lazy {
    listOf<Button>(
        buttonOneDay,
        buttonFiveDays,
        buttonOneMonth,
        buttonThreeMonth,
        buttonYTD,
        buttonOneYear,
        buttonFiveYears,
        buttonMax
    )
  }

  // Setup formatter for X and Y Axis and data slider.
  private val axisTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(SHORT)
  private val axisDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(LONG, SHORT)
  private val xAxisDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(SHORT)
  private val axisDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(LONG)

  private val xAxisFormatter: ValueFormatter
    get() =
      when (stockViewRange) {
        StockViewRange.OneDay -> {
          IndexAxisValueFormatter(stockDataEntries!!.map { stockDataEntry ->
            val date =
              LocalDateTime.ofEpochSecond(
                  stockDataEntry.dateTimePoint, 0, ZoneOffset.UTC
              )
            date.format(axisTimeFormatter)
          })
        }
        else -> {
          IndexAxisValueFormatter(stockDataEntries!!.map { stockDataEntry ->
            val date =
              LocalDateTime.ofEpochSecond(
                  stockDataEntry.dateTimePoint, 0, ZoneOffset.UTC
              )
            date.format(xAxisDateFormatter)
          })
        }
      }

  private fun updateButtons(
    stockViewRange: StockViewRange,
    stockViewMode: StockViewMode
  ) {
    rangeButtons.forEach { button ->
      button.isEnabled = true
    }

    when (stockViewRange) {
      StockViewRange.OneDay -> {
        buttonOneDay.isEnabled = false
        textViewRange.text = getString(R.string.one_day_range)
      }
      StockViewRange.FiveDays -> {
        buttonFiveDays.isEnabled = false
        textViewRange.text = getString(R.string.five_days_range)
      }
      StockViewRange.OneMonth -> {
        buttonOneMonth.isEnabled = false
        textViewRange.text = getString(R.string.one_month_range)
      }
      StockViewRange.ThreeMonth -> {
        buttonThreeMonth.isEnabled = false
        textViewRange.text = getString(R.string.three_month_range)
      }
      StockViewRange.YTD -> {
        buttonYTD.isEnabled = false
        textViewRange.text = getString(R.string.ytd_range)
      }
      StockViewRange.OneYear -> {
        buttonOneYear.isEnabled = false
        textViewRange.text = getString(R.string.one_year_range)
      }
      StockViewRange.FiveYears -> {
        buttonFiveYears.isEnabled = false
        textViewRange.text = getString(R.string.five_year_range)
      }
      StockViewRange.Max -> {
        buttonMax.isEnabled = false
        textViewRange.text = getString(R.string.max_range)
      }
    }

    when (stockViewMode) {
      StockViewMode.Line -> {
        lineChart.visibility = View.VISIBLE
        candleStickChart.visibility = View.GONE
        imageButtonIconLine.visibility = View.VISIBLE
        imageButtonIconCandle.visibility = View.GONE
      }
      StockViewMode.Candle -> {
        lineChart.visibility = View.GONE
        candleStickChart.visibility = View.VISIBLE
        imageButtonIconLine.visibility = View.GONE
        imageButtonIconCandle.visibility = View.VISIBLE
      }
    }
  }

  protected fun setupCandleStickChart(stockViewRange: StockViewRange) {
    val candleStickChart
        : CandleStickChart = findViewById(R.id.candleStickChart)
    candleStickChart.isDoubleTapToZoomEnabled = false
    candleStickChart.axisLeft.setDrawGridLines(false)
    candleStickChart.axisLeft.setDrawAxisLine(false)
    candleStickChart.axisLeft.isEnabled = false
    candleStickChart.axisRight.setDrawGridLines(false)
    candleStickChart.axisRight.setDrawAxisLine(true)
    candleStickChart.axisRight.isEnabled = true
    candleStickChart.xAxis.setDrawGridLines(false)
    candleStickChart.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
    candleStickChart.legend.isEnabled = false
    candleStickChart.description = null
    candleStickChart.setNoDataText("")
  }

  private fun loadCandleStickChart(
    ticker: String,
    stockViewRange: StockViewRange
  ) {
    val candleStickChart: CandleStickChart = findViewById(R.id.candleStickChart)
    if (stockDataEntries == null || stockDataEntries!!.isEmpty()) {
      //candleStickChart.setNoDataText(""))
      candleStickChart.invalidate()
      return
    }
    candleStickChart.setNoDataText("")

    candleStickChart.candleData?.clearValues()

    val candleEntries: MutableList<CandleEntry> = mutableListOf()
    stockDataEntries!!.forEach { stockDataEntry ->
      candleEntries.add(stockDataEntry.candleEntry)
    }
    val series = CandleDataSet(candleEntries, ticker)
    series.color = Color.rgb(0, 0, 255)
    series.shadowColor = Color.rgb(255, 255, 0)
    series.shadowWidth = 1f
    series.decreasingColor = Color.rgb(255, 0, 0)
    series.decreasingPaintStyle = Paint.Style.FILL
    series.increasingColor = Color.rgb(0, 255, 0)
    series.increasingPaintStyle = Paint.Style.FILL
    series.neutralColor = Color.LTGRAY
    series.setDrawValues(false)
    candleStickChart.data = CandleData(series)

    when (stockViewRange) {
      StockViewRange.OneDay -> {
        candleStickChart.marker =
          TextMarkerViewCandleChart(this, axisTimeFormatter, stockDataEntries!!)
      }
      StockViewRange.FiveDays, StockViewRange.OneMonth -> {
        candleStickChart.marker =
          TextMarkerViewCandleChart(this, axisDateTimeFormatter, stockDataEntries!!)
      }
      else -> {
        candleStickChart.marker =
          TextMarkerViewCandleChart(this, axisDateFormatter, stockDataEntries!!)
      }
    }

    val xAxis: XAxis = candleStickChart.xAxis
    val yAxis: YAxis = candleStickChart.axisRight

    xAxis.valueFormatter = xAxisFormatter
    yAxis.valueFormatter = DefaultValueFormatter(2)

    xAxis.position = XAxis.XAxisPosition.BOTTOM
    yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    xAxis.setDrawAxisLine(true)
    yAxis.setDrawAxisLine(true)
    xAxis.setDrawGridLines(false)
    yAxis.setDrawGridLines(true)

    candleStickChart.invalidate()
  }

  private fun setupLineChart(stockViewRange: StockViewRange) {
    val lineChart: LineChart = findViewById(R.id.lineChart)
    lineChart.isDoubleTapToZoomEnabled = false
    lineChart.axisLeft.setDrawGridLines(false)
    lineChart.axisLeft.setDrawAxisLine(false)
    lineChart.axisLeft.isEnabled = false
    lineChart.axisRight.setDrawGridLines(false)
    lineChart.axisRight.setDrawAxisLine(true)
    lineChart.axisRight.isEnabled = true
    lineChart.xAxis.setDrawGridLines(false)
    lineChart.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
    lineChart.legend.isEnabled = false
    lineChart.description = null
    lineChart.setNoDataText("")
  }

  private fun loadLineChart(
    ticker: String,
    stockViewRange: StockViewRange
  ) {
    val lineChart: LineChart = findViewById(R.id.lineChart)
    if (stockDataEntries == null || stockDataEntries!!.isEmpty()) {
      lineChart.invalidate()
      return
    }
    lineChart.setNoDataText("")
    lineChart.lineData?.clearValues()

    val dataPoints = ArrayList<DataPoint>()
    stockDataEntries!!.forEach { stockDataEntry ->
      dataPoints.add(DataPoint(stockDataEntry.candleEntry.x, stockDataEntry.candleEntry.y))
    }

    val series = LineDataSet(dataPoints as List<Entry>?, ticker)

    series.setDrawHorizontalHighlightIndicator(false)
    series.setDrawValues(false)
    series.setDrawFilled(true)
    series.setDrawCircles(false)

    val lineData = LineData(series)
    lineChart.data = lineData
    val xAxis: XAxis = lineChart.xAxis
    val yAxis: YAxis = lineChart.axisRight

    when (stockViewRange) {
      StockViewRange.OneDay -> {
        lineChart.marker = TextMarkerViewLineChart(this, axisTimeFormatter, stockDataEntries!!)
      }
      StockViewRange.FiveDays, StockViewRange.OneMonth -> {
        lineChart.marker =
          TextMarkerViewLineChart(this, axisDateTimeFormatter, stockDataEntries!!)
      }
      else -> {
        lineChart.marker = TextMarkerViewLineChart(this, axisDateFormatter, stockDataEntries!!)
      }
    }

    xAxis.valueFormatter = xAxisFormatter
    yAxis.valueFormatter = DefaultValueFormatter(2)

    xAxis.position = XAxis.XAxisPosition.BOTTOM
    yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    xAxis.setDrawAxisLine(true)
    yAxis.setDrawAxisLine(true)
    xAxis.setDrawGridLines(false)
    yAxis.setDrawGridLines(true)

    lineChart.invalidate()
  }
}