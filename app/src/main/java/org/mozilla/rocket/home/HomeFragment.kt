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
import kotlinx.android.synthetic.main.fragment_home.main_list
import kotlinx.android.synthetic.main.fragment_home.mission_button
import kotlinx.android.synthetic.main.fragment_home.page_indicator
import kotlinx.android.synthetic.main.fragment_home.search_panel
import kotlinx.android.synthetic.main.fragment_home.shopping_button
import kotlinx.android.synthetic.main.fragment_home.logoman
import kotlinx.android.synthetic.main.fragment_home.notification_board
import kotlinx.android.synthetic.main.home_notification_board.view.*
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.ViewUtils
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

class HomeFragment : LocaleAwareFragment(), ScreenNavigator.HomeScreen {

    @Inject
    lateinit var homeViewModelCreator: Lazy<HomeViewModel>
    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var themeManager: ThemeManager
    private lateinit var topSitesAdapter: DelegateAdapter

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initNotificationBoard()
        swipeInLogoManNotification()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        themeManager = (context as ThemeManager.ThemeHost).themeManager
        initSearchToolBar()
        initBackgroundView()
        initTopSites()
        initContentHub()
        initFxaView()
        observeNightMode()
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

    private fun initNotificationBoard() {

        notification_board.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

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
                swipeOutLogoMan()
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
                    val alpha = 1f - Math.abs(dX) / (recyclerView.width / 2f)
                    viewHolder.itemView.alpha = alpha
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(notification_board)

        notification_board.adapter = NotificatoinBoardAdapter(requireContext())
    }

    class NotificatoinBoardAdapter(val context: Context) : RecyclerView.Adapter<NotificationBoardViewHolder>() {

        override fun getItemCount(): Int {
            return 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationBoardViewHolder {
            return NotificationBoardViewHolder(LayoutInflater.from(context).inflate(R.layout.home_notification_board, parent, false))
        }

        override fun onBindViewHolder(holder: NotificationBoardViewHolder, position: Int) {
            holder.title.text = "Win your free coupon"
            holder.subtitle.text = "7-day challenge for Rs 15,000 shopping coupon7-day challenge for Rs 15,000 "
        }
    }

    class NotificationBoardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.notification_title
        val subtitle = view.notification_subtitle
        val icon = view.notification_icon
    }

    private fun swipeInLogoManNotification() {

        val logomanListener = ValueAnimator.AnimatorUpdateListener() {
            val value = it.animatedValue as Int
            logoman.translationY = value.toFloat()
        }

        val logomanSwipeInAnimator1 = ValueAnimator.ofInt(
                logoman.dpToPx(LOGOMAN_SWIPEIN_1_START_Y_IN_DP),
                logoman.dpToPx(LOGOMAN_SWIPEIN_1_END_Y_IN_DP))
        logomanSwipeInAnimator1.setDuration(LOGOMAN_SWIPEIN_1_DURATION_IN_MS)
        logomanSwipeInAnimator1.addUpdateListener(logomanListener)

        val logomanSwipeInAnimator2 = ValueAnimator.ofInt(
                logoman.dpToPx(LOGOMAN_SWIPEIN_2_START_Y_IN_DP),
                logoman.dpToPx(LOGOMAN_SWIPEIN_2_END_Y_IN_DP))
        logomanSwipeInAnimator2.setDuration(LOGOMAN_SWIPEIN_2_DURATION_IN_MS)
        logomanSwipeInAnimator2.addUpdateListener(logomanListener)

        val logomanSwipeInAnimator3 = ValueAnimator.ofInt(
                logoman.dpToPx(LOGOMAN_SWIPEIN_3_START_Y_IN_DP),
                logoman.dpToPx(LOGOMAN_SWIPEIN_3_END_Y_IN_DP))
        logomanSwipeInAnimator3.setDuration(LOGOMAN_SWIPEIN_3_DURATION_IN_MS)
        logomanSwipeInAnimator3.addUpdateListener(logomanListener)

        val logomanSwipeInAnimatorSet = AnimatorSet()

        logomanSwipeInAnimatorSet.playSequentially(logomanSwipeInAnimator1, logomanSwipeInAnimator2, logomanSwipeInAnimator3)

        val notificationBoardListener = ValueAnimator.AnimatorUpdateListener() {
            val value = it.animatedValue as Int
            notification_board.translationY = value.toFloat()
        }

        val notificationBoardSwipeInAnimator1 = ValueAnimator.ofInt(
                notification_board.dpToPx(NOTIFICATION_BOARD_SWIPEIN_1_START_Y_IN_DP),
                notification_board.dpToPx(NOTIFICATION_BOARD_SWIPEIN_1_END_Y_IN_DP))
        notificationBoardSwipeInAnimator1.setDuration(NOTIFICATION_BOARD_SWIPEIN_1_DURATION_IN_MS)
        notificationBoardSwipeInAnimator1.addUpdateListener(notificationBoardListener)

        val notificationBoardSwipeInAnimator2 = ValueAnimator.ofInt(
                notification_board.dpToPx(NOTIFICATION_BOARD_SWIPEIN_2_START_Y_IN_DP),
                notification_board.dpToPx(NOTIFICATION_BOARD_SWIPEIN_2_END_Y_IN_DP))
        notificationBoardSwipeInAnimator2.setDuration(NOTIFICATION_BOARD_SWIPEIN_2_DURATION_IN_MS)
        notificationBoardSwipeInAnimator2.addUpdateListener(notificationBoardListener)

        val notificationBoardSwipeInAnimatorSet = AnimatorSet()
        notificationBoardSwipeInAnimatorSet.playSequentially(notificationBoardSwipeInAnimator1, notificationBoardSwipeInAnimator2)

        val swipeInAnimatorSet = AnimatorSet()
        swipeInAnimatorSet.play(logomanSwipeInAnimatorSet).with(notificationBoardSwipeInAnimatorSet).after(1500)
        swipeInAnimatorSet.start()
    }

    private fun swipeOutLogoMan() {

        val logomanListener = ValueAnimator.AnimatorUpdateListener() {
            val value = it.animatedValue as Int
            logoman.translationY = value.toFloat()
        }

        val logomanSwipeOutAnimator1 = ValueAnimator.ofInt(
                logoman.dpToPx(LOGOMAN_SWIPEOUT_1_START_Y_IN_DP),
                logoman.dpToPx(LOGOMAN_SWIPEOUT_1_END_Y_IN_DP))
        logomanSwipeOutAnimator1.setDuration(LOGOMAN_SWIPEOUT_1_DURATION_IN_MS)
        logomanSwipeOutAnimator1.addUpdateListener(logomanListener)

        val logomanSwipeOutAnimator2 = ValueAnimator.ofInt(
                logoman.dpToPx(LOGOMAN_SWIPEOUT_2_START_Y_IN_DP),
                logoman.dpToPx(LOGOMAN_SWIPEOUT_2_END_Y_IN_DP))
        logomanSwipeOutAnimator2.setDuration(LOGOMAN_SWIPEOUT_2_DURATION_IN_MS)
        logomanSwipeOutAnimator2.addUpdateListener(logomanListener)

        val logomanSwipeOutAnimatorSet = AnimatorSet()
        logomanSwipeOutAnimatorSet.playSequentially(logomanSwipeOutAnimator1, logomanSwipeOutAnimator2)
        logomanSwipeOutAnimatorSet.start()
    }

    companion object {
        private const val LOGOMAN_SWIPEIN_1_DURATION_IN_MS = 270L
        private const val LOGOMAN_SWIPEIN_1_START_Y_IN_DP = 108f
        private const val LOGOMAN_SWIPEIN_1_END_Y_IN_DP = 5f

        private const val LOGOMAN_SWIPEIN_2_DURATION_IN_MS = 100L
        private const val LOGOMAN_SWIPEIN_2_START_Y_IN_DP = 5f
        private const val LOGOMAN_SWIPEIN_2_END_Y_IN_DP = -5f

        private const val LOGOMAN_SWIPEIN_3_DURATION_IN_MS = 130L
        private const val LOGOMAN_SWIPEIN_3_START_Y_IN_DP = -5f
        private const val LOGOMAN_SWIPEIN_3_END_Y_IN_DP = 0f

        private const val LOGOMAN_SWIPEOUT_1_DURATION_IN_MS = 160L
        private const val LOGOMAN_SWIPEOUT_1_START_Y_IN_DP = 0f
        private const val LOGOMAN_SWIPEOUT_1_END_Y_IN_DP = -26f

        private const val LOGOMAN_SWIPEOUT_2_DURATION_IN_MS = 270L
        private const val LOGOMAN_SWIPEOUT_2_START_Y_IN_DP = -26f
        private const val LOGOMAN_SWIPEOUT_2_END_Y_IN_DP = 108f

        private const val NOTIFICATION_BOARD_SWIPEIN_1_DURATION_IN_MS = 300L
        private const val NOTIFICATION_BOARD_SWIPEIN_1_START_Y_IN_DP = 75f
        private const val NOTIFICATION_BOARD_SWIPEIN_1_END_Y_IN_DP = -60f

        private const val NOTIFICATION_BOARD_SWIPEIN_2_DURATION_IN_MS = 100L
        private const val NOTIFICATION_BOARD_SWIPEIN_2_START_Y_IN_DP = -60f
        private const val NOTIFICATION_BOARD_SWIPEIN_2_END_Y_IN_DP = -56f
    }
}