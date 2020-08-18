package com.polarsteps.android.example.mapboxapplication

import android.os.Bundle
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import coil.ImageLoader
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.SupportMapFragment
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), MapView.OnStyleImageMissingListener {

    private var currentStyle: Style? = null

    private val imageLoader by lazy {
        ImageLoader(applicationContext)
    }

    // keep a lookup tables mapping image_identifiers to domain objects
    private val imageLookupTable = mutableMapOf<String, DomainData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_api_key))
        setContentView(R.layout.activity_main)

        val supportMapFragment =
            supportFragmentManager.findFragmentById(R.id.mapbox_fragment) as SupportMapFragment
        (supportMapFragment.view as MapView).addOnStyleImageMissingListener(this)
        supportMapFragment.getMapAsync {
            initMap(it)
        }
    }

    private fun initMap(map: MapboxMap) {

        // we deliver our style.json with the app, including all the layers we need to use.
        val openRawResource = resources.assets.open("style.json")
            .bufferedReader()
            .readText()

        map.setStyle(
            Style.Builder()
                .fromJson(openRawResource)
        ) {
            this.currentStyle = it
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 4.2), 3.0))
            initWithStyle(it)
        }
    }

    private fun initWithStyle(style: Style) {
        updateContent(style)
    }

    /**
     * This is the main function where the data for the map is updated
     * ( can be called multiple times based on when data changes )
     *
     * for the sake of this example it's only called once
     */
    private fun updateContent(style: Style) {
        imageLookupTable.clear()
        // generate some test data
        val featureCollection = FeatureCollection.fromFeatures(
            generateRandomListOfComplexData().map { data ->
                Feature.fromGeometry(
                    Point.fromLngLat(data.longitude, data.latitude),
                    JsonObject(),
                    data.identifier
                )
                    .apply {
                        imageLookupTable[data.identifier] = data
                        this.addStringProperty("custom_icon", data.identifier)
                    }
            })

        var source = style.getSourceAs<GeoJsonSource>("custom")
        if (source == null) {
            source = GeoJsonSource("custom", featureCollection)
            style.addSource(source)
        } else {
            source.setGeoJson(featureCollection)
        }
    }

    /**
     * Just some random complex data
     */
    private fun generateRandomListOfComplexData(): List<DomainData> {
        return (-180).until(180).step(10).map { longitude ->
            return@map DomainData(
                longitude = longitude.toDouble(),
                latitude = 0.0,
                text = "Longitude: $longitude",
                otherText = "Double longitude: ${longitude * 2}",
                urlToDownload = "https://picsum.photos/200/200?random=$longitude",
                localIcon = when (longitude % 3) {
                    0 -> R.drawable.mapbox_compass_icon
                    1 -> R.drawable.mapbox_info_icon_default
                    else -> R.drawable.mapbox_marker_icon_default
                }
            )
        }
    }

    override fun onStyleImageMissing(id: String) {
        // for the sake of simplicity we are not taking into account
        // that this id might already be processing so we do not have to start another coroutine
        imageLookupTable[id]?.let { data ->
            GlobalScope.launch(context = Dispatchers.IO) {
                Log.d("OnStyleImageMissing", "Request image with id: $id")
                // we are executing the image rendering async since this can be quite a heavy process
                // and we need to stay off the UI thread for that
                val renderedImage =
                    data.render(context = applicationContext, imageLoader = imageLoader)

                withContext(context = Dispatchers.Main) {
                    Log.d("OnStyleImageMissing", "Loaded image with id: $id")
                    // once the image is rendered, we need to add it to the map
                    currentStyle?.addImage(id, renderedImage)
                }
            }
        } ?: throw IllegalArgumentException("Could not load image with id: $id")
    }
}

/**
 * Some custom dynamic data object
 */
data class DomainData(
    val longitude: Double,
    val latitude: Double,
    val text: String,
    val otherText: String,
    @DrawableRes
    val localIcon: Int,
    val urlToDownload: String
) {
    val identifier
        get() = "$longitude"
}
