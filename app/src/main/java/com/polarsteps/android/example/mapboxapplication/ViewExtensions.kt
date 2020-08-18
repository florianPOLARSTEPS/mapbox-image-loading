package com.polarsteps.android.example.mapboxapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import coil.ImageLoader
import coil.request.GetRequest

/**
 * Renders the domain data via [View] into an image
 */
suspend fun DomainData.render(
    context: Context,
    imageLoader: ImageLoader
): Bitmap {
    val view = LayoutInflater.from(context).inflate(R.layout.view_complex_map_icon, null, false)
    view.findViewById<TextView>(R.id.text_primary).text = this@render.text
    view.findViewById<TextView>(R.id.text_secondary).text = this@render.otherText
    view.findViewById<ImageView>(R.id.offline_icon).setImageResource(this@render.localIcon)
    val request = GetRequest.Builder(context)
        .data(this@render.urlToDownload)
        .allowHardware(false).build()
    val result = imageLoader.execute(
        request = request
    )
    view.findViewById<ImageView>(R.id.download_icon).setImageDrawable(result.drawable)
    view.measureAndLayout()
    return view.toBitmap()
}

fun View.measureAndLayout(
    widthMeasureSpecs: Int = View.MeasureSpec.UNSPECIFIED,
    heightMeasureSpecs: Int = View.MeasureSpec.UNSPECIFIED
) {
    measure(
        widthMeasureSpecs,
        heightMeasureSpecs
    )
    layout(
        0,
        0,
        measuredWidth,
        measuredHeight
    )
}

fun View.toBitmap(): Bitmap {
    val width = this.width
    val height = this.height
    val b = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )
    val c = Canvas(b)
    c.drawColor(
        Color.TRANSPARENT,
        PorterDuff.Mode.CLEAR
    )
    this.draw(c)
    return b
}
