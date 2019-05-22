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

package com.ivianuu.director.sample.util

import android.view.ViewGroup
import com.ivianuu.director.ControllerChangeHandler
import com.ivianuu.director.Controller
import com.ivianuu.director.Router
import com.ivianuu.director.RouterListener

/**
 * @author Manuel Wrage (IVIanuu)
 */
class LoggingRouterListener : RouterListener {
    override fun onChangeStarted(
        router: Router,
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
        d { "on change started: to $to, from $from, is push $isPush, container ${container.childCount}, changeHandler $handler" }
    }

    override fun onChangeEnded(
        router: Router,
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
        container: ViewGroup,
        handler: ControllerChangeHandler
    ) {
        d { "on change completed: to $to, from $from, is push $isPush, container ${container.childCount}, changeHandler $handler" }
    }
}