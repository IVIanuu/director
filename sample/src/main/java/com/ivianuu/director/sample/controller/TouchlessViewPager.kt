package com.ivianuu.director.sample.controller

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class TouchlessViewPager(context: Context, attrs: AttributeSet?) : ViewPager(context, attrs) {
    override fun onInterceptTouchEvent(event: MotionEvent) = false
    override fun onTouchEvent(event: MotionEvent) = false

    override fun setCurrentItem(item: Int) {
        setCurrentItem(item, false)
    }
}