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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.d
import com.ivianuu.scopes.MutableScope
import com.ivianuu.scopes.rx.disposeBy
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ArchController : BaseController() {

    override val layoutRes get() = R.layout.controller_arch
    override val toolbarTitle: String?
        get() = "Arch"

    // todo

    /*private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.NewInstanceFactory()
        ).get(ArchViewModel::class.java)
    }

    init {
        lifecycle.addObserver(LifecycleEventObserver { source, event ->
            d { "lifecycle event $event" }
        })
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { source, event ->
            d { "view lifecycle event $event" }
        })
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        viewModel.count.observe(this, Observer { tv_title.text = "Count: $it" })
    }
     */
}

class ArchViewModel : ViewModel() {

    val count: LiveData<Long> get() = _count
    private val _count = MutableLiveData<Long>()

    private val scope = MutableScope()

    init {
        scope.addListener { d { "cleared" } }

        Observable.interval(1, TimeUnit.SECONDS)
            .startWith(0)
            .subscribe { _count.postValue(it) }
            .disposeBy(scope)
    }

    override fun onCleared() {
        scope.close()
        super.onCleared()
    }
}