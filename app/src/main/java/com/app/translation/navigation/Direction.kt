package com.app.translation.navigation

import com.google.android.gms.maps.model.*

class Direction(
    val error_message: String?,
    val routes: List<Route>,
    val status: String
) {

    class Route(
        val overview_polyline: Polyline,
        val legs: List<Leg>
    )

    class Leg(
        val distance: Content,
        val duration: Content,
        val start_address: String,
        val start_location: Location,
        val end_address: String,
        val end_location: Location,
        val steps: ArrayList<Step>,
    )

    class Step(
        val distance: Content,
        val duration: Content,
        val start_location: Location,
        val end_location: Location,
        val polyline: Polyline,
        val travel_mode: String,
        val html_instructions: String
    )

    class Content(
        val text: String,
        val value: Int
    )

    class Polyline(
        val points: String
    )

    class Location(var lat: Double, var lng: Double) {

        fun location() = LatLng(lat, lng)
        override fun toString() = "$lat,$lng"
    }

    companion object {
        const val MODE_CAR = "driving"
        const val MODE_WALKING = "walking"

        const val NAVIGATION_FILE_NAME = "Navigation.mp3"
        const val ZOOM = 15f
        const val PATTERN_DASH_LENGTH_PX = 20f
        val DOT = Dot()
        val GAP = Gap(PATTERN_DASH_LENGTH_PX)
    }
}