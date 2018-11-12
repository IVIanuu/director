/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.director.sample.controller

import android.os.Handler
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivianuu.director.common.contextRef
import com.ivianuu.director.internal.d
import com.ivianuu.director.sample.R
import kotlinx.android.synthetic.main.controller_arch.*

annotation class Inject

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ArchController : BaseController() {

    override val layoutRes = R.layout.controller_arch

    private val viewModel by lazy {
        ViewModelProvider(this, ViewModelProvider.NewInstanceFactory())
            .get(ArchViewModel::class.java)
    }

    @Inject var haha by contextRef<Boolean>()

    override fun onAttach(view: View) {
        super.onAttach(view)

        viewModel.count.observe(this, Observer { tv_title.text = "Count: $it" })
    }
}

class ArchViewModel : ViewModel() {

    val count: LiveData<Long> get() = _count
    private val _count = MutableLiveData<Long>().apply { value = 0 }

    private val handler = Handler()

    private val runnable = object : Runnable {
        override fun run() {
            _count.value = _count.value!!.inc()
            d { "increased count $count" }
            handler.postDelayed(this, 1000)
        }
    }

    init {
        handler.postDelayed(runnable, 1000)
    }

    override fun onCleared() {
        d { "on cleared" }
        handler.removeCallbacksAndMessages(null)
        super.onCleared()
    }
}