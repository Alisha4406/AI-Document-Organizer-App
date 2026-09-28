package com.example.aidocumentorganizer.ui.home


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.aidocumentorganizer.data.repository.DocumentRepository

class HomeViewModelFactory(
    private val repository: DocumentRepository,
    private val userId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                repository = repository,
                userId = userId
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}