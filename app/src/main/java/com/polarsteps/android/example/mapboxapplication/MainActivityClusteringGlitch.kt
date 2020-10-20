package com.polarsteps.android.example.mapboxapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils

class MainActivityClusteringGlitch : AppCompatActivity(), MapView.OnStyleImageMissingListener {

    private var currentStyle: Style? = null

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
        val openRawResource = resources.assets.open("style_clusters.json")
            .bufferedReader()
            .readText()

        map.setStyle(
            Style.Builder()
                .fromJson(openRawResource)
        ) {
            this.currentStyle = it
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 4.0), 0.0))
            map.easeCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 180.0), 0.0), 20000, false)
            initWithStyle(it)
        }

    }

    private fun initWithStyle(style: Style) {
        // add static images
        style.addImage(
            "clustered",
            BitmapUtils.getDrawableFromRes(this, R.drawable.mapbox_compass_icon)!!
        )
        style.addImage(
            "unclustered",
            BitmapUtils.getDrawableFromRes(this, R.drawable.mapbox_marker_icon_default)!!
        )
        updateContent(style)
    }

    /**
     * This is the main function where the data for the map is updated
     * ( can be called multiple times based on when data changes )
     *
     * for the sake of this example it's only called once
     */
    private fun updateContent(style: Style) {
        // generate some test data
        val featureCollection = FeatureCollection.fromFeatures(
            generateRandomListOfComplexData().map { data ->
                Feature.fromGeometry(
                    Point.fromLngLat(data.longitude, data.latitude),
                    JsonObject()
                )
            })

        var source = style.getSourceAs<GeoJsonSource>("custom")
        if (source == null) {
            source = GeoJsonSource("custom", featureCollection, GeoJsonOptions().apply {
                this.withCluster(true)
                this.withClusterRadius(20)
            })
            style.addSource(source)
        } else {
            source.setGeoJson(featureCollection)
        }

    }

    /**
     * Just some random complex data
     */
    private fun generateRandomListOfComplexData(): List<ClusteredData> {
        return (-180).until(180).step(10).map { longitude ->
            return@map ClusteredData(
                longitude = longitude.toDouble(),
                latitude = 0.0
            )
        }
    }

    override fun onStyleImageMissing(id: String) {
        // noop in this example
    }
}

data class ClusteredData(
    val longitude: Double,
    val latitude: Double
)
