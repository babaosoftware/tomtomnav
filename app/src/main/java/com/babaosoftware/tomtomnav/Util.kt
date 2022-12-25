package com.babaosoftware.tomtomnav

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.roundToInt


fun convertDpToPx(dp: Double, density: Float): Int {
    return (dp * density).roundToInt()
}


