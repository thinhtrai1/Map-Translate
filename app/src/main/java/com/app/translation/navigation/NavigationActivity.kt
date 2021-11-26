package com.app.translation.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.app.translation.*
import com.app.translation.R
import com.app.translation.databinding.ActivityNavigationBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*

class NavigationActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityNavigationBinding
    private lateinit var mapView: SupportMapFragment
    private var googleMap: GoogleMap? = null
    private var textToSpeech: TextToSpeech? = null
    private val mediaPayer = NavigationMediaPlayer()
    private val location = Direction.Location(16.0, 108.0)
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            speechRecognizer.startListening(speechIntent)
        }
    }
    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            requestEnableGPS()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapView = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapView.getMapAsync(this)
        with(binding) {
            btnStart.setOnClickListener {
                getDirection(edtInput.text.toString())
            }
            imvSpeech.setOnClickListener {
                if (ActivityCompat.checkSelfPermission(this@NavigationActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    speechRecognizer.startListening(speechIntent)
                } else {
                    requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            imvPlay.setOnClickListener {
                if (mediaPayer.duration > 0) {
                    if (mediaPayer.isPlaying) {
                        mediaPayer.pause()
                    } else {
                        mediaPayer.start()
                    }
                }
            }
            mediaPayer.setupWithSeekBar(seekBar)
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : MyRecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                binding.btnStart.isEnabled = false
                binding.btnStart.text = getString(R.string.listening)
            }

            override fun onError(p0: Int) {
                binding.btnStart.isEnabled = true
                binding.btnStart.text = getString(R.string.start)
                toast(p0.toStringError())
            }

            override fun onResults(p0: Bundle?) {
                binding.btnStart.isEnabled = true
                binding.btnStart.text = getString(R.string.start)
                p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    getDirection(it)
                }
            }
        })
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, LocaleVN)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requestEnableGPS()
        } else {
            requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getDirection(input: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    directionService.getDirection(
                        location.toString(),
                        input,
                        Direction.MODE_CAR,
                        "false",
                        LocaleVN.language,
                        getString(R.string.map_api_key)
                    ).body()
                }?.let {
                    if (it.routes.isNotEmpty()) {
                        showData(input, it)
                    } else {
                        toast(it.error_message)
                    }
                }
            } catch (e: Exception) {
                toast(e.message)
            }
        }
    }

    private fun showData(input: String, direction: Direction) {
        binding.layoutPlaySpeech.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE
        binding.imvPlay.visibility = View.GONE
        binding.tvInformation.visibility = View.VISIBLE
        binding.tvInformation.text = direction.routes[0].legs[0].run {
            getString(
                R.string.navigation_description,
                start_address,
                end_address,
                distance.text,
                duration.text
            )
        }
        googleMap?.apply {
            clear()
            addMarker(
                MarkerOptions()
                    .position(direction.routes[0].legs[0].end_location.location())
                    .title(input)
            )?.showInfoWindow()
            addPolyline(
                PolylineOptions()
                    .geodesic(true)
                    .width(Direction.PATTERN_DASH_LENGTH_PX)
                    .color(Color.BLUE)
                    .pattern(listOf(Direction.GAP, Direction.DOT))
                    .addAll(PolyUtil.decode(direction.routes[0].overview_polyline.points))
            )
            animateCamera(
                CameraUpdateFactory.newLatLngZoom(direction.routes[0].legs[0].start_location.location(), Direction.ZOOM)
            )
        }
        textToSpeech?.stop()
        textToSpeech = TextToSpeech(this) { status ->
            when (status) {
                TextToSpeech.SUCCESS -> {
                    val navigation = direction.routes[0].legs[0].steps.joinToString("\nSau đó, ") { it.html_instructions.removeHtml() }
                    val file = File(getExternalFilesDir(null), Direction.NAVIGATION_FILE_NAME).apply {
                        if (exists()) {
                            delete()
                        }
                        createNewFile()
                    }
                    textToSpeech!!.language = LocaleVN
                    textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onDone(p0: String?) {
                            mediaPayer.reset()
                            mediaPayer.setDataSource(file.path)
                            mediaPayer.prepare()
                            mediaPayer.start()
                            lifecycleScope.launch {
                                binding.progressBar.visibility = View.GONE
                                binding.imvPlay.visibility = View.VISIBLE
                            }
                        }

                        override fun onError(utteranceId: String?) {
                            toast("Some Error Occurred")
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            super.onError(utteranceId, errorCode)
                            toast("Some Error Occurred $errorCode")
                        }

                        override fun onStart(p0: String?) {
                        }
                    })
                    textToSpeech!!.synthesizeToFile(navigation, null, file, (0..1000).random().toString())
                }
                else -> toast("Error while initializing TextToSpeech engine!")
            }
        }
    }

    private val requestEnableGPSForResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            getLocation()
        }
    }
    private fun requestEnableGPS() {
        val requestEnable = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(10000))
            .build()
        LocationServices.getSettingsClient(applicationContext)
            .checkLocationSettings(requestEnable)
            .addOnCompleteListener { task ->
                try {
                    task.getResult(ApiException::class.java)
                    getLocation()
                } catch (exception: ApiException) {
                    if (exception.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        try {
                            requestEnableGPSForResult.launch(
                                IntentSenderRequest.Builder((exception as ResolvableApiException).resolution).build()
                            )
                        } catch (e: Exception) {
                        }
                    }
                }
            }
    }

    private var locationProviderClient: FusedLocationProviderClient? = null
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation.let {
                location.lat = it.latitude
                location.lng = it.longitude
            }
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(location.lat, location.lng), Direction.ZOOM)
            )
            locationProviderClient!!.removeLocationUpdates(this)
        }
    }

    private fun getLocation() {
        val requestLocation = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(0)
        // or for real time
//                            .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
//                            .setFastestInterval(5000)
//                            .setInterval(5000)
        locationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        locationProviderClient!!.requestLocationUpdates(requestLocation, locationCallback, Looper.getMainLooper())
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        googleMap!!.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(location.lat, location.lng), Direction.ZOOM)
        )
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        mediaPayer.stop()
        mediaPayer.release()
        locationProviderClient?.removeLocationUpdates(locationCallback)
    }

    companion object {
        private val directionService = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/directions/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DirectionService::class.java)
    }
}