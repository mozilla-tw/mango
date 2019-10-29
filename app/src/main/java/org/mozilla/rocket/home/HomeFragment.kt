package org.mozilla.rocket.home

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_home.account_layout
import kotlinx.android.synthetic.main.fragment_home.arc_panel
import kotlinx.android.synthetic.main.fragment_home.arc_view
import kotlinx.android.synthetic.main.fragment_home.content_hub
import kotlinx.android.synthetic.main.fragment_home.content_hub_layout
import kotlinx.android.synthetic.main.fragment_home.content_hub_title
import kotlinx.android.synthetic.main.fragment_home.home_background
import kotlinx.android.synthetic.main.fragment_home.home_fragment_fake_input
import kotlinx.android.synthetic.main.fragment_home.home_fragment_fake_input_icon
import kotlinx.android.synthetic.main.fragment_home.home_fragment_fake_input_text
import kotlinx.android.synthetic.main.fragment_home.home_fragment_menu_button
import kotlinx.android.synthetic.main.fragment_home.home_fragment_tab_counter
import kotlinx.android.synthetic.main.fragment_home.logo_man_notification
import kotlinx.android.synthetic.main.fragment_home.main_list
import kotlinx.android.synthetic.main.fragment_home.page_indicator
import kotlinx.android.synthetic.main.fragment_home.private_mode_button
import kotlinx.android.synthetic.main.fragment_home.profile_button
import kotlinx.android.synthetic.main.fragment_home.reward_button
import kotlinx.android.synthetic.main.fragment_home.search_panel
import kotlinx.android.synthetic.main.fragment_home.shopping_button
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.ecommerce.ui.ShoppingActivity
import org.mozilla.rocket.content.game.ui.GameActivity
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.news.ui.NewsActivity
import org.mozilla.rocket.content.travel.ui.TravelActivity
import org.mozilla.rocket.download.DownloadIndicatorViewModel
import org.mozilla.rocket.extension.showFxToast
import org.mozilla.rocket.fxa.ProfileActivity
import org.mozilla.rocket.home.contenthub.ui.ContentHub
import org.mozilla.rocket.home.logoman.ui.LogoManNotification
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.SitePage
import org.mozilla.rocket.home.topsites.ui.SitePageAdapterDelegate
import org.mozilla.rocket.home.topsites.ui.SiteViewHolder.Companion.TOP_SITE_LONG_CLICK_TARGET
import org.mozilla.rocket.home.ui.MenuButton.Companion.DOWNLOAD_STATE_DEFAULT
import org.mozilla.rocket.home.ui.MenuButton.Companion.DOWNLOAD_STATE_DOWNLOADING
import org.mozilla.rocket.home.ui.MenuButton.Companion.DOWNLOAD_STATE_UNREAD
import org.mozilla.rocket.home.ui.MenuButton.Companion.DOWNLOAD_STATE_WARNING
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.ui.RewardActivity
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchActivity
import org.mozilla.rocket.theme.ThemeManager
import org.mozilla.rocket.util.ToastMessage
import javax.inject.Inject

class HomeFragment : LocaleAwareFragment(), ScreenNavigator.HomeScreen {

    @Inject
    lateinit var homeViewModelCreator: Lazy<HomeViewModel>
    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>
    @Inject
    lateinit var downloadIndicatorViewModelCreator: Lazy<DownloadIndicatorViewModel>
    @Inject
    lateinit var appContext: Context

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var downloadIndicatorViewModel: DownloadIndicatorViewModel
    private lateinit var themeManager: ThemeManager
    private lateinit var topSitesAdapter: DelegateAdapter
    private lateinit var contentServiceSpotlightDialog: Dialog
    private var currentShoppingBtnVisibleState = false

    private val topSitesPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            homeViewModel.onTopSitesPagePositionChanged(position)
        }
    }
    private val toastObserver = Observer<ToastMessage> {
        appContext.showFxToast(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        homeViewModel = getActivityViewModel(homeViewModelCreator)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
        downloadIndicatorViewModel = getActivityViewModel(downloadIndicatorViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        themeManager = (context as ThemeManager.ThemeHost).themeManager
        initSearchToolBar()
        initBackgroundView()
        initTopSites()
        initContentHub()
        initFxaView()
        initLogoManNotification()
        observeNightMode()
        initOnboardingSpotlight()

        observeActions()
    }

    private fun initSearchToolBar() {
        home_fragment_fake_input.setOnClickListener {
            chromeViewModel.showUrlInput.call()
            TelemetryWrapper.showSearchBarHome()
        }
        home_fragment_menu_button.apply {
            setOnClickListener {
                chromeViewModel.showMenu.call()
                TelemetryWrapper.showMenuHome()
            }
            setOnLongClickListener {
                chromeViewModel.showDownloadPanel.call()
                TelemetryWrapper.longPressDownloadIndicator()
                true
            }
        }
        home_fragment_tab_counter.setOnClickListener {
            chromeViewModel.showTabTray.call()
            TelemetryWrapper.showTabTrayHome()
        }
        chromeViewModel.tabCount.observe(this, Observer {
            setTabCount(it ?: 0)
        })
        homeViewModel.isShoppingSearchEnabled.observe(this, Observer { isEnabled ->
            shopping_button.isVisible = isEnabled
            private_mode_button.isVisible = !isEnabled
        })
        shopping_button.setOnClickListener { homeViewModel.onShoppingButtonClicked() }
        homeViewModel.openShoppingSearch.observe(this, Observer {
            showShoppingSearch()
        })
        chromeViewModel.isPrivateBrowsingActive.observe(this, Observer {
            private_mode_button.isActivated = it
        })
        private_mode_button.setOnClickListener { homeViewModel.onPrivateModeButtonClicked() }
        homeViewModel.openPrivateMode.observe(this, Observer {
            chromeViewModel.togglePrivateMode.call()
        })
        downloadIndicatorViewModel.downloadIndicatorObservable.observe(this, Observer {
            home_fragment_menu_button.apply {
                when (it) {
                    DownloadIndicatorViewModel.Status.DOWNLOADING -> setDownloadState(DOWNLOAD_STATE_DOWNLOADING)
                    DownloadIndicatorViewModel.Status.UNREAD -> setDownloadState(DOWNLOAD_STATE_UNREAD)
                    DownloadIndicatorViewModel.Status.WARNING -> setDownloadState(DOWNLOAD_STATE_WARNING)
                    else -> setDownloadState(DOWNLOAD_STATE_DEFAULT)
                }
            }
        })
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

    private fun initTopSites() {
        topSitesAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(SitePage::class, R.layout.item_top_site_page, SitePageAdapterDelegate(homeViewModel, chromeViewModel))
            }
        )
        main_list.apply {
            adapter = this@HomeFragment.topSitesAdapter
            registerOnPageChangeCallback(topSitesPageChangeCallback)
        }
        var savedTopSitesPagePosition = homeViewModel.topSitesPageIndex.value
        homeViewModel.run {
            sitePages.observe(this@HomeFragment, Observer {
                page_indicator.setSize(it.size)
                topSitesAdapter.setData(it)
                savedTopSitesPagePosition?.let { savedPosition ->
                    savedTopSitesPagePosition = null
                    main_list.setCurrentItem(savedPosition, false)
                }
            })
            topSitesPageIndex.observe(this@HomeFragment, Observer {
                page_indicator.setSelection(it)
            })
            openBrowser.observe(this@HomeFragment, Observer {
                ScreenNavigator.get(context).showBrowserScreen(it.url, true, false)
            })
            showTopSiteMenu.observe(this@HomeFragment, Observer { (site, position) ->
                site as Site.RemovableSite
                val anchorView = main_list.findViewWithTag<View>(TOP_SITE_LONG_CLICK_TARGET).apply { tag = null }
                val allowToPin = !site.isPinned && homeViewModel.pinEnabled.value == true
                showTopSiteMenu(anchorView, allowToPin, site, position)
            })
        }
    }

    private fun initContentHub() {
        content_hub.setOnItemClickListener {
            homeViewModel.onContentHubItemClicked(it)
        }
        homeViewModel.run {
            contentHubItems.observe(this@HomeFragment, Observer {
                content_hub_layout.visibility = if (it.isEmpty()) {
                    View.INVISIBLE
                } else {
                    View.VISIBLE
                }
                // TODO: Remove filter when travel is ready for release
                if (!AppConstants.isDevBuild() && !AppConstants.isFirebaseBuild() && !AppConstants.isNightlyBuild()) {
                    content_hub.setItems(it.filter { item -> item !is ContentHub.Item.Travel })
                } else {
                    content_hub.setItems(it)
                }
            })
            openContentPage.observe(this@HomeFragment, Observer {
                val context = requireContext()
                when (it) {
                    is ContentHub.Item.Travel -> startActivity(TravelActivity.getStartIntent(context))
                    is ContentHub.Item.Shopping -> startActivity(ShoppingActivity.getStartIntent(context))
                    is ContentHub.Item.News -> startActivity(NewsActivity.getStartIntent(context))
                    is ContentHub.Item.Games -> startActivity(GameActivity.getStartIntent(context))
                }
            })
        }
    }

    private fun initFxaView() {
        homeViewModel.isAccountLayerVisible.observe(this, Observer {
            account_layout.isVisible = it
        })
        homeViewModel.hasUnreadMissions.observe(this, Observer {
            reward_button.isActivated = it
        })
        homeViewModel.isFxAccount.observe(this, Observer {
            profile_button.isActivated = it
        })
        reward_button.setOnClickListener { homeViewModel.onRewardButtonClicked() }
        profile_button.setOnClickListener { homeViewModel.onProfileButtonClicked() }
    }

    private fun observeNightMode() {
        chromeViewModel.isNightMode.observe(this, Observer {
            val isNightMode = it.isEnabled
            ViewUtils.updateStatusBarStyle(!isNightMode, requireActivity().window)
            topSitesAdapter.notifyDataSetChanged()
            home_background.setNightMode(isNightMode)
            content_hub_title.setNightMode(isNightMode)
            arc_view.setNightMode(isNightMode)
            arc_panel.setNightMode(isNightMode)
            search_panel.setNightMode(isNightMode)
            home_fragment_fake_input.setNightMode(isNightMode)
            home_fragment_fake_input_icon.setNightMode(isNightMode)
            home_fragment_fake_input_text.setNightMode(isNightMode)
            home_fragment_tab_counter.setNightMode(isNightMode)
            home_fragment_menu_button.setNightMode(isNightMode)
            account_layout.setNightMode(isNightMode)
            shopping_button.setNightMode(isNightMode)
            private_mode_button.setNightMode(isNightMode)
        })
    }

    override fun onStart() {
        super.onStart()
        homeViewModel.onPageForeground()
    }

    override fun onStop() {
        super.onStop()
        homeViewModel.onPageBackground()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        themeManager.unsubscribeThemeChange(home_background)
        main_list.unregisterOnPageChangeCallback(topSitesPageChangeCallback)
        homeViewModel.showToast.removeObserver(toastObserver)
    }

    override fun getFragment(): Fragment = this

    override fun onUrlInputScreenVisible(visible: Boolean) {
        if (visible) {
            chromeViewModel.onShowHomePageUrlInput()
        } else {
            chromeViewModel.onDismissHomePageUrlInput()
        }
    }

    override fun applyLocale() {
        home_fragment_fake_input_text.text = getString(R.string.home_search_bar_text)
    }

    private fun showTopSiteMenu(anchorView: View, pinEnabled: Boolean, site: Site, position: Int) {
        PopupMenu(anchorView.context, anchorView, Gravity.CLIP_HORIZONTAL)
                .apply {
                    menuInflater.inflate(R.menu.menu_top_site_item, menu)
                    menu.findItem(R.id.pin)?.apply {
                        isVisible = pinEnabled
                    }
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.pin -> homeViewModel.onPinTopSiteClicked(site, position)
                            R.id.remove -> homeViewModel.onRemoveTopSiteClicked(site)
                            else -> throw IllegalStateException("Unhandled menu item")
                        }

                        true
                    }
                }
                .show()
    }

    private fun setTabCount(count: Int, animationEnabled: Boolean = false) {
        home_fragment_tab_counter.apply {
            if (animationEnabled) {
                setCountWithAnimation(count)
            } else {
                setCount(count)
            }
            if (count > 0) {
                isEnabled = true
                alpha = 1f
            } else {
                isEnabled = false
                alpha = 0.3f
            }
        }
    }

    private fun showShoppingSearch() {
        val context: Context = this.context ?: return
        startActivity(ShoppingSearchActivity.getStartIntent(context))
    }

    private fun initLogoManNotification() {
        homeViewModel.logoManNotification.observe(this, Observer {
            it?.let { (notification, animate) ->
                showLogoManNotification(notification, animate)
            }
        })
        homeViewModel.hideLogoManNotification.observe(this, Observer {
            hideLogoManNotification()
        })
        logo_man_notification.setNotificationActionListener(object : LogoManNotification.NotificationActionListener {
            override fun onNotificationClick() {
                homeViewModel.onLogoManNotificationClicked()
            }

            override fun onNotificationDismiss() {
                homeViewModel.onLogoManDismissed()
            }
        })
    }

    private fun showLogoManNotification(notification: LogoManNotification.Notification, animate: Boolean) {
        logo_man_notification.showNotification(notification, animate)
        homeViewModel.onLogoManShown()
    }

    private fun hideLogoManNotification() {
        logo_man_notification.isVisible = false
    }

    private fun showContentServiceSpotlight() {
        activity?.let {
            content_hub.post {
                setOnboardingStatusBarColor()
                contentServiceSpotlightDialog = DialogUtils.showContentServiceOnboardingSpotlight(it, content_hub, {
                    restoreStatusBarColor()
                }) {
                    homeViewModel.onContentServiceOnboardingButtonClicked()
                }
            }
        }
    }

    private fun closeContentServiceSpotlight() {
        if (::contentServiceSpotlightDialog.isInitialized) {
            contentServiceSpotlightDialog.dismiss()
        }
    }

    private fun showShoppingSearchSpotlight() {
        activity?.let {
            shopping_button.post {
                setOnboardingStatusBarColor()
                DialogUtils.showShoppingSearchSpotlight(it, shopping_button) {
                    restoreStatusBarColor()
                    shopping_button.isVisible = currentShoppingBtnVisibleState
                    private_mode_button.isVisible = !currentShoppingBtnVisibleState
                }
            }
        }
    }

    private fun restoreStatusBarColor() {
        activity?.window?.statusBarColor = Color.TRANSPARENT
    }

    private fun setOnboardingStatusBarColor() {
        activity?.let {
            it.window.statusBarColor = ContextCompat.getColor(it, R.color.paletteBlack50)
        }
    }

    private fun initOnboardingSpotlight() {
        homeViewModel.showContentServicesOnboardingSpotlight.observe(this, Observer {
            showContentServiceSpotlight()
        })
        homeViewModel.dismissContentServiceOnboardingDialog.observe(this, Observer {
            closeContentServiceSpotlight()
        })
        homeViewModel.showShoppingSearchOnboardingSpotlight.observe(this, Observer {
            currentShoppingBtnVisibleState = shopping_button.isVisible
            shopping_button.isVisible = true
            private_mode_button.isVisible = false
            showShoppingSearchSpotlight()
        })
    }

    private fun observeActions() {
        homeViewModel.showToast.observeForever(toastObserver)
        homeViewModel.openRewardPage.observe(this, Observer {
            openRewardPage()
        })
        homeViewModel.openProfilePage.observe(this, Observer {
            openProfilePage()
        })
        homeViewModel.showMissionCompleteDialog.observe(this, Observer { mission ->
            showMissionCompleteDialog(mission)
        })
        homeViewModel.openMissionDetailPage.observe(this, Observer { mission ->
            openMissionDetailPage(mission)
        })
        homeViewModel.showContentHubClickOnboarding.observe(this, Observer { couponName ->
            showRequestClickContentHubOnboarding(couponName)
        })
    }

    private fun showMissionCompleteDialog(mission: Mission) {
        DialogUtils.createMissionCompleteDialog(requireContext(), mission.imageUrl)
                .onPositive {
                    homeViewModel.onRedeemCompletedMissionButtonClicked(mission)
                }
                .onNegative {
                    homeViewModel.onRedeemCompletedLaterButtonClicked()
                }
                .onClose {
                    homeViewModel.onRedeemCompletedDialogClosed()
                }
                .show()
    }

    private fun showRequestClickContentHubOnboarding(couponName: String) {
        activity?.let {
            content_hub.post {
                homeViewModel.onShowClickContentHubOnboarding()
                setOnboardingStatusBarColor()
                DialogUtils.showContentServiceRequestClickSpotlight(it, content_hub, couponName) {
                    restoreStatusBarColor()
                    homeViewModel.onContentHubRequestClickHintDismissed()
                }
            }
        }
    }

    private fun openRewardPage() {
        startActivity(RewardActivity.getStartIntent(requireContext()))
    }

    private fun openProfilePage() {
        startActivity(ProfileActivity.getStartIntent(requireContext()))
    }

    private fun openMissionDetailPage(mission: Mission) {
        startActivity(RewardActivity.getStartIntent(requireContext(), RewardActivity.DeepLink.MissionDetailPage(mission)))
    }
}
