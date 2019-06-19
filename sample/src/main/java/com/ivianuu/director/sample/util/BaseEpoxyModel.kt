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

import android.view.View
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.ivianuu.epoxyktx.KtEpoxyHolder

/**
 * Base epoxy model with holder
 */
abstract class BaseEpoxyModel : EpoxyModelWithHolder<KtEpoxyHolder>() {

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onClick: ((View) -> Unit)? = null

    protected open val onClickView: View? get() = null
    protected open val useContainerForClicks get() = true

    override fun bind(holder: KtEpoxyHolder) {
        super.bind(holder)

        if (onClickView != null) {
            onClickView?.setOnClickListener(onClick)
        } else if (useContainerForClicks) {
            holder.containerView.setOnClickListener(onClick)
        }
    }

    override fun unbind(holder: KtEpoxyHolder) {
        if (onClickView != null) {
            onClickView?.setOnClickListener(null)
        } else if (useContainerForClicks) {
            holder.containerView.setOnClickListener(null)
        }

        super.unbind(holder)
    }
}