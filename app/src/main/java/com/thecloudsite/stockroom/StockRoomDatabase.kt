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
import android.content.res.Resources
import androidx.annotation.RawRes
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * This is the backend. The database. This used to be done by the OpenHelper.
 * The fact that this has very few comments emphasizes its coolness.
 */
@Database(
    entities = [StockDBdata::class, Group::class, Asset::class, Event::class], version = 1,
    exportSchema = true
)
abstract class StockRoomDatabase : RoomDatabase() {

  abstract fun stockRoomDao(): StockRoomDao

  companion object {
    @Volatile
    private var INSTANCE: StockRoomDatabase? = null

    fun getDatabase(
      context: Context,
      scope: CoroutineScope
    ): StockRoomDatabase {
      // if the INSTANCE is not null, then return it,
      // if it is, then create the database
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
            context.applicationContext,
            StockRoomDatabase::class.java,
            "stockroom_database"
        )
            // Wipes and rebuilds instead of migrating if no Migration object.
            // Migration is not part of this codelab.
            .fallbackToDestructiveMigration()
            .addCallback(StockRoomDatabaseCallback(scope, context))
            .build()
        INSTANCE = instance
        // return instance
        instance
      }
    }

    private class StockRoomDatabaseCallback(
      private val scope: CoroutineScope,
      val context: Context
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateDatabase(database.stockRoomDao(), context)
          }
        }
      }
    }

    private fun importExampleJSON(
      stockRoomDao: StockRoomDao,
      json: String
    ) {
      val sType = object : TypeToken<List<StockItemJson>>() {}.type
      val gson = Gson()
      val stockItemJsonList = gson.fromJson<List<StockItemJson>>(json, sType)

      stockItemJsonList.forEach { stockItemJson ->
        val symbol = stockItemJson.symbol.toUpperCase(Locale.ROOT)
        stockRoomDao.insert(
            StockDBdata(
                symbol = symbol,
                // can be null if it is not in the json
                groupColor = stockItemJson.groupColor ?: 0,
                notes = stockItemJson.notes ?: "",
                alertBelow = stockItemJson.alertBelow ?: 0f,
                alertAbove = stockItemJson.alertAbove ?: 0f
            )
        )

        stockRoomDao.updateAssets(symbol = symbol, assets = stockItemJson.assets.map { asset ->
          Asset(symbol = symbol,
              shares = asset.shares ?: 0f,
              price = asset.price ?: 0f
          )
        })

        stockRoomDao.updateEvents(symbol = symbol, events = stockItemJson.events.map { event ->
          Event(
              symbol = symbol,
              type = event.type ?: 0,
              title = event.title ?: "",
              note = event.note ?: "",
              datetime = event.datetime ?: 0L
          )
        })
      }
    }

    private fun Resources.getRawTextFile(@RawRes id: Int) =
      openRawResource(id).bufferedReader()
          .use { it.readText() }

    fun populateDatabase(
      stockRoomDao: StockRoomDao,
      context: Context
    ) {
      // Add predefined values to the DB.
      stockRoomDao.setPredefinedGroups(context)

      val jsonText = context.resources.getRawTextFile(R.raw.example_stocks)
      importExampleJSON(stockRoomDao, jsonText)

      // List is sorted alphabetically. Add comment about deleting the example list in the first entry.
      stockRoomDao.updateNotes(
          symbol = "AAPL", notes = context.getString(R.string.example_List_delete_all)
      )
      stockRoomDao.updateNotes(
          symbol = "AMZN", notes = context.getString(R.string.example_List_note)
      )

      /*
      stockRoomDao.insert(StockDBdata(symbol = "AAPL", groupColor = Color.BLUE))
      stockRoomDao.addAsset(Asset(symbol = "AAPL", shares = 20f, price = 100f))
      stockRoomDao.updateNotes(symbol = "AAPL", notes = context.getString(R.string.example_List_delete_all))
      stockRoomDao.insert(StockDBdata(symbol = "AMZN", groupColor = Color.BLUE, alertAbove = 4000f))
      stockRoomDao.addAsset(Asset(symbol = "AMZN", shares = 2f, price = 3000f))
      stockRoomDao.updateNotes(symbol = "AMZN", notes = context.getString(R.string.example_List_note))
      stockRoomDao.insert(StockDBdata(symbol = "BA", groupColor = Color.YELLOW, alertBelow = 100f))
      stockRoomDao.addAsset(Asset(symbol = "BA", shares = 30f, price = 200f))
      stockRoomDao.insert(StockDBdata(symbol = "CVX", groupColor = Color.YELLOW))
      stockRoomDao.addAsset(Asset(symbol = "CVX", shares = 40f, price = 100f))
      stockRoomDao.insert(StockDBdata(symbol = "DIS", groupColor = Color.YELLOW))
      stockRoomDao.addAsset(Asset(symbol = "DIS", shares = 15f, price = 150f))
      stockRoomDao.addEvent(
          Event(
              symbol = "DIS", type = 0, datetime = 1619870400, title = "Earnings report",
              note = "Check the DIS site"
          )
      )
      stockRoomDao.insert(StockDBdata(symbol = "FB", groupColor = Color.RED))
      stockRoomDao.addAsset(Asset(symbol = "FB", shares = 12f, price = 120f))
      stockRoomDao.insert(StockDBdata(symbol = "IBM", groupColor = Color.RED))
      stockRoomDao.addAsset(Asset(symbol = "IBM", shares = 20f, price = 200f))
      stockRoomDao.insert(StockDBdata(symbol = "MSFT", groupColor = Color.BLUE))
      stockRoomDao.addAsset(Asset(symbol = "MSFT", shares = 20f, price = 150f))
      stockRoomDao.insert(StockDBdata(symbol = "QCOM", groupColor = Color.GREEN))
      stockRoomDao.addAsset(Asset(symbol = "QCOM", shares = 30f, price = 100f))
      stockRoomDao.insert(StockDBdata(symbol = "T", groupColor = Color.rgb(72, 209, 204)))
      stockRoomDao.addAsset(Asset(symbol = "T", shares = 100f, price = 10f))
      stockRoomDao.insert(StockDBdata(symbol = "TSLA", groupColor = Color.rgb(72, 209, 204)))
      stockRoomDao.addAsset(Asset(symbol = "TSLA", shares = 5f, price = 1000f))
      stockRoomDao.insert(StockDBdata(symbol = "^GSPC", groupColor = 0))
       */
    }
  }
}
