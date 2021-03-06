package com.example.android.stockroom

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

import android.content.Context
import android.graphics.Color
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * This is not meant to be a full set of tests. For simplicity, most of your samples do not
 * include tests. However, when building the Room, it is helpful to make sure it works before
 * adding the UI.
 */

@RunWith(AndroidJUnit4::class)
class StockRoomDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var stockRoomDao: StockRoomDao
    private lateinit var db: StockRoomDatabase

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext()
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        db = Room.inMemoryDatabaseBuilder(context, StockRoomDatabase::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()
        stockRoomDao = db.stockRoomDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGet() {
        val data = StockDBdata("symbol")
        stockRoomDao.insert(data)
        val allStockDBdata = stockRoomDao.getAllProperties().waitForValue()
        assertEquals(allStockDBdata[0].symbol, data.symbol)
    }

    @Test
    @Throws(Exception::class)
    fun updateProperties() {
        val stockDBdata1 = StockDBdata("symbol1")
        stockRoomDao.insert(stockDBdata1)
        val allStockDBdata1 = stockRoomDao.getAllProperties().waitForValue()
        assertEquals(allStockDBdata1[0].symbol, stockDBdata1.symbol)
        assertEquals(allStockDBdata1[0].notes, "")
        assertEquals(allStockDBdata1[0].alertBelow, 0f)
        assertEquals(allStockDBdata1[0].alertAbove, 0f)

        val stockDBdata2 = StockDBdata("symbol1", alertBelow = 1f, alertAbove = 2f)
        stockRoomDao.insert(stockDBdata2)
        val allStockDBdata2 = stockRoomDao.getAllProperties().waitForValue()
        assertEquals(allStockDBdata2.size, 1)
        assertEquals(allStockDBdata2[0].symbol, stockDBdata2.symbol)
        assertEquals(allStockDBdata2[0].notes, stockDBdata2.notes)
        assertEquals(allStockDBdata2[0].alertBelow, stockDBdata2.alertBelow)
        assertEquals(allStockDBdata2[0].alertAbove, stockDBdata2.alertAbove)

        stockRoomDao.updateAlertAbove("symbol1", 123f)
        val allStockDBdata3 = stockRoomDao.getAllProperties().waitForValue()
        assertEquals(allStockDBdata3.size, 1)
        assertEquals(allStockDBdata3[0].symbol, stockDBdata2.symbol)
        assertEquals(allStockDBdata3[0].notes, stockDBdata2.notes)
        assertEquals(allStockDBdata3[0].alertBelow, stockDBdata2.alertBelow)
        assertEquals(allStockDBdata3[0].alertAbove, 123f)

        stockRoomDao.updateAlertBelow("symbol1", 10f)
        val allStockDBdata4 = stockRoomDao.getAllProperties().waitForValue()
        assertEquals(allStockDBdata3.size, 1)
        assertEquals(allStockDBdata4[0].symbol, stockDBdata2.symbol)
        assertEquals(allStockDBdata4[0].notes, stockDBdata2.notes)
        assertEquals(allStockDBdata4[0].alertBelow, 10f)
        assertEquals(allStockDBdata4[0].alertAbove, 123f)

        stockRoomDao.updateNotes("symbol1", "new notes")
        val allStockDBdata5 = stockRoomDao.getAllProperties().waitForValue()
        assertEquals(allStockDBdata3.size, 1)
        assertEquals(allStockDBdata5[0].symbol, stockDBdata2.symbol)
        assertEquals(allStockDBdata5[0].notes, "new notes")
        assertEquals(allStockDBdata5[0].alertBelow, 10f)
        assertEquals(allStockDBdata5[0].alertAbove, 123f)
    }

    @Test
    @Throws(Exception::class)
    fun getAllStockDBdata() {
//        val assets: ArrayList<Asset> = arrayListOf(Asset(symbol = "symbol", shares = 123f, price = 321f))
//        val assets1: List<String> = listOf("symbol1", "symbol2")

//        val gson = Gson()
//        val assetStr = gson.toJson(assets)
//        val stockDBdata1 = StockDBdata("aaa", alertBelow = 1f, alertAbove = 2f, assets1 = assetStr, assets2 = assets)
        val stockDBdata1 = StockDBdata("aaa", alertBelow = 1f, alertAbove = 2f)
        stockRoomDao.insert(stockDBdata1)
        val stockDBdata2 = StockDBdata("bbb")
        stockRoomDao.insert(stockDBdata2)
        val allStockDBdata = stockRoomDao.getAll().waitForValue()

//        val sType = object : TypeToken<List<Asset>>() { }.type
//        val assetJson = gson.fromJson<List<Asset>>(allStockDBdata[0].assets1, sType)
        assertEquals(allStockDBdata[0].symbol, stockDBdata1.symbol)
        assertEquals(allStockDBdata[1].symbol, stockDBdata2.symbol)
    }

    @Test
    @Throws(Exception::class)
    fun getAssets() {
        val stockDBdata1 = StockDBdata("symbol1")
        stockRoomDao.insert(stockDBdata1)
        val asset1 = Asset(symbol = "symbol1", shares = 10f, price = 123f)
        stockRoomDao.addAsset(asset1)
        stockRoomDao.addAsset(asset1)
        val asset2 = Asset(symbol = "symbol1", shares = 20f, price = 223f)
        stockRoomDao.addAsset(asset2)
        val asset3 = Asset(symbol = "symbol2", shares = 30f, price = 323f)
        stockRoomDao.addAsset(asset3)
        val stockDBdata2 = StockDBdata("symbol2")
        stockRoomDao.insert(stockDBdata2)

        val assets1 = stockRoomDao.getAssets("symbol1")
        assertEquals(assets1.assets.size, 3)
        assertEquals(assets1.assets[0].shares, asset1.shares)
        assertEquals(assets1.assets[0].price, asset1.price)
        assertEquals(assets1.assets[2].shares, asset2.shares)
        assertEquals(assets1.assets[2].price, asset2.price)

        stockRoomDao.deleteAsset(symbol = asset1.symbol, shares = asset1.shares, price = asset1.price)
        val assetsDel1 = stockRoomDao.getAssets("symbol1")
        assertEquals(assetsDel1.assets.size, 1)

        val assets2 = stockRoomDao.getAssets("symbol2")
        assertEquals(assets2.assets.size, 1)
        assertEquals(assets2.assets[0].shares, asset3.shares)
        assertEquals(assets2.assets[0].price, asset3.price)

        stockRoomDao.deleteAssets("symbol1")
        val assets3 = stockRoomDao.getAssets("symbol1")
        assertEquals(assets3, null)
    }

    @Test
    @Throws(Exception::class)
    fun updateAssets() {
        val stockDBdata1 = StockDBdata("symbol1")
        stockRoomDao.insert(stockDBdata1)

        var assets: MutableList<Asset> = mutableListOf()
        assets.add(Asset(symbol = "symbol1", shares = 10f, price = 123f))
        assets.add(Asset(symbol = "symbol1", shares = 20f, price = 223f))
        //assets.add(Asset(symbol = "symbol2", shares = 30f, price = 323f))

        // Update = delete + add
        stockRoomDao.deleteAssets("symbol1")
        assets.forEach { asset ->
            stockRoomDao.addAsset(asset)
        }

        val assets1 = stockRoomDao.getAssets("symbol1")
        assertEquals(assets1.assets.size, 2)

        stockRoomDao.deleteAssets("symbol1")
        val assetsdel = stockRoomDao.getAssets("symbol1")
        assertEquals(assetsdel.assets.size, 0)
    }

    @Test
    @Throws(Exception::class)
    fun addDeleteGroups() {
        var groups1: MutableList<Group> = mutableListOf()
        groups1.add(Group(color = 1, name = "g1"))
        groups1.add(Group(color = 2, name = "g2"))
        stockRoomDao.setGroups(groups1)

        // Add groups with two elements.
        val groups2 = stockRoomDao.getGroups()
        assertEquals(groups2.size, 2)

        // Add one group
        stockRoomDao.setGroup(Group(color = 10, name = "g10"))

        val groups3 = stockRoomDao.getGroups()
        assertEquals(groups3.size, 3)

        // Set again groups with two elements.
        stockRoomDao.setGroups(groups1)

        // previously set group g10,10 is not deleted
        val groups4 = stockRoomDao.getGroups()
        assertEquals(groups4.size, 3)

        // Delete all groups
        stockRoomDao.deleteAllGroups()
        val groups5 = stockRoomDao.getGroups()
        assertEquals(groups5.size, 0)
    }

    @Test
    @Throws(Exception::class)
    fun updateGroups() {
        val stockDBdata1 = StockDBdata(symbol = "symbol")
        stockRoomDao.insert(stockDBdata1)
        val stockDBdata2 = stockRoomDao.getStockDBdata("symbol")
        assertEquals(stockDBdata2.groupColor, 0)
        stockRoomDao.setStockGroup("symbol",  0xfff000)
        val stockDBdata3 = stockRoomDao.getStockDBdata("symbol")
        assertEquals(stockDBdata3.groupColor, 0xfff000)

        var groups1: MutableList<Group> = mutableListOf()
        groups1.add(Group(color = 1, name = "g1"))
        groups1.add(Group(color = 2, name = "g2"))
        var groups12: MutableList<Group> = mutableListOf()
        groups12.add(Group(color = 12, name = "g12"))
        groups12.add(Group(color = 22, name = "g22"))
        stockRoomDao.setGroups(groups12)

        val groups2 = stockRoomDao.getGroups()
        assertEquals(groups2.size, 2)
        assertEquals(groups2[0].name, "g12")
        assertEquals(groups2[0].color, 12)
        assertEquals(groups2[1].name, "g22")
        assertEquals(groups2[1].color, 22)

        val group1 = stockRoomDao.getGroup(12)
        assertEquals(group1.name, "g12")
        val group10 = stockRoomDao.getGroup(10)
        assertEquals(group10, null)

        val groupsA = stockRoomDao.getGroups()
        assertEquals(groupsA.size, 2)

        // group with color 10 does not exist and will be added
        stockRoomDao.setGroup(Group(color = 10, name = "g10"))

        val groupsB = stockRoomDao.getGroups()
        assertEquals(groupsB.size, 3)

        // group with color 10 does exist and will be updated
        stockRoomDao.setGroup(Group(color = 10, name = "g100"))

        val groupsC = stockRoomDao.getGroups()
        assertEquals(groupsC.size, 3)

        val group10Updated = stockRoomDao.getGroup(10)
        assertEquals(group10Updated.name, "g10")
        stockRoomDao.updateGroup(10, "g1010")
        val group1010 = stockRoomDao.getGroup(10)
        assertEquals(group1010.name, "g1010")

        val groupsD = stockRoomDao.getGroups()
        assertEquals(groupsD.size, 2)
    }

    @Test
    @Throws(Exception::class)
    fun setGroupColor() {
        val groups: MutableList<Group> = mutableListOf()
        groups.add(Group(color = 1, name = "Kaufen"))
        groups.add(Group(color = 2 ,name = "Verkaufen"))
        groups.add(Group(color = 3, name = "Beobachten"))
        stockRoomDao.setGroups(groups)

        stockRoomDao.insert(StockDBdata(symbol = "MSFT", groupColor = 1))
        stockRoomDao.insert(StockDBdata(symbol ="AAPL", groupColor = 2))
        stockRoomDao.insert(StockDBdata(symbol ="AMZN"))
        stockRoomDao.insert(StockDBdata(symbol ="TLSA", groupColor = Color.WHITE))
        stockRoomDao.insert(StockDBdata(symbol ="VZ", groupColor = 5))

        stockRoomDao.setGroup(Group(color = 5, name = "test1"))
        // Overwrite the previous group
        stockRoomDao.setGroup(Group(color = 5, name = "test2"))
        // Same name, but different color
        stockRoomDao.setGroup(Group(color = 6, name = "test2"))
        stockRoomDao.setStockGroup(symbol = "VZ", color = Color.BLACK)
        val stockDBdata3 = stockRoomDao.getStockDBdata("VZ")
        assertEquals(stockDBdata3.groupColor, Color.BLACK)

        val groups1 = stockRoomDao.getGroups()
        assertEquals(groups1.size, 5)
        assertEquals(groups1[3].name, "test2")
        assertEquals(groups1[2].color, 5)
    }

    @Test
    @Throws(Exception::class)
    fun getEvents() {
        val StockDBdata1 = StockDBdata("symbol1")
        stockRoomDao.insert(StockDBdata1)
        val current = LocalDateTime.now()

        val dateTime1 = current.toEpochSecond(ZoneOffset.MIN)
        val event1 = Event(symbol = "symbol1", type = 1, title = "title1", note = "note1", datetime = dateTime1)
        stockRoomDao.addEvent(event1)

        val dateTime2 = current.toEpochSecond(ZoneOffset.UTC)
        val event2 = Event(symbol = "symbol1", type = 2, title = "title2", note = "note2", datetime = dateTime2)
        stockRoomDao.addEvent(event2)

        val dateTime3 = current.toEpochSecond(ZoneOffset.MAX)
        val event3 = Event(symbol = "symbol2", type = 3, title = "title3", note = "note3", datetime = dateTime3)
        stockRoomDao.addEvent(event3)
        val StockDBdata2 = StockDBdata("symbol2")
        stockRoomDao.insert(StockDBdata2)

        val events1 = stockRoomDao.getEvents("symbol1")
        assertEquals(events1.events.size, 2)
        assertEquals(events1.events[0].type, event1.type)
        assertEquals(events1.events[0].datetime, dateTime1)
        assertEquals(events1.events[1].type, event2.type)
        assertEquals(events1.events[1].datetime,  dateTime2)

        val events2 = stockRoomDao.getEvents("symbol2")
        assertEquals(events2.events.size, 1)
        assertEquals(events2.events[0].type, event3.type)
        assertEquals(events2.events[0].datetime, dateTime3)
    }

    @Test
    @Throws(Exception::class)
    fun delete() {
        val StockDBdata1 = StockDBdata("symbol1")
        stockRoomDao.insert(StockDBdata1)
        val StockDBdata2 = StockDBdata("symbol2")
        stockRoomDao.insert(StockDBdata2)
        stockRoomDao.delete("symbol1")
        val allStockDBdata = stockRoomDao.getAllProperties().waitForValue()
        assertEquals(allStockDBdata.size, 1)
        assertEquals(allStockDBdata[0].symbol, StockDBdata2.symbol)
    }

    @Test
    @Throws(Exception::class)
    fun deleteAll() {
        val StockDBdata1 = StockDBdata("symbol1")
        stockRoomDao.insert(StockDBdata1)
        val StockDBdata2 = StockDBdata("symbol2")
        stockRoomDao.insert(StockDBdata2)
        stockRoomDao.deleteAllStockTable()
        val allStockDBdata = stockRoomDao.getAllProperties().waitForValue()
        assertTrue(allStockDBdata.isEmpty())
    }
}
