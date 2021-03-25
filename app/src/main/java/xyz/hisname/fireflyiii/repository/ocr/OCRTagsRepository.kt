package xyz.hisname.fireflyiii.repository.ocr

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import xyz.hisname.fireflyiii.data.local.dao.OCRTagDao
import xyz.hisname.fireflyiii.repository.models.ocr.OCRTag

@Suppress("RedundantSuspendModifier")
@WorkerThread
class OCRTagsRepository(private val ocrTagDao: OCRTagDao) {

    suspend fun allOCRTags(): LiveData<MutableList<OCRTag>> {
        return ocrTagDao.getAllOCRTags()
    }

    suspend fun insertAmountTag(valueTag: String) {
        ocrTagDao.insert(OCRTag(OCRTag.AMOUNT_FIELD, valueTag))
    }

    suspend fun insertDescriptionTag(valueTag: String) {
        ocrTagDao.insert(OCRTag(OCRTag.DESCRIPTION_FIELD, valueTag))
    }

    suspend fun deleteTagByValue(valueTag: String) {
        ocrTagDao.deleteTagByValue(valueTag)
    }

    fun filterDescriptionTags(tags: MutableList<OCRTag>) : MutableList<OCRTag> {
        return tags.filter { it.field_name.equals(OCRTag.DESCRIPTION_FIELD) } as MutableList<OCRTag>
    }

    fun filterAmountTags(tags: MutableList<OCRTag>) : MutableList<OCRTag> {
        return tags.filter { it.field_name.equals(OCRTag.AMOUNT_FIELD) } as MutableList<OCRTag>
    }
}