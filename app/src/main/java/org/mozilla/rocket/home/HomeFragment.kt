package org.mozilla.rocket.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.viewModelProvider
import javax.inject.Inject

class HomeFragment : LocaleAwareFragment(), ScreenNavigator.HomeScreen {

    @Inject
    lateinit var homeViewModelFactory: HomeViewModelFactory

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        homeViewModel = viewModelProvider(homeViewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun getFragment(): Fragment = this

    override fun onUrlInputScreenVisible(visible: Boolean) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun applyLocale() {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}