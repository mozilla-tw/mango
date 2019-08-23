package org.mozilla.rocket.content.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.utils.Settings
import org.mozilla.lite.partner.NewsItem
import org.mozilla.rocket.content.*
import org.mozilla.rocket.content.news.data.NewsRepository
import org.mozilla.rocket.content.news.data.NewsSettingsLocalDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRemoteDataSource
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.rocket.content.news.data.NewsSourceManager
import org.mozilla.rocket.content.portal.ContentFeature
import org.mozilla.rocket.content.portal.ContentPortalListener
import org.mozilla.rocket.widget.BottomSheetBehavior
import javax.inject.Inject

class NewsFragment : Fragment(), ContentPortalListener, NewsViewContract {

    @Inject
    lateinit var newsViewModelCreator: Lazy<NewsViewModel>

    override fun getCategory(): String {
        return arguments?.getString(ContentFeature.TYPE_KEY) ?: "top-news"
    }

    override fun getLanguage(): String {
        return arguments?.getString(ContentFeature.EXTRA_NEWS_LANGUAGE) ?: "english"
    }

    override fun updateSourcePriority() {
        // the user had seen the news. Treat it as an user selection so no on can change it
        Settings.getInstance(context).setPriority(NewsSourceManager.PREF_INT_NEWS_PRIORITY, Settings.PRIORITY_USER)
    }

    companion object {
        private const val NEWS_THRESHOLD = 10
        private const val TIME_MILLIS_STATE_LOADING_THRESHOLD = 5000L

        fun newInstance(bottomSheetBehavior: BottomSheetBehavior<View>): NewsFragment {
            return NewsFragment().also { it.bottomSheetBehavior = bottomSheetBehavior }
        }

        fun newInstance(category: String, language: String): NewsFragment {
            val args = Bundle().apply {
                putString(ContentFeature.TYPE_KEY, category)
                putString(ContentFeature.EXTRA_NEWS_LANGUAGE, language)
            }
            return NewsFragment().apply {
                arguments = args
            }
        }
    }

    interface NewsListListener {
        fun loadMore()
    }

    private var newsPresenter: NewsPresenter? = null
    private var recyclerView: RecyclerView? = null
    private var newsListListener: NewsListListener? = null
    private var newsEmptyView: View? = null
    private var newsProgressCenter: ProgressBar? = null
    private var newsAdapter: NewsAdapter<NewsItem>? = null
    private var newsListLayoutManager: LinearLayoutManager? = null
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.content_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val newsViewModel: NewsViewModel = getActivityViewModel { newsViewModelCreator.get() }
        newsPresenter = NewsPresenter(this, newsViewModel)

        // creating a repository will also create a new subscription.
        // we deliberately create a new subscription again to load data aggressively.
        val newsSettingsRemoteDataSource = NewsSettingsRemoteDataSource()
        val newsSettingsLocalDataSource = NewsSettingsLocalDataSource(appContext())
        val newsSettingRepo = NewsSettingsRepository(newsSettingsRemoteDataSource, newsSettingsLocalDataSource)

        val newsRepo = NewsRepository.newInstance(
            context,
            hashMapOf(
                NewsRepository.CONFIG_URL to NewsSourceManager.getInstance().newsSourceUrl,
                NewsRepository.CONFIG_CATEGORY to getCategory(),
                NewsRepository.CONFIG_LANGUAGE to getLanguage()
            )
        )

        newsPresenter?.setupNewsViewModel(newsRepo, newsSettingRepo)
        newsListListener = newsPresenter

        view.findViewById<Button>(R.id.news_try_again)?.setOnClickListener {
            // call onStatus() again with null to display the loading indicator
            onStatus(null)
            newsListListener?.loadMore()
        }
        recyclerView = view.findViewById(R.id.news_list)
        newsEmptyView = view.findViewById(R.id.empty_view_container)
        newsProgressCenter = view.findViewById(R.id.news_progress_center)
        newsAdapter = NewsAdapter(this)

        recyclerView?.adapter = newsAdapter
        newsListLayoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        recyclerView?.layoutManager = newsListLayoutManager
        newsListLayoutManager?.let {
            recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val totalItemCount = it.itemCount
                    val visibleItemCount = it.childCount
                    val lastVisibleItem = it.findLastVisibleItemPosition()
                    if (visibleItemCount + lastVisibleItem + NEWS_THRESHOLD >= totalItemCount) {
                        newsListListener?.loadMore()
                    }
                }
            })
        }
    }

    override fun updateNews(items: List<NewsItem>?) {
        onStatus(items)

        newsAdapter?.submitList(items)
        ContentPortalViewState.lastNewsPos?.let {
            val size = items?.size
            if (size != null && size > it) {
                newsListLayoutManager?.scrollToPosition(it)
                // forget about last scroll position
                ContentPortalViewState.lastNewsPos = null
            }
        }
    }

    override fun onItemClicked(url: String) {
        ScreenNavigator.get(context).showBrowserScreen(url, true, false)

        // use findFirstVisibleItemPosition so we don't need to remember offset
        newsListLayoutManager?.findFirstVisibleItemPosition()?.let {
            ContentPortalViewState.lastNewsPos = it
        }
    }

    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var stateLoadingJob: Job? = null

    override fun onStatus(items: List<NewsItem>?) {
        when {
            items == null -> {
                stateLoading()
                stateLoadingJob = uiScope.launch {
                    delay(TIME_MILLIS_STATE_LOADING_THRESHOLD)
                    stateEmpty()
                }
            }
            items.isEmpty() -> {
                stateLoadingJob?.cancel()
                stateLoadingJob = null
                stateEmpty()
            }
            else -> {
                stateLoadingJob?.cancel()
                stateLoadingJob = null
                stateDisplay()
            }
        }
    }

    private fun stateDisplay() {
        recyclerView?.visibility = View.VISIBLE
        newsEmptyView?.visibility = View.GONE
        newsProgressCenter?.visibility = View.GONE
    }

    private fun stateEmpty() {
        recyclerView?.visibility = View.GONE
        newsEmptyView?.visibility = View.VISIBLE
        newsProgressCenter?.visibility = View.GONE
        bottomSheetBehavior?.skipCollapsed = true
    }

    private fun stateLoading() {
        recyclerView?.visibility = View.GONE
        newsEmptyView?.visibility = View.GONE
        newsProgressCenter?.visibility = View.VISIBLE
        bottomSheetBehavior?.skipCollapsed = true
    }
}