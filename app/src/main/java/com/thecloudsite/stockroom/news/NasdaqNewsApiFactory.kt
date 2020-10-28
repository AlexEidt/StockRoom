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

package com.thecloudsite.stockroom.news

import com.thecloudsite.stockroom.utils.checkUrl

// https://www.nasdaq.com/feed/rssoutbound?symbol=msft

object NasdaqNewsApiFactory : NewsApiFactory() {

  var newsApi: NasdaqNewsApi? = null

  private var defaultUrl = "https://www.nasdaq.com/"

  init {
    update(defaultUrl)
  }

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        newsApi = null
      } else {
        url = checkUrl(_url)
        newsApi = try {
          retrofit().create(NasdaqNewsApi::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }
}

// https://www.nasdaq.com/feed/rssoutbound

object NasdaqAllNewsApiFactory : NewsApiFactory() {

  var newsApi: NasdaqAllNewsApi? = null

  private var defaultUrl = "https://www.nasdaq.com/"

  init {
    update(defaultUrl)
  }

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        newsApi = null
      } else {
        url = checkUrl(_url)
        newsApi = try {
          retrofit().create(NasdaqAllNewsApi::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }
}
