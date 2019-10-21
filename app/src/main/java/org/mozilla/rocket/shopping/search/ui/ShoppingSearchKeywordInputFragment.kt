package org.mozilla.rocket.shopping.search.ui

import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_shopping_search_keyword_input.*
import org.mozilla.focus.R
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.shopping.search.data.ShoppingSearchMode
import javax.inject.Inject

class ShoppingSearchKeywordInputFragment : Fragment(), View.OnClickListener {

    @Inject
    lateinit var viewModelCreator: Lazy<ShoppingSearchKeywordInputViewModel>

    private lateinit var viewModel: ShoppingSearchKeywordInputViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        viewModel = getViewModel(viewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shopping_search_keyword_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ShoppingSearchMode.getInstance(view.context).deleteKeyword()

        viewModel.uiModel.observe(this, Observer { uiModel ->
            setupView(uiModel)
        })

        viewModel.navigateToResultTab.observe(this, Observer { showResultTab(it) })

        search_keyword_edit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // TODO: Deal with non-sequence responses when a user types quickly
                s?.let { viewModel.fetchSuggestions(it.toString()) }
            }
        })
        search_keyword_edit.setOnEditorActionListener { editTextView, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    viewModel.onKeywordSent(editTextView.text.toString())
                    true
                }
                else -> false
            }
        }
        search_keyword_edit.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            // Avoid showing keyboard again when returning to the previous page by back key.
            if (hasFocus) {
                ViewUtils.showKeyboard(v)
            } else {
                ViewUtils.hideKeyboard(v)
            }
        }

        clear.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        search_keyword_edit.requestFocus()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.clear -> search_keyword_edit.text.clear()
            R.id.suggestion_item -> {
                val searchTerm = (view as TextView).text
                search_keyword_edit.text = SpannableStringBuilder(searchTerm)
                viewModel.onKeywordSent(searchTerm.toString())
            }
            else -> throw IllegalStateException("Unhandled view in onClick()")
        }
    }

    private fun setupView(uiModel: ShoppingSearchKeywordInputUiModel) {
        hint_container.visibility = if (uiModel.hideHintContainer) View.GONE else View.VISIBLE
        logo_man.visibility = if (uiModel.hideLogoMan) View.GONE else View.VISIBLE
        indication.visibility = if (uiModel.hideIndication) View.GONE else View.VISIBLE
        clear.visibility = if (uiModel.hideClear) View.GONE else View.VISIBLE
        setSuggestions(uiModel.keywordSuggestions)
    }

    private fun setSuggestions(suggestions: List<CharSequence>?) {
        search_suggestion_view.removeAllViews()
        if (suggestions == null) {
            return
        }

        for (suggestion in suggestions) {
            val item = View.inflate(context, R.layout.tag_text, null) as TextView
            item.text = suggestion
            item.setOnClickListener(this)
            this.search_suggestion_view.addView(item)
        }
    }

    private fun showResultTab(keyword: String) {
        findNavController().navigate(
            ShoppingSearchKeywordInputFragmentDirections.actionSearchKeywordToResult(keyword)
        )
    }
}
