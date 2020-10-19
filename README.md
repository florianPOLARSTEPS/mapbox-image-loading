# mapbox-issues
- The first project `MainActivityAsyncImageLoading.kt` shows a common use-case for asynchronous image-generation with mapbox.
- The second project `MainActivityClusteringGlitch.kt` demos a glitch we experience when panning clustered Geojson sources

## Building
Please rename `api_keys.sample.properties` into `api_keys.properties` and fill in the relevant tokens.

## The problem: async-image-loading

- Start up the demo application.`MainActivityAsyncImageLoading.kt`
- The app will render a map and will immediately add some data points in a horizontal line along the equator.
- Those data points will have dynamic image identifiers the Mapbox renderer will not be able to find in the style.
- The [`onStyleImageMissing`](https://github.com/florianPOLARSTEPS/mapbox-image-loading/blob/27ddaaae6ce989e1684a3a25c3863c3eb1477a13/app/src/main/java/com/polarsteps/android/example/mapboxapplication/MainActivity.kt#L122) callback is invoked with those identifiers.
- Since we need to do some heavy computation + network requests in order to render those images, we offload the rendering of the `Bitmaps` to a background thread. 
- We also do not initially know the size of those icons.
- After the computation completes, we want to add the image to the map via `addImage`
- Here we encounter the problem: The images are added to the map successfully, but they are not rendered until a layout pass happens for some other reason (pinch gesture)

## The problem: clustering
- Start up the demo application. `MainActivityClusteringGlitch.kt`
- Observe the fading clusters when crossing the screen bounds
