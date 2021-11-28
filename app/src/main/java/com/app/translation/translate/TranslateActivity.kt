package com.app.translation.translate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.app.translation.*
import com.app.translation.databinding.ActivityTranslateBinding
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import java.util.*

class TranslateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTranslateBinding
    private lateinit var service: Translate
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var textToSpeech: TextToSpeech
    private var locale = Pair(LocaleVN, Locale.ENGLISH)
    private val viewModel: TranslateViewModel by viewModels()
    private val adapter = TranslateRcvAdapter { message, locale ->
        textToSpeech.language = locale
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            speechRecognizer.startListening(speechIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranslateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            rcvMessage.adapter = adapter
            imvSpeech.setOnClickListener {
                if (ActivityCompat.checkSelfPermission(this@TranslateActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    speechRecognizer.startListening(speechIntent)
                } else {
                    requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            btnTranslate.setOnClickListener {
                startTranslate(edtInput.text.toString())
                binding.edtInput.text = null
            }
            btnLanguage.setOnClickListener {
                locale = Pair(locale.second, locale.first)
                btnLanguage.text = locale.first.language
            }
        }

        viewModel.translateMessage.observe(this) {
            val lastIndex = adapter.add(it)
            binding.rcvMessage.scrollToPosition(lastIndex)
        }
        viewModel.errorMessage.observe(this) {
            toast(it)
        }

        service = TranslateOptions.newBuilder()
            .setCredentials(GoogleCredentials.fromStream(assets.open("service_account.json")))
            .build()
            .service
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : MyRecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                binding.btnTranslate.isEnabled = false
                binding.btnTranslate.text = getString(R.string.listening)
            }

            override fun onError(p0: Int) {
                binding.btnTranslate.isEnabled = true
                binding.btnTranslate.text = getString(R.string.translate)
                toast(p0.toStringError())
            }

            override fun onResults(p0: Bundle?) {
                binding.btnTranslate.text = getString(R.string.translate)
                binding.btnTranslate.isEnabled = true
                p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    startTranslate(it)
                }
            }
        })
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.first.language)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        textToSpeech = TextToSpeech(this) {
            if (it != TextToSpeech.SUCCESS) {
                toast("Error while initializing TextToSpeech engine!")
            }
        }
    }

    private fun startTranslate(input: String) {
        viewModel.translate(service, input, locale.first, locale.second)
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}