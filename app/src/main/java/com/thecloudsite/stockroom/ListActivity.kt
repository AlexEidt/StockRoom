package com.thecloudsite.stockroom

import android.content.res.Resources
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_list.debugswitch
import kotlinx.android.synthetic.main.activity_list.webview
import okhttp3.internal.toHexString
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

class ListActivity : AppCompatActivity() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  private val stockTableRows = StringBuilder()
  private var stockTableRowsCount = 0

  private val groupTableRows = StringBuilder()
  private var groupTableRowsCount = 0

  private val assetTableRows = StringBuilder()
  private var assetTableRowsCount = 0

  private val eventTableRows = StringBuilder()
  private var eventTableRowsCount = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_list)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val settings = webview.settings
    settings.setJavaScriptEnabled(true);
    settings.setPluginState(WebSettings.PluginState.ON);
    settings.setAllowFileAccess(true);
    settings.setAllowContentAccess(true);
    settings.setAllowFileAccessFromFileURLs(true);
    settings.setAllowUniversalAccessFromFileURLs(true);

    settings.setSupportZoom(true);
    settings.setBlockNetworkImage(false);
    settings.setLoadsImagesAutomatically(true);
    settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);
    settings.setAppCacheEnabled(true);

    settings.setUserAgentString(
        "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 5 Build/LMY48B; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/43.0.2357.65 Mobile Safari/537.36"
    )
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW)
    val cookieManager: CookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webview, true)

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
    debugswitch.isChecked = sharedPreferences.getBoolean("list", false)

    debugswitch.setOnCheckedChangeListener{_, isChecked ->
      sharedPreferences
          .edit()
          .putBoolean("list", isChecked)
          .apply()
    }

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)

/*
    stockRoomViewModel.allStockItems.observe(this, Observer { items ->
      items?.let { stockItemSet ->
        val htmlText = updateHtmlText(stockItemSet)
        val mimeType: String = "text/html"
        val utfType: String = "UTF-8"
        webview.loadDataWithBaseURL(null, htmlText, mimeType, utfType, null);
      }
    })
  */

    stockRoomViewModel.allProperties.observe(this, Observer { items ->

      stockTableRowsCount = items.size

      items.forEach { stockItem ->
        stockTableRows.append("<tr>")
        stockTableRows.append("<td>${stockItem.symbol}</td>")
        stockTableRows.append("<td>${stockItem.portfolio}</td>")
        stockTableRows.append("<td>${stockItem.data}</td>")
        stockTableRows.append("<td>${getColorStr(stockItem.groupColor)}</td>")
        stockTableRows.append("<td>${stockItem.notes}</td>")
        stockTableRows.append("<td>${DecimalFormat("0.####").format(stockItem.alertAbove)}</td>")
        stockTableRows.append(
            "<td>${DecimalFormat("0.####")
                .format(stockItem.alertBelow)}</td>"
        )
        stockTableRows.append("</tr>")
      }

      stockRoomViewModel.allProperties.removeObservers(this)
      updateHtmlText()
    })

    stockRoomViewModel.allGroupTable.observe(this, Observer { items ->

      groupTableRowsCount = items.size

      items.forEach { groupItem ->
        groupTableRows.append("<tr>")
        groupTableRows.append("<td>${getColorStr(groupItem.color)}</td>")
        groupTableRows.append("<td>${groupItem.name}</td>")
        groupTableRows.append("</tr>")
      }

      stockRoomViewModel.allGroupTable.removeObservers(this)
      updateHtmlText()
    })

    stockRoomViewModel.allAssetTable.observe(this, Observer { items ->

      assetTableRowsCount = items.size

      items.forEach { assetItem ->
        assetTableRows.append("<tr>")
        assetTableRows.append("<td>${assetItem.id}</td>")
        assetTableRows.append("<td>${assetItem.symbol}</td>")
        assetTableRows.append("<td>${DecimalFormat("0.####").format(assetItem.shares)}</td>")
        assetTableRows.append("<td>${assetItem.price}</td>")
        assetTableRows.append("</tr>")
      }

      stockRoomViewModel.allAssetTable.removeObservers(this)
      updateHtmlText()
    })

    stockRoomViewModel.allEventTable.observe(this, Observer { items ->

      eventTableRowsCount = items.size

      items.forEach { eventItem ->
        eventTableRows.append("<tr>")
        eventTableRows.append("<td>${eventItem.id}</td>")
        eventTableRows.append("<td>${eventItem.symbol}</td>")
        eventTableRows.append("<td>${eventItem.type}</td>")
        eventTableRows.append("<td>${eventItem.title}</td>")
        eventTableRows.append("<td>${eventItem.note}</td>")
        eventTableRows.append("<td>${getDateTimeStr(eventItem.datetime)}</td>")
        eventTableRows.append("</tr>")
      }

      stockRoomViewModel.allEventTable.removeObservers(this)
      updateHtmlText()
    })

    //webview.setBackgroundResource(R.drawable.circuit)
    //webview.setBackgroundColor(android.graphics.Color.TRANSPARENT);
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  private fun getDateTimeStr(datetime: Long): String {
    val localDateTime: LocalDateTime = LocalDateTime.ofEpochSecond(datetime, 0, ZoneOffset.UTC)
    val dateTimeStr =
      "${datetime}</br>${localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
      }&nbsp;${localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
      }"
    return dateTimeStr
  }

  private fun getColorStr(color: Int): String =
    if (color == 0) {
      "$color"
    } else {
      val hexStr = "0x${color.toHexString()}"
      val colorCode = hexStr.replace("0xff", "#")
      val colorSample =
        "<font style=\"background-color: $colorCode;\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>"
      "$color</br>$colorSample&nbsp;$colorCode"
    }

  private fun updateHtmlText() {
    var htmlText = resources.getRawTextFile(R.raw.list)

    htmlText = htmlText.replace("<!-- stock_table_name -->", "stock_table ($stockTableRowsCount)")
    htmlText = htmlText.replace("<!-- stock_table -->", stockTableRows.toString())

    htmlText = htmlText.replace("<!-- group_table_name -->", "group_table ($groupTableRowsCount)")
    htmlText = htmlText.replace("<!-- group_table -->", groupTableRows.toString())

    htmlText = htmlText.replace("<!-- asset_table_name -->", "asset_table ($assetTableRowsCount)")
    htmlText = htmlText.replace("<!-- asset_table -->", assetTableRows.toString())

    htmlText = htmlText.replace("<!-- event_table_name -->", "event_table ($eventTableRowsCount)")
    htmlText = htmlText.replace("<!-- event_table -->", eventTableRows.toString())

    val mimeType: String = "text/html"
    val utfType: String = "UTF-8"
    webview.loadDataWithBaseURL(null, htmlText, mimeType, utfType, null)
  }

  private fun Resources.getRawTextFile(@RawRes id: Int) =
    openRawResource(id).bufferedReader()
        .use { it.readText() }
}