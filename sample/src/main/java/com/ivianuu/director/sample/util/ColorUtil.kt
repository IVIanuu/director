package com.ivianuu.director.sample.util

import android.content.res.Resources
import android.graphics.Color
import com.ivianuu.director.sample.R

object ColorUtil {

    fun getMaterialColor(resources: Resources, index: Int): Int {
        val colors = resources.obtainTypedArray(R.array.mdcolor_300)

        val returnColor = colors.getColor(index % colors.length(), Color.BLACK)

        colors.recycle()
        return returnColor
    }
}
