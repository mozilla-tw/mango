package org.mozilla.rocket.home

import android.content.Context
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_home.home_background
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.viewModelProvider
import org.mozilla.rocket.theme.ThemeManager
import javax.inject.Inject

class HomeFragment : LocaleAwareFragment(), ScreenNavigator.HomeScreen {

    @Inject
    lateinit var homeViewModelFactory: HomeViewModelFactory

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var themeManager: ThemeManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        themeManager = (context as ThemeManager.ThemeHost).themeManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        homeViewModel = viewModelProvider(homeViewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initBackgroundView()
    }

    private fun initBackgroundView() {
        themeManager.subscribeThemeChange(home_background)
        val backgroundGestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                return homeViewModel.onBackgroundViewDoubleTap()
            }

            override fun onLongPress(e: MotionEvent?) {
                homeViewModel.onBackgroundViewLongPress()
            }
        })
        home_background.setOnTouchListener { _, event ->
            backgroundGestureDetector.onTouchEvent(event)
        }
        homeViewModel.toggleBackgroundColor.observe(this, Observer {
            val themeSet = themeManager.toggleNextTheme()
            TelemetryWrapper.changeThemeTo(themeSet.name)
        })
        homeViewModel.resetBackgroundColor.observe(this, Observer {
            themeManager.resetDefaultTheme()
            TelemetryWrapper.resetThemeToDefault()
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        themeManager.unsubscribeThemeChange(home_background)
    }

    override fun getFragment(): Fragment = this

    override fun onUrlInputScreenVisible(visible: Boolean) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun applyLocale() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}