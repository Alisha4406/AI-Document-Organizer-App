package com.example.aidocumentorganizer.Ui.ocr

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aidocumentorganizer.ocr.TextRecognitionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OcrViewModel : ViewModel() {

    private val textRecognitionManager = TextRecognitionManager()

    private val _extractedText = MutableStateFlow("")
    val extractedText: StateFlow<String> = _extractedText

    private val _warning = MutableStateFlow<String?>(null)
    val warning: StateFlow<String?> = _warning

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun recognizeText(
        context: Context,
        imageUri: Uri
    ) {
        _isProcessing.value = true
        _error.value = null
        _warning.value = null

        viewModelScope.launch {
            try {
                val outcome = textRecognitionManager.recognize(context, imageUri)
                _extractedText.value = outcome.text
                _warning.value = outcome.warning
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _error.value = exception.message ?: "OCR failed"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    override fun onCleared() {
        textRecognitionManager.close()
        super.onCleared()
    }
}
