package com.kellinreaver.rubricislam.ui.qiblat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kellinreaver.rubricislam.domain.usecase.GetLocationUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetQiblatDirectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QiblatViewModel @Inject constructor(
    private val getQiblatDirectionUseCase: GetQiblatDirectionUseCase,
    private val getLocationUseCase: GetLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiblatUiState())
    val uiState: StateFlow<QiblatUiState> = _uiState.asStateFlow()

    init {
        loadQiblatDirection()
    }

    private fun loadQiblatDirection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getLocationUseCase().collectLatest { location ->
                getQiblatDirectionUseCase(location.latitude, location.longitude).collectLatest { qiblat ->
                    _uiState.value = _uiState.value.copy(
                        direction = qiblat.bearing,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
}
