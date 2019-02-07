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

import android.os.Bundle
import android.view.View
import com.ivianuu.director.common.changehandler.horizontal
import com.ivianuu.director.push
import com.ivianuu.director.sample.R
import com.ivianuu.director.sample.util.d
import com.ivianuu.director.scopes.destroy
import com.ivianuu.director.scopes.detach
import com.ivianuu.director.scopes.unbindView
import com.ivianuu.scopes.rx.disposeBy
import io.reactivex.Observable
import kotlinx.android.synthetic.main.controller_scopes.btn_next_release_view
import kotlinx.android.synthetic.main.controller_scopes.btn_next_retain_view
import java.util.concurrent.TimeUnit

class ScopesController : BaseController() {

    override val layoutRes: Int
        get() = R.layout.controller_scopes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        actionBarTitle = "Scopes Demo"

        Observable.interval(1, TimeUnit.SECONDS)
            .doOnDispose { d { "Disposing from onCreate()" } }
            .subscribe {
                d { "Started in onCreate(), running until hostDestroyed(): $it" }
            }
            .disposeBy(destroy)
    }

    override fun onBindView(view: View, savedViewState: Bundle?) {
        super.onBindView(view, savedViewState)
        d { "onBindView() called" }

        btn_next_release_view.setOnClickListener {
            retainView = false

            router.push {
                controller(
                    TextController.newInstance(
                        "Logcat should now report that the observables from onAttach() and onBindView() have been disposed of, while the onCreate() observable is still running."
                    )
                )
                horizontal()
            }
        }

        btn_next_retain_view.setOnClickListener {
            retainView = true

            router.push {
                controller(
                    TextController.newInstance(
                        "Logcat should now report that the observables from onAttach() has been disposed of, while the constructor and onBindView() observables are still running."
                    )
                )
                horizontal()
            }
        }

        Observable.interval(1, TimeUnit.SECONDS)
            .doOnDispose { d { "Disposing from onBindView()" } }
            .subscribe {
                d { "Started in onBindView(), running until onUnbindView(): $it" }
            }
            .disposeBy(unbindView)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        d { "onAttach() called" }

        Observable.interval(1, TimeUnit.SECONDS)
            .doOnDispose { d { "Disposing from onAttach()" } }
            .subscribe {
                d { "Started in onAttach(), running until onDetach(): $it" }
            }
            .disposeBy(detach)
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        d { "onDetach() called" }
    }

    override fun onUnbindView(view: View) {
        super.onUnbindView(view)
        d { "onUnbindView() called" }
    }

    override fun onDestroy() {
        super.onDestroy()
        d { "hostDestroyed() called" }
    }
}