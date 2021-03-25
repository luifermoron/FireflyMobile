package xyz.hisname.fireflyiii.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import xyz.hisname.fireflyiii.repository.models.ocr.OCRTag

@Dao
abstract class OCRTagDao: BaseDao<OCRTag> {
    @Query("SELECT * FROM OCRTag")
    abstract fun getAllOCRTags(): LiveData<MutableList<OCRTag>>

    @Query("DELETE FROM OCRTag WHERE value_tag = :valueTag")
    abstract fun deleteTagByValue(valueTag: String): Int
}