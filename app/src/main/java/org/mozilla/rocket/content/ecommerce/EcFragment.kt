package org.mozilla.rocket.content.ecommerce

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.banner.BannerAdapter
import org.mozilla.banner.BannerConfigViewModel
import org.mozilla.banner.TelemetryListener
import org.mozilla.focus.R
import org.mozilla.focus.home.BannerHelper
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.ecommerce.data.ShoppingLink
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.portal.ContentFeature.Companion.TYPE_COUPON
import org.mozilla.rocket.content.portal.ContentFeature.Companion.TYPE_KEY
import org.mozilla.rocket.content.portal.ContentFeature.Companion.TYPE_TICKET
import javax.inject.Inject

/**
 * Fragment that display the content for [ShoppingLink]s and [Coupon]s
 *
 */
class EcFragment : Fragment() {

    companion object {
        fun newInstance(feature: Int): EcFragment {
            val args = Bundle().apply {
                putInt(TYPE_KEY, feature)
            }
            return EcFragment().apply { arguments = args }
        }
    }

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private lateinit var chromeViewModel: ChromeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        when (arguments?.getInt(TYPE_KEY)) {
            TYPE_COUPON -> {
                return inflater.inflate(R.layout.content_tab_coupon, container, false).apply {
                    val recyclerView = findViewById<RecyclerView>(R.id.content_coupons)
                    val couponAdapter = CouponAdapter()
                    recyclerView?.layoutManager = LinearLayoutManager(context)
                    recyclerView?.adapter = couponAdapter
                    val coupons = AppConfigWrapper.getEcommerceCoupons()
                    couponAdapter.submitList(coupons)

                    setupCouponBanner(this)
                }
            }
            TYPE_TICKET -> {
                return inflater.inflate(R.layout.content_tab_shoppinglink, container, false).apply {

                    val recyclerView = findViewById<RecyclerView>(R.id.ct_shoppinglink_list)
                    val shoppinglinkAdapter = ShoppingLinkAdapter()
                    recyclerView?.layoutManager = GridLayoutManager(context, 2)
                    recyclerView?.adapter = shoppinglinkAdapter

                    val ecommerceShoppingLinks = AppConfigWrapper.getEcommerceShoppingLinks()
                    ecommerceShoppingLinks.add(
                        ShoppingLink(
                            "",
                            "footer",
                            "",
                            ""
                        )
                    )
                    shoppinglinkAdapter.submitList(ecommerceShoppingLinks)
                }
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setupCouponBanner(view: View) {
        val banner = view.findViewById<RecyclerView>(R.id.content_banners)
        banner.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(banner)

        val viewModelProvider = ViewModelProviders.of(activity!!)
        val viewModel = viewModelProvider.get(BannerConfigViewModel::class.java)
        val bannerLiveData = viewModel.couponConfig

        BannerHelper().initCouponBanner(context, bannerLiveData)

        bannerLiveData.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                return@Observer
            }

            banner?.adapter = BannerAdapter(
                    it,
                    { arg -> chromeViewModel.openUrl.value = ChromeViewModel.OpenUrlAction(url = arg, withNewTab = true, isFromExternal = false) },
                    telemetryListener)
            banner?.visibility = View.VISIBLE
        })
    }

    private val telemetryListener = object : TelemetryListener {
        override fun sendClickItemTelemetry(jsonString: String, itemPosition: Int) {
        }

        override fun sendClickBackgroundTelemetry(jsonString: String) {
            val jsonObject: JSONObject
            try {
                jsonObject = JSONObject(jsonString)
                var pos: String? = jsonObject.optString("pos")
                if (pos == null) {
                    pos = "-1"
                }
                val feed = jsonObject.optString("feed")
                val id = jsonObject.optString("id")
                val source = jsonObject.optString("source")
                val category = jsonObject.optString("category")
                val subCategory = jsonObject.optString("sub_category")

                TelemetryWrapper.clickOnPromoItem(pos, id, feed, source, category, subCategory)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }
}