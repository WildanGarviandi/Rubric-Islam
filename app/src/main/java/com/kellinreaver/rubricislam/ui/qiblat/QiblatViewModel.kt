package com.kellinreaver.rubricislam.ui.qiblat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kellinreaver.rubricislam.domain.usecase.GetDirectionForQiblatUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetLocationUseCase
import com.kellinreaver.rubricislam.domain.usecase.GetQiblatDirectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class QiblatViewModel
@Inject
constructor(
    private val getQiblatDirectionUseCase: GetQiblatDirectionUseCase,
    private val getLocationUseCase: GetLocationUseCase,
    private val getDirectionForQiblatUseCase: GetDirectionForQiblatUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(QiblatUiState())
    val uiState: StateFlow<QiblatUiState> = _uiState.asStateFlow()

    init {
        loadQiblatDirection()
        observeDeviceHeading()
    }

    private fun observeDeviceHeading() {
        viewModelScope.launch {
            getDirectionForQiblatUseCase.invoke()
            getDirectionForQiblatUseCase.deviceHeadingStateFlow.collectLatest { heading ->
                _uiState.value = _uiState.value.copy(deviceHeading = heading)
            }
        }
    }

    private fun loadQiblatDirection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getLocationUseCase().collectLatest { location ->
                getQiblatDirectionUseCase(
                    location.latitude,
                    location.longitude
                ).collectLatest { qiblat ->
                    _uiState.value =
                        _uiState.value.copy(
                            direction = qiblat.bearing,
                            isLoading = false,
                            error = null
                        )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getDirectionForQiblatUseCase.stopListening()
    }
}
