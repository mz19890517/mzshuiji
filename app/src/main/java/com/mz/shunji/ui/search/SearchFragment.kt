package com.mz.shunji.ui.search

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.mz.shunji.R
import com.mz.shunji.data.model.Note
import com.mz.shunji.databinding.FragmentSearchBinding
import com.mz.shunji.databinding.LayoutNoteBinding
import com.mz.shunji.ui.common.AbstractNotesFragment
import com.mz.shunji.ui.utils.navigateSafely
import com.mz.shunji.ui.utils.requestFocusAndKeyboard
import com.mz.shunji.ui.utils.viewBinding

class SearchFragment : AbstractNotesFragment(resId = R.layout.fragment_search) {
    private val binding by viewBinding(FragmentSearchBinding::bind)
    private val args: SearchFragmentArgs by navArgs()

    override val currentDestinationId: Int = R.id.fragment_search
    override val model: SearchViewModel by viewModel()

    override val isSelectionEnabled = false
    override val hasMenu: Boolean = false

    override val recyclerView: RecyclerView
        get() = binding.recyclerSearch
    override val swipeRefreshLayout: SwipeRefreshLayout
        get() = binding.layoutSwipeRefresh
    override val emptyIndicator: View
        get() = binding.indicatorNotesEmpty
    override val toolbar: Toolbar
        get() = binding.toolbar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerAdapter.searchMode = true

        binding.editTextSearch.doAfterTextChanged { text ->
            model.setSearchQuery(text.toString())
        }

        when {
            !model.isFirstLoad -> return
            args.searchQuery.isNotEmpty() -> {
                binding.editTextSearch.setText(args.searchQuery)
                binding.editTextSearch.requestFocusAndMoveCaret()
            }
            binding.editTextSearch.text?.isEmpty() == true -> {
                binding.editTextSearch.requestFocusAndKeyboard()
            }
        }

        model.isFirstLoad = false
    }

    override fun onNoteClick(noteId: Long, position: Int, viewBinding: LayoutNoteBinding) {
        applyNavToEditorAnimation(position)
        findNavController().navigateSafely(
            SearchFragmentDirections.actionSearchToEditor("editor_$noteId").setNoteId(noteId),
            FragmentNavigatorExtras(viewBinding.root to "editor_$noteId")
        )
    }

    override fun onNoteLongClick(noteId: Long, position: Int, viewBinding: LayoutNoteBinding): Boolean {
        showMenuForNote(position, isSelectionEnabled = false)
        return true
    }

    override fun onNotesChanged(notes: List<Note>) {
        binding.indicatorTextView.text =
            if (notes.isEmpty() && binding.editTextSearch.text?.isNotEmpty() == true) getString(R.string.indicator_no_results_found)
            else getString(R.string.indicator_search_empty)

        if (binding.editTextSearch.text?.isNotEmpty() == true)
            recyclerAdapter.submitList(notes)
        else
            recyclerAdapter.submitList(listOf())
    }
}
