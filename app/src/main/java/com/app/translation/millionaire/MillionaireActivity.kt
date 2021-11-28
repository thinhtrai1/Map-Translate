package com.app.translation.millionaire

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.app.translation.*
import com.app.translation.databinding.ActivityMillionaireBinding
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.TranslateOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class MillionaireActivity : AppCompatActivity() {
    private lateinit var questions: List<QuestionSet>
    private lateinit var binding: ActivityMillionaireBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent
    private lateinit var textToSpeech: TextToSpeech
    private val viewModel: MillionaireViewModel by viewModels()
    private var currentSet: QuestionSet? = null
    private var currentNumber = 0
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            speechRecognizer.startListening(speechIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMillionaireBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val json = try { assets.open("questions.json").bufferedReader().use { it.readText() } } catch (e: IOException) { return }
        questions = Gson().fromJson(json, object : TypeToken<List<QuestionSet>>() {}.type)
        binding.apply {
            btnAnswer.setOnClickListener {
                if (currentSet == null) {
                    loadQuestion()
                } else {
                    if (ActivityCompat.checkSelfPermission(this@MillionaireActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        textToSpeech.stop()
                        speechRecognizer.startListening(speechIntent)
                    } else {
                        requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        }
        viewModel.init(
            TranslateOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(assets.open("service_account.json")))
                .build()
                .service
        )
        viewModel.isTranslated.observe(this) {
            binding.apply {
                val question = "Câu " + (currentNumber + 1) + "\n" + it.question
                tvQuestion.text = question
                btnA.text = it.a
                btnB.text = it.b
                btnC.text = it.c
                btnD.text = it.d
                textToSpeech.speak(
                    question + "\n" + it.a + "\n" + it.b + "\n" + it.c + "\n" + it.d,
                    TextToSpeech.QUEUE_FLUSH, null, null
                )
                btnAnswer.text = getString(R.string.answer)
                btnAnswer.isEnabled = true
            }
        }
        viewModel.errorMessage.observe(this) {
            textToSpeech.speak(
                "Đã có lỗi xảy ra. $it",
                TextToSpeech.QUEUE_FLUSH, null, null
            )
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : MyRecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {
                binding.btnAnswer.isEnabled = false
                binding.btnAnswer.text = getString(R.string.listening)
            }

            override fun onError(p0: Int) {
                binding.btnAnswer.isEnabled = true
                binding.btnAnswer.text = getString(R.string.answer)
                if (p0 == SpeechRecognizer.ERROR_NO_MATCH) {
                    textToSpeech.speak("Xin bạn nói rõ hơn", TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    textToSpeech.speak("Đã có lỗi xảy ra. " + p0.toStringError(), TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }

            override fun onResults(p0: Bundle) {
                val results = p0.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.map { it.lowercase() }
                if (results.isNullOrEmpty()) {
                    textToSpeech.speak("Xin bạn nói rõ hơn", TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    binding.tvAnswer.text = results.first()
                    val answer = when {
                        results.contains("a") || results.contains("phương án đầu tiên") || results.contains("đáp án đầu tiên")
                                || results.contains("đáp án a") || results.contains(viewModel.isTranslated.value!!.a!!.lowercase()) -> 0
                        results.contains("b") || results.contains(viewModel.isTranslated.value!!.b!!.lowercase()) -> 1
                        results.contains("c") || results.contains(viewModel.isTranslated.value!!.c!!.lowercase()) -> 2
                        results.contains("d") || results.contains(viewModel.isTranslated.value!!.d!!.lowercase()) -> 3
                        else -> {
                            val (correct, text) = when (currentSet!!.questions[currentNumber].correct) {
                                0 -> Pair("a", viewModel.isTranslated.value!!.a)
                                1 -> Pair("b", viewModel.isTranslated.value!!.b)
                                2 -> Pair("c", viewModel.isTranslated.value!!.c)
                                else -> Pair("d", viewModel.isTranslated.value!!.d)
                            }
                            textToSpeech.speak("Không chính xác. Đáp án đúng phải là $correct $text", TextToSpeech.QUEUE_FLUSH, null, null)
                            Handler(Looper.getMainLooper()).postDelayed({
                                currentNumber = 0
                                loadQuestion()
                            }, 4000)
                            return
                        }
                    }
                    if(answer == currentSet!!.questions[currentNumber].correct) {
                        textToSpeech.speak("Chính xác", TextToSpeech.QUEUE_FLUSH, null, null)
                        Handler(Looper.getMainLooper()).postDelayed({
                            currentNumber++
                            loadQuestion()
                        }, 500)
                    } else {
                        val (correct, text) = when (currentSet!!.questions[currentNumber].correct) {
                            0 -> Pair("a", viewModel.isTranslated.value!!.a)
                            1 -> Pair("b", viewModel.isTranslated.value!!.b)
                            2 -> Pair("c", viewModel.isTranslated.value!!.c)
                            else -> Pair("d", viewModel.isTranslated.value!!.d)
                        }
                        textToSpeech.speak("Không chính xác. Đáp án đúng phải là $correct $text", TextToSpeech.QUEUE_FLUSH, null, null)
                        Handler(Looper.getMainLooper()).postDelayed({
                            currentNumber = 0
                            loadQuestion()
                        }, 4000)
                    }
                }
            }
        })
        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, LocaleVN.language)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        textToSpeech = TextToSpeech(this) {
            when (it) {
                TextToSpeech.SUCCESS -> {
                    textToSpeech.language = LocaleVN
                    textToSpeech.setSpeechRate(1.5f)
                }
                else -> toast("Error while initializing TextToSpeech engine!")
            }
        }
    }

    private fun loadQuestion() {
        currentSet = questions.random()
        viewModel.translate(currentSet!!.questions[currentNumber])
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    class QuestionSet(val questions: List<Question>)

    class Question(
        val question: String,
        val answers: List<String>,
        val correct: Int,
    )

    class QuestionTranslated {
        var question: String? = null
        var a: String? = null
        var b: String? = null
        var c: String? = null
        var d: String? = null
    }
}