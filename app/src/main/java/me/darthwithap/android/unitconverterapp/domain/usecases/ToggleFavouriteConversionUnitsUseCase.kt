package me.darthwithap.android.unitconverterapp.domain.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.darthwithap.android.unitconverterapp.domain.models.ConversionUnits
import me.darthwithap.android.unitconverterapp.domain.repository.ConverterRepository
import me.darthwithap.android.unitconverterapp.util.ConversionException
import me.darthwithap.android.unitconverterapp.util.ConversionResult

class ToggleFavouriteConversionUnitsUseCase(
    private val repository: ConverterRepository
) {
  suspend operator fun invoke(
      units: ConversionUnits
  ): ConversionResult<Long> {
    return try {
      withContext(Dispatchers.IO) {
        val numberOfRowsUpdated = repository.updateConversionUnits(
            units.copy(isFavourite = !units.isFavourite)
        )
        ConversionResult.Success(numberOfRowsUpdated)
      }
    } catch (e: ConversionException) {
      e.printStackTrace()
      ConversionResult.Error(e)
    }
  }
}