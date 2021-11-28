package com.app.translation.millionaire

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.translation.LocaleVN
import com.app.translation.removeHtml
import com.google.cloud.translate.Translate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MillionaireViewModel:ViewModel() {
    private lateinit var service: Translate
    private val source = Locale.ENGLISH.language
    private val target = LocaleVN.language
    private var error: String? = null
    private val questionTranslated = MillionaireActivity.QuestionTranslated()
    val isTranslated = MutableLiveData<MillionaireActivity.QuestionTranslated>()
    val errorMessage = MutableLiveData<String>()

    fun init(translate: Translate) {
        service = translate
    }

    fun translate(question: MillionaireActivity.Question) {
        error = null
        questionTranslated.question = null
        questionTranslated.a = null
        questionTranslated.b = null
        questionTranslated.c = null
        questionTranslated.d = null
        viewModelScope.launch {
            questionTranslated.question = try {
                withContext(Dispatchers.IO) {
                    service.translate(
                        question.question,
                        Translate.TranslateOption.sourceLanguage(source),
                        Translate.TranslateOption.targetLanguage(target)
                    ).translatedText.removeHtml()
                }
            } catch (e: Exception) {
                error = e.message
                ""
            }
            checkDone()
        }
        viewModelScope.launch {
            questionTranslated.a = try {
                withContext(Dispatchers.IO) {
                    service.translate(
                        question.answers[0],
                        Translate.TranslateOption.sourceLanguage(source),
                        Translate.TranslateOption.targetLanguage(target)
                    ).translatedText.removeHtml()
                }
            } catch (e: Exception) {
                error = e.message
                ""
            }
            checkDone()
        }
        viewModelScope.launch {
            questionTranslated.b = try {
                withContext(Dispatchers.IO) {
                    service.translate(
                        question.answers[1],
                        Translate.TranslateOption.sourceLanguage(source),
                        Translate.TranslateOption.targetLanguage(target)
                    ).translatedText.removeHtml()
                }
            } catch (e: Exception) {
                error = e.message
                ""
            }
            checkDone()
        }
        viewModelScope.launch {
            questionTranslated.c = try {
                withContext(Dispatchers.IO) {
                    service.translate(
                        question.answers[2],
                        Translate.TranslateOption.sourceLanguage(source),
                        Translate.TranslateOption.targetLanguage(target)
                    ).translatedText.removeHtml()
                }
            } catch (e: Exception) {
                error = e.message
                ""
            }
            checkDone()
        }
        viewModelScope.launch {
            questionTranslated.d = try {
                withContext(Dispatchers.IO) {
                    service.translate(
                        question.answers[3],
                        Translate.TranslateOption.sourceLanguage(source),
                        Translate.TranslateOption.targetLanguage(target)
                    ).translatedText.removeHtml()
                }
            } catch (e: Exception) {
                error = e.message
                ""
            }
            checkDone()
        }
    }

    private fun checkDone() {
        if (questionTranslated.question != null && questionTranslated.a != null && questionTranslated.b != null && questionTranslated.c != null && questionTranslated.d != null) {
            if (error != null) {
                errorMessage.value = error!!
            } else {
                isTranslated.value = questionTranslated
            }
        }
    }
}