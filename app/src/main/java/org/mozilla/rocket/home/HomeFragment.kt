package org.mozilla.rocket.home

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import kotlinx.android.synthetic.main.fragment_home.home_fragment_fake_input_text
import kotlinx.android.synthetic.main.fragment_home.home_fragment_menu_button
import kotlinx.android.synthetic.main.fragment_home.home_fragment_tab_counter
import kotlinx.android.synthetic.main.fragment_home.logo_man
import kotlinx.android.synthetic.main.fragment_home.main_list
import kotlinx.android.synthetic.main.fragment_home.mission_button
import kotlinx.android.synthetic.main.fragment_home.notification_board
import kotlinx.android.synthetic.main.fragment_home.page_indicator
import kotlinx.android.synthetic.main.fragment_home.search_panel
import kotlinx.android.synthetic.main.fragment_home.shopping_button
import kotlinx.android.synthetic.main.home_notification_board.notification_subtitle
import kotlinx.android.synthetic.main.home_notification_board.notification_title
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.AdapterDelegatesManager
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.ecommerce.ui.ShoppingActivity
import org.mozilla.rocket.content.games.ui.GamesActivity
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.news.ui.NewsActivity
import org.mozilla.rocket.extension.dpToPx
import org.mozilla.rocket.home.contenthub.ui.ContentHub
import org.mozilla.rocket.home.topsites.ui.Site
import org.mozilla.rocket.home.topsites.ui.SitePage
import org.mozilla.rocket.home.topsites.ui.SitePageAdapterDelegate
import org.mozilla.rocket.home.topsites.ui.SiteViewHolder.Companion.TOP_SITE_LONG_CLICK_TARGET
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchActivity
import org.mozilla.rocket.theme.ThemeManager
import javax.inject.Inject
import kotlin.math.abs

class HomeFragment : LocaleAwareFragment(), ScreenNavigator.HomeScreen {

    @Inject
    lateinit var homeViewModelCreator: Lazy<HomeViewModel>
    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var themeManager: ThemeManager
    private lateinit var topSitesAdapter: DelegateAdapter
    private lateinit var logoManAdapter: DelegateAdapter

    private val topSitesPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            homeViewModel.onTopSitesPagePositionChanged(position)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        homeViewModel = getActivityViewModel(homeViewModelCreator)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
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
        initLogoMan()
        observeNightMode()

        // test
        showLogoManNotification(
            Notification(
                icon = "",
                title = "Win your free coupon",
                subtitle = "7-day challenge for Rs 15,000 shopping coupon7-day challenge for Rs 15,000 "
            )
        )
    }

    private fun initSearchToolBar() {
        home_fragment_fake_input.setOnClickListener {
            chromeViewModel.showUrlInput.call()
            TelemetryWrapper.showSearchBarHome()
        }
        home_fragment_menu_button.setOnClickListener {
            chromeViewModel.showMenu.call()
            TelemetryWrapper.showMenuHome()
        }
        home_fragment_tab_counter.setOnClickListener {
            chromeViewModel.showTabTray.call()
            TelemetryWrapper.showTabTrayHome()
        }
        shopping_button.setOnClickListener { homeViewModel.onShoppingButtonClicked() }
        homeViewModel.launchShoppingSearch.observe(this, Observer {
            showShoppingSearch()
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
            showTopSiteMenu.observe(this@HomeFragment, Observer { site ->
                site as Site.RemovableSite
                val anchorView = main_list.findViewWithTag<View>(TOP_SITE_LONG_CLICK_TARGET).apply { tag = null }
                val allowToPin = !site.isPinned && homeViewModel.pinEnabled.value == true
                showTopSiteMenu(anchorView, allowToPin, site)
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
                content_hub.setItems(it)
            })
            navigateToContentPage.observe(this@HomeFragment, Observer {
                val context = requireContext()
                when (it) {
//                    is ContentHub.Item.Travel -> // TODO: navigation
                    is ContentHub.Item.Shopping -> startActivity(ShoppingActivity.getStartIntent(context))
                    is ContentHub.Item.News -> startActivity(NewsActivity.getStartIntent(context))
                    is ContentHub.Item.Games -> startActivity(GamesActivity.getStartIntent(context))
                }
            })
        }
    }

    private fun initFxaView() {
        homeViewModel.hasPendingMissions.observe(this, Observer {
            mission_button.isActivated = it
        })
        mission_button.setOnClickListener { showMissionFragment() }
    }

    private fun showMissionFragment() {
        ScreenNavigator.get(context).addMissionDetail()
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
            home_fragment_fake_input_text.setNightMode(isNightMode)
            account_layout.setNightMode(isNightMode)
        })
    }

    override fun onStart() {
        super.onStart()
        TelemetryWrapper.showHome()
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.updateTopSitesData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        themeManager.unsubscribeThemeChange(home_background)
        main_list.unregisterOnPageChangeCallback(topSitesPageChangeCallback)
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
        home_fragment_fake_input_text.text = "" // TODO: use resource id after defined
    }

    private fun showTopSiteMenu(anchorView: View, pinEnabled: Boolean, site: Site) {
        PopupMenu(anchorView.context, anchorView, Gravity.CLIP_HORIZONTAL)
                .apply {
                    menuInflater.inflate(R.menu.menu_top_site_item, menu)
                    menu.findItem(R.id.pin)?.apply {
                        isVisible = pinEnabled
                    }
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.pin -> homeViewModel.onPinTopSiteClicked(site)
                            R.id.remove -> homeViewModel.onRemoveTopSiteClicked(site)
                            else -> throw IllegalStateException("Unhandled menu item")
                        }

                        true
                    }
                }
                .show()
    }

    private fun showShoppingSearch() {
        val context: Context = this.context ?: return
        startActivity(ShoppingSearchActivity.getStartIntent(context))
    }

    private fun showLogoManNotification(notification: Notification) {
        logoManAdapter.setData(listOf(notification))
        startLogoManSwipeIn()
    }

    private fun initLogoMan() {
        logoManAdapter = DelegateAdapter(
            AdapterDelegatesManager().apply {
                add(Notification::class, R.layout.home_notification_board, LogoManNotificationAdapterDelegate())
            }
        )
        notification_board.apply {
            adapter = logoManAdapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }

        val swipeFlag = ItemTouchHelper.START or ItemTouchHelper.END
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, swipeFlag) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                startLogoManSwipeOut()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val alpha = 1f - abs(dX) / (recyclerView.width / 2f)
                    viewHolder.itemView.alpha = alpha
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(notification_board)
    }

    class LogoManNotificationAdapterDelegate : AdapterDelegate {
        override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
                LogoManNotificationViewHolder(view)
    }

    class LogoManNotificationViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
        override fun bind(uiModel: DelegateAdapter.UiModel) {
            uiModel as Notification
            notification_title.text = uiModel.title
            notification_subtitle.text = uiModel.subtitle
        }
    }

    data class Notification(
        val icon: String,
        val title: String,
        val subtitle: String
    ) : DelegateAdapter.UiModel()

    private fun startLogoManSwipeIn() {
        val logoManListener = ValueAnimator.AnimatorUpdateListener {
            val value = it.animatedValue as Int
            logo_man.translationY = value.toFloat()
        }
        val notificationBoardListener = ValueAnimator.AnimatorUpdateListener {
            val value = it.animatedValue as Int
            notification_board.translationY = value.toFloat()
        }
        val logoManMoveInAnimatorSet = AnimatorSet().apply {
            val logoManSwipeInAnimator1 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_IN_1_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_IN_1_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_IN_1_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            val logoManSwipeInAnimator2 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_IN_2_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_IN_2_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_IN_2_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            val logoManSwipeInAnimator3 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_IN_3_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_IN_3_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_IN_3_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            playSequentially(logoManSwipeInAnimator1, logoManSwipeInAnimator2, logoManSwipeInAnimator3)
        }
        val notificationBoardMoveInAnimatorSet = AnimatorSet().apply {
            val notificationBoardMoveInAnimator1 = ValueAnimator.ofInt(
                dpToPx(NOTIFICATION_BOARD_SWIPE_IN_1_START_Y_IN_DP),
                dpToPx(NOTIFICATION_BOARD_SWIPE_IN_1_END_Y_IN_DP)
            ).apply {
                duration = NOTIFICATION_BOARD_SWIPE_IN_1_DURATION_IN_MS
                addUpdateListener(notificationBoardListener)
            }
            val notificationBoardMoveInAnimator2 = ValueAnimator.ofInt(
                dpToPx(NOTIFICATION_BOARD_SWIPE_IN_2_START_Y_IN_DP),
                dpToPx(NOTIFICATION_BOARD_SWIPE_IN_2_END_Y_IN_DP)
            ).apply {
                duration = NOTIFICATION_BOARD_SWIPE_IN_2_DURATION_IN_MS
                addUpdateListener(notificationBoardListener)
            }
            playSequentially(notificationBoardMoveInAnimator1, notificationBoardMoveInAnimator2)
        }
        AnimatorSet().apply {
            this.play(logoManMoveInAnimatorSet)
                    .with(notificationBoardMoveInAnimatorSet)
                    .after(1500)
        }.start()
    }

    private fun startLogoManSwipeOut() {
        val logoManListener = ValueAnimator.AnimatorUpdateListener {
            val value = it.animatedValue as Int
            logo_man.translationY = value.toFloat()
        }
        AnimatorSet().apply {
            val logoManMoveOutAnimator1 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_OUT_1_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_OUT_1_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_OUT_1_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            val logoManMoveOutAnimator2 = ValueAnimator.ofInt(
                dpToPx(LOGO_MAN_SWIPE_OUT_2_START_Y_IN_DP),
                dpToPx(LOGO_MAN_SWIPE_OUT_2_END_Y_IN_DP)
            ).apply {
                duration = LOGO_MAN_SWIPE_OUT_2_DURATION_IN_MS
                addUpdateListener(logoManListener)
            }
            playSequentially(logoManMoveOutAnimator1, logoManMoveOutAnimator2)
        }.start()
    }

    companion object {
        private const val LOGO_MAN_SWIPE_IN_1_DURATION_IN_MS = 270L
        private const val LOGO_MAN_SWIPE_IN_1_START_Y_IN_DP = 144f
        private const val LOGO_MAN_SWIPE_IN_1_END_Y_IN_DP = 41f

        private const val LOGO_MAN_SWIPE_IN_2_DURATION_IN_MS = 100L
        private const val LOGO_MAN_SWIPE_IN_2_START_Y_IN_DP = 41f
        private const val LOGO_MAN_SWIPE_IN_2_END_Y_IN_DP = 31f

        private const val LOGO_MAN_SWIPE_IN_3_DURATION_IN_MS = 130L
        private const val LOGO_MAN_SWIPE_IN_3_START_Y_IN_DP = 31f
        private const val LOGO_MAN_SWIPE_IN_3_END_Y_IN_DP = 36f

        private const val LOGO_MAN_SWIPE_OUT_1_DURATION_IN_MS = 160L
        private const val LOGO_MAN_SWIPE_OUT_1_START_Y_IN_DP = 36f
        private const val LOGO_MAN_SWIPE_OUT_1_END_Y_IN_DP = 10f

        private const val LOGO_MAN_SWIPE_OUT_2_DURATION_IN_MS = 270L
        private const val LOGO_MAN_SWIPE_OUT_2_START_Y_IN_DP = 10f
        private const val LOGO_MAN_SWIPE_OUT_2_END_Y_IN_DP = 144f

        private const val NOTIFICATION_BOARD_SWIPE_IN_1_DURATION_IN_MS = 300L
        private const val NOTIFICATION_BOARD_SWIPE_IN_1_START_Y_IN_DP = 75f
        private const val NOTIFICATION_BOARD_SWIPE_IN_1_END_Y_IN_DP = -60f

        private const val NOTIFICATION_BOARD_SWIPE_IN_2_DURATION_IN_MS = 100L
        private const val NOTIFICATION_BOARD_SWIPE_IN_2_START_Y_IN_DP = -60f
        private const val NOTIFICATION_BOARD_SWIPE_IN_2_END_Y_IN_DP = -56f
    }
}