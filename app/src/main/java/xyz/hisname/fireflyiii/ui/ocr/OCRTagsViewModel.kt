package xyz.hisname.fireflyiii.ui.ocr

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.data.local.dao.AppDatabase
import xyz.hisname.fireflyiii.repository.BaseViewModel
import xyz.hisname.fireflyiii.repository.models.ocr.OCRTag
import xyz.hisname.fireflyiii.repository.models.tags.TagsData
import xyz.hisname.fireflyiii.repository.ocr.OCRTagsRepository


class OCRTagsViewModel(application: Application): BaseViewModel(application) {

    private val ocrTagsRepository = OCRTagsRepository(
            AppDatabase.getInstance(application).ocrTagDataDao(),
    )

    suspend fun getAllOCRTags(): LiveData<MutableList<OCRTag>> {
        return ocrTagsRepository.allOCRTags()
    }

    fun isLoading() {
        isLoading.postValue(true)
    }

    fun loaded() {
        isLoading.postValue(false)
    }

    fun insertAmountTag(value: String) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            ocrTagsRepository.insertAmountTag(value)
            isLoading.postValue(false)
        }
    }

    fun insertDescriptionTag(value: String) {
        isLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            ocrTagsRepository.insertDescriptionTag(value)
            isLoading.postValue(false)
        }
    }

    suspend fun deleteTagByValue(tagValue: String) {
        return ocrTagsRepository.deleteTagByValue(tagValue)
    }

    fun filterDescriptionTags(tags: MutableList<OCRTag>) : MutableList<OCRTag> {
        return ocrTagsRepository.filterDescriptionTags(tags)
    }

    fun filterAmountTags(tags: MutableList<OCRTag>) : MutableList<OCRTag> {
        return ocrTagsRepository.filterAmountTags(tags)
    }

    fun descriptionSringList(tags: MutableList<OCRTag>) : List<String> {
        return filterDescriptionTags(tags).map { it.value_tag }
    }

    fun amountStringList(tags: MutableList<OCRTag>) : List<String> {
        return filterAmountTags(tags).map { it.value_tag }
    }
}