package me.darthwithap.android.unitconverterapp.presentation.conversion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.darthwithap.android.unitconverterapp.domain.models.SingleUnit
import me.darthwithap.android.unitconverterapp.domain.preferences.Preferences
import me.darthwithap.android.unitconverterapp.domain.usecases.ConversionUseCases
import me.darthwithap.android.unitconverterapp.presentation.conversion.components.UiCollection
import me.darthwithap.android.unitconverterapp.util.ConversionResult
import javax.inject.Inject

@HiltViewModel
class ConversionViewModel @Inject constructor(
    private val conversions: ConversionUseCases,
    prefs: Preferences
) : ViewModel() {
  var state by mutableStateOf(ConversionState())
    private set
  
  private var allUnits: List<SingleUnit> = emptyList()
  private var convertJob: Job? = null
  
  init {
    loadCollections()
    loadConversions()
    loadConversionUnits()
  }
  
  fun onEvent(event: ConversionEvent) {
    when (event) {
      ConversionEvent.ChoosingCollection -> {
        state = state.copy(
            isChoosingCollection = true
        )
      }
      
      ConversionEvent.ChoosingFromUnit -> {
        state = state.copy(
            isChoosingFromUnit = true
        )
      }
      
      ConversionEvent.ChoosingToUnit -> {
        state = state.copy(
            isChoosingToUnit = true
        )
      }
      
      is ConversionEvent.ChosenCollection -> {
        state = state.copy(
            currentCollection = UiCollection.byCollection(event.collection),
            fromUnit = event.collection.units.first(),
            toUnit = event.collection.units.last(),
            isChoosingCollection = false
        )
        convert()
      }
      
      is ConversionEvent.ChosenConversion -> {
        state = state.copy(
            fromUnit = event.conversion.fromUnit,
            toUnit = event.conversion.toUnit,
            inputValue = event.conversion.inputValue.toString(),
            outputValue = event.conversion.outputValue.toString(),
            currentCollection = state.collections.find {
              it.collection.name == event.conversion.collectionName
            }
        )
      }
      
      is ConversionEvent.ChosenConversionUnits -> {
        state = state.copy(
            fromUnit = event.units.fromUnit,
            toUnit = event.units.toUnit,
            currentCollection = state.collections.find {
              it.collection.name == event.units.collection
            }
        )
      }
      
      is ConversionEvent.ChosenFromUnit -> {
        state = state.copy(
            fromUnit = event.unit,
            isChoosingFromUnit = false
        )
        convert()
      }
      
      is ConversionEvent.ChosenToUnit -> {
        state = state.copy(
            toUnit = event.unit,
            isChoosingToUnit = false
        )
        convert()
      }
      
      is ConversionEvent.Convert -> {
        convert()
      }
      
      is ConversionEvent.InputValueChanged -> {
        state = state.copy(
            inputValue = event.value
        )
        if (event.value.isBlank()) {
          onEvent(ConversionEvent.ResetConversion)
          return
        }
        convert()
      }
      
      ConversionEvent.StoppedChoosingCollection -> {
        state = state.copy(
            isChoosingCollection = false
        )
      }
      
      ConversionEvent.StoppedChoosingFromUnit -> {
        state = state.copy(
            isChoosingFromUnit = false
        )
      }
      
      ConversionEvent.StoppedChoosingToUnit -> {
        state = state.copy(
            isChoosingToUnit = false
        )
      }
      
      is ConversionEvent.ToggleFavouriteConversion -> {
        viewModelScope.launch {
          when (val result = conversions.toggleFavouriteConversion(event.conversion)) {
            is ConversionResult.Error -> {
              state = state.copy(error = result.error?.error)
            }
            
            is ConversionResult.Success -> {
              loadConversions()
            }
          }
        }
      }
      
      is ConversionEvent.ToggleFavouriteConversionUnits -> {
        viewModelScope.launch {
          when (val result = conversions.toggleFavouriteConversionUnits(event.units)) {
            is ConversionResult.Error -> {
              state = state.copy(error = result.error?.error)
            }
            
            is ConversionResult.Success -> {
              loadConversionUnits()
            }
          }
        }
      }
      
      is ConversionEvent.OnErrorSeen -> {
        state = state.copy(
            error = null
        )
      }
      
      is ConversionEvent.DeleteConversion -> {
        viewModelScope.launch {
          when (val result = conversions.deleteConversion(event.conversion)) {
            is ConversionResult.Error -> {
              state = state.copy(error = result.error?.error)
            }
            
            is ConversionResult.Success -> {
              loadConversions()
            }
          }
        }
      }
      
      is ConversionEvent.DeleteConversionUnits -> {
        viewModelScope.launch {
          when (val result = conversions.deleteConversionUnits(event.units)) {
            is ConversionResult.Error -> {
              state = state.copy(error = result.error?.error)
            }
            
            is ConversionResult.Success -> {
              loadConversionUnits()
            }
          }
        }
      }
      
      ConversionEvent.ResetConversion -> {
        state = state.copy(
            inputValue = "",
            outputValue = null
        )
      }
      
      ConversionEvent.ToggleBatchConversion -> {
        state = state.copy(
            isBatchConversion = !state.isBatchConversion
        )
      }
    }
  }
  
  private fun loadCollections() {
    viewModelScope.launch {
      handleUseCaseResult(conversions.collections(), onError = {}) { collections ->
        val uiCollections = collections.map { UiCollection.byCollection(it) }
        state = state.copy(
            collections = uiCollections,
            currentCollection = uiCollections.first(),
            fromUnit = collections.first().units.first(),
            toUnit = collections.first().units.last()
        )
        allUnits = collections.flatMap { it.units }
      }
    }
  }
  
  private fun loadConversions() {
    viewModelScope.launch {
      handleUseCaseResult(conversions.getRecentConversions(), onError = {}) {
        state = state.copy(
            recentConversions = it
        )
      }
      handleUseCaseResult(conversions.getFavouriteConversions(), onError = {}) {
        state = state.copy(
            favouriteConversions = it
        )
      }
    }
  }
  
  private fun loadConversionUnits() {
    viewModelScope.launch {
      handleUseCaseResult(conversions.getRecentConversionUnits(), onError = {}) {
        state = state.copy(
            recentConversionUnits = it
        )
      }
      handleUseCaseResult(conversions.getFavouriteConversionUnits(), onError = {}) {
        state = state.copy(
            favouriteConversionUnits = it
        )
      }
    }
  }
  
  private fun convert() {
    if (state.isConverting || state.inputValue.isBlank()) {
      return
    }
    state = state.copy(isConverting = true)
    val fromUnit = state.fromUnit ?: allUnits.first()
    val toUnit = state.toUnit ?: allUnits.first()
    convertJob = viewModelScope.launch {
      state = state.copy(isConverting = true)
      handleUseCaseResult(conversions.convert(
          state.inputValue,
          fromUnit,
          toUnit
      ), onError = {
        state = state.copy(isConverting = false)
      }
      ) {
        // Not consuming the generatedId from room to see if we have to retry caching
        state = state.copy(
            isConverting = false,
            outputValue = it.outputValue
        )
        addConversionUnits(fromUnit, toUnit)
      }
    }
  }
  
  private suspend fun addConversionUnits(fromUnit: SingleUnit, toUnit: SingleUnit) {
    when (val addUnitsResult = conversions.addConversionUnits(fromUnit, toUnit)) {
      is ConversionResult.Error -> {
        state = state.copy(error = addUnitsResult.error?.error)
      }
      
      is ConversionResult.Success -> {
        loadConversionUnits()
      }
    }
  }
  
  private suspend fun <T> handleUseCaseResult(
      useCaseFlow: Flow<ConversionResult<T>>,
      onError: () -> Unit,
      onSuccess: suspend (T) -> Unit
  ) {
    useCaseFlow.collect { result ->
      when (result) {
        
        is ConversionResult.Success -> {
          onSuccess(result.data!!)
        }
        
        is ConversionResult.Error -> {
          onError()
          state = state.copy(error = result.error?.error)
        }
      }
    }
  }
}