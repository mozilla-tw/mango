package org.mozilla.focus.activity

import androidx.lifecycle.Observer
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import dagger.Lazy
import kotlinx.android.synthetic.main.activity_edit_bookmark.*
import org.mozilla.focus.R
import org.mozilla.focus.persistence.BookmarkModel
import org.mozilla.focus.viewmodel.BookmarkViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import javax.inject.Inject

private const val SAVE_ACTION_ID = 1
const val ITEM_UUID_KEY = "ITEM_UUID_KEY"

class EditBookmarkActivity : BaseActivity() {

    @Inject
    lateinit var viewModelCreator: Lazy<BookmarkViewModel>

    private lateinit var viewModel: BookmarkViewModel

    private val itemId: String by lazy { intent.getStringExtra(ITEM_UUID_KEY) }
    private lateinit var bookmark: BookmarkModel
    private val editTextName: EditText by lazy { findViewById<EditText>(R.id.bookmark_name) }
    private val editTextLocation: EditText by lazy { findViewById<EditText>(R.id.bookmark_location) }
    private val labelName: TextView by lazy { findViewById<TextView>(R.id.bookmark_name_label) }
    private val labelLocation: TextView by lazy { findViewById<TextView>(R.id.bookmark_location_label) }
    private val originalName: String by lazy { bookmark.title }
    private val originalLocation: String by lazy { bookmark.url }
    private lateinit var menuItemSave: MenuItem
    private var nameChanged: Boolean = false
    private var locationChanged: Boolean = false
    private var locationEmpty: Boolean = false
    private val buttonClearName: ImageButton by lazy { findViewById<ImageButton>(R.id.bookmark_name_clear) }
    private val buttonClearLocation: ImageButton by lazy { findViewById<ImageButton>(R.id.bookmark_location_clear) }
    private val nameWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (::bookmark.isInitialized) {
                nameChanged = s.toString() != originalName
                setupMenuItemSave()
            }
        }
    }
    private val locationWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            if (::bookmark.isInitialized) {
                locationChanged = s.toString() != originalLocation
                locationEmpty = TextUtils.isEmpty(s)
                setupMenuItemSave()
            }
        }
    }

    private val focusChangeListener: OnFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
        when (v.id) {
            R.id.bookmark_location -> {
                labelLocation.isActivated = hasFocus
            }
            R.id.bookmark_name -> {
                labelName.isActivated = hasFocus
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        viewModel = getViewModel { viewModelCreator.get() }

        setContentView(R.layout.activity_edit_bookmark)
        setSupportActionBar(toolbar)
        val drawable: Drawable = DrawableCompat.wrap(resources.getDrawable(R.drawable.edit_close, theme))
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.paletteWhite100))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(drawable)
        editTextName.addTextChangedListener(nameWatcher)
        editTextLocation.addTextChangedListener(locationWatcher)
        editTextName.onFocusChangeListener = focusChangeListener
        editTextLocation.onFocusChangeListener = focusChangeListener
        buttonClearName.setOnClickListener {
            editTextName.text.clear()
        }
        buttonClearLocation.setOnClickListener {
            editTextLocation.text.clear()
        }

        viewModel.getBookmarkById(itemId).observe(this, Observer<BookmarkModel> { bookmarkModel ->
            bookmarkModel?.apply {
                bookmark = bookmarkModel
                editTextName.setText(bookmark.title)
                editTextLocation.setText(bookmark.url)
            }
        })
    }

    override fun onDestroy() {
        editTextName.removeTextChangedListener(nameWatcher)
        editTextLocation.removeTextChangedListener(locationWatcher)
        super.onDestroy()
    }

    override fun applyLocale() {
    }

    private fun isSaveValid(): Boolean {
        return !locationEmpty && (nameChanged || locationChanged)
    }

    fun setupMenuItemSave() {
        if (::menuItemSave.isInitialized) {
            menuItemSave.isEnabled = isSaveValid()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuItemSave = menu.add(Menu.NONE, SAVE_ACTION_ID, Menu.NONE, R.string.bookmark_edit_save)
        menuItemSave.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        setupMenuItemSave()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            SAVE_ACTION_ID -> {
                viewModel.updateBookmark(BookmarkModel(bookmark.id, editTextName.text.toString(), editTextLocation.text.toString()))
                Toast.makeText(this, R.string.bookmark_edit_success, Toast.LENGTH_LONG).show()
                finish()
            }
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
