package com.app.translation.translate

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.translation.removeHtml
import com.google.cloud.translate.Translate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class TranslateViewModel : ViewModel() {
    val translateMessage = MutableLiveData<TranslateMessage>()
    val errorMessage = MutableLiveData<String>()
    private var output: String? = null
    private var china: String? = null
    private var japan: String? = null
    private var error: String? = null

    private fun checkDone(input: String, source: Locale, target: Locale) {
        if (output != null && china != null && japan != null) {
            if (error != null) {
                errorMessage.value = error!!
            } else {
                translateMessage.value = TranslateMessage(
                    input,
                    output!!.removeHtml(),
                    china!!.removeHtml(),
                    japan!!.removeHtml(),
                    source,
                    target
                )
            }
        }
    }

    fun translate(service: Translate, input: String, source: Locale, target: Locale) {
        output = null
        china = null
        japan = null
        error = null
        viewModelScope.launch {
            output = try {
                withContext(Dispatchers.IO) {
                    service.translate(
                        input,
                        Translate.TranslateOption.sourceLanguage(source.language),
                        Translate.TranslateOption.targetLanguage(target.language)
                    ).translatedText
                }
            } catch (e: Exception) {
                error = e.message
                ""
            }
            checkDone(input, source, target)
        }
        viewModelScope.launch {
            china = try {
                withContext(Dispatchers.IO) {
                    service.translate(
                        input,
                        Translate.TranslateOption.sourceLanguage(source.language),
                        Translate.TranslateOption.targetLanguage(Locale.CHINA.language)
                    ).translatedText
                }
            } catch (e: Exception) {
                error = e.message
                ""
            }
            checkDone(input, source, target)
        }
        viewModelScope.launch {
            japan = try {
                withContext(Dispatchers.IO) {
                    service.translate(
                        input,
                        Translate.TranslateOption.sourceLanguage(source.language),
                        Translate.TranslateOption.targetLanguage(Locale.JAPAN.language)
                    ).translatedText
                }
            } catch (e: Exception) {
                error = e.message
                ""
            }
            checkDone(input, source, target)
        }
    }
}