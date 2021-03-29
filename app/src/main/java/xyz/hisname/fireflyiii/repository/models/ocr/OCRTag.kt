package xyz.hisname.fireflyiii.repository.models.ocr

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "OCRTag")
data class OCRTag(val field_name: String,
                  val value_tag: String) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    companion object {
        @JvmStatic val AMOUNT_FIELD = "amount"
        @JvmStatic val DESCRIPTION_FIELD = "description"
    }
}