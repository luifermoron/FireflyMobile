package xyz.hisname.fireflyiii.ui.ocr

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.utils.sizeDp
import io.github.subhamtyagi.ocr.OCRSettings
import kotlinx.coroutines.*
import xyz.hisname.fireflyiii.R
import xyz.hisname.fireflyiii.databinding.FragmentBaseListBinding
import xyz.hisname.fireflyiii.databinding.FragmentOcrBinding
import xyz.hisname.fireflyiii.repository.models.ocr.OCRTag
import xyz.hisname.fireflyiii.ui.base.BaseFragment
import xyz.hisname.fireflyiii.ui.base.TextInputAutoCompleteTextView
import xyz.hisname.fireflyiii.ui.tags.AddTagsViewModel
import xyz.hisname.fireflyiii.util.extension.*
import xyz.hisname.fireflyiii.util.extension.getImprovedViewModel


class OcrFragment: BaseFragment() {
    private val tagViewModel by lazy { getImprovedViewModel(OCRTagsViewModel::class.java) }
    private var fragmentListTagsBinding: FragmentOcrBinding? = null
    private val binding get() = fragmentListTagsBinding!!
    private var fragmentBaseBinding: FragmentBaseListBinding? = null
    private val baseListBinding get() = fragmentBaseBinding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentListTagsBinding = FragmentOcrBinding.inflate(inflater, container, false)
        fragmentBaseBinding = binding.tagsBaseList
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        baseListBinding.baseSwipeLayout.swipeContainer.isGone = true
        binding.allDescriptionTags.setChipSpacing(16)
        binding.allAmountTags.setChipSpacing(16)
        setResponse()
        displayView()
    }

    private fun setResponse(){
        tagViewModel.apiResponse.observe(viewLifecycleOwner) {
            toastError(it)
        }

        binding.descriptionEdittext.setOnEditorActionListener(TextView.OnEditorActionListener
        {  v: TextView, actionId: Int,  event: KeyEvent ->
            var handled : Boolean = false
            if (wasEnterPressed(actionId, event)) {
                val description: String = popText(binding.descriptionEdittext)
                tagViewModel.insertDescriptionTag(description)

                handled = true
            }
            handled
        })

        binding.amountEdittext.setOnEditorActionListener(TextView.OnEditorActionListener
        {  v: TextView, actionId: Int,  event: KeyEvent ->
            var handled : Boolean = false
            if (wasEnterPressed(actionId, event)) {
                val amount: String = popText(binding.amountEdittext)
                tagViewModel.insertAmountTag(amount)

                handled = true
            }
            handled
        })
    }

    private fun popText(textView: TextInputAutoCompleteTextView) : String {
        val textContent: String = textView.getString()
        textView.setText("")
        return textContent
    }

    private fun wasEnterPressed(actionId: Int,  event: KeyEvent) : Boolean {
        return (actionId == EditorInfo.IME_ACTION_DONE
                || event.action == KeyEvent.ACTION_DOWN
                || event.action == KeyEvent.KEYCODE_ENTER)
    }

    private fun displayView() {
        tagViewModel.getAllOCRTags().observe(viewLifecycleOwner) { tags ->
            tagViewModel.loaded()

            binding.allDescriptionTags.removeAllViewsInLayout()
            binding.allAmountTags.removeAllViewsInLayout()
            if(tags.isEmpty()){
                baseListBinding.listImage.isVisible = true
                baseListBinding.listText.isVisible = true
                baseListBinding.listImage.setImageDrawable(IconicsDrawable(requireContext()).apply {
                    icon = FontAwesome.Icon.faw_tag
                    sizeDp = 24
                })
                baseListBinding.listText.text = "No Tags Found! Start tagging now?"
            } else {
                baseListBinding.listImage.isGone = true
                baseListBinding.listText.isGone = true

                replaceListOn(tagViewModel.filterDescriptionTags(tags), binding.allDescriptionTags)
                replaceListOn(tagViewModel.filterAmountTags(tags), binding.allAmountTags)
            }
        }

    }

    private fun replaceListOn(tags: MutableList<OCRTag>, chipGroup: ChipGroup){
        tags.forEach { tagsData ->
            val chipTags = Chip(requireContext(), null, R.attr.chipStyle)
            chipTags.apply {
                text = tagsData.value_tag
                isCloseIconVisible = true
                setOnCloseIconClickListener { close ->
                    val tagName = (close as TextView).text.toString()
                    deleteTag(tagName, chipTags)
                }
            }
            chipGroup.addView(chipTags)
        }
    }

    private fun deleteTag(tagName: String, chip: Chip){
        AlertDialog.Builder(requireContext())
                .setTitle(resources.getString(R.string.delete_tag_title, tagName))
                .setMessage(resources.getString(R.string.delete_tag_message, tagName))
                .setPositiveButton(R.string.delete_permanently){ _, _ ->
                    GlobalScope.launch {
                        tagViewModel.deleteTagByValue(tagName)
                    }
                }
                .setNegativeButton("No"){ _, _ ->
                    toastInfo("Tag not deleted")
                }
                .show()
    }

    override fun onAttach(context: Context){
        super.onAttach(context)
        requireActivity().findViewById<Toolbar>(R.id.activity_toolbar).title = resources.getString(R.string.tags)
    }
}