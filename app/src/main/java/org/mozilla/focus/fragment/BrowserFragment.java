/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.download.EnqueueDownloadTask;
import org.mozilla.focus.locale.LocaleAwareFragment;
import org.mozilla.focus.menu.WebContextMenu;
import org.mozilla.focus.navigation.ScreenNavigator;
import org.mozilla.focus.screenshot.CaptureRunnable;
import org.mozilla.focus.tabs.tabtray.TabTray;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.FileChooseAction;
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.focus.utils.Settings;
import org.mozilla.focus.utils.SupportUtils;
import org.mozilla.focus.utils.ViewUtils;
import org.mozilla.focus.viewmodel.ShoppingSearchPromptViewModel;
import org.mozilla.focus.web.GeoPermissionCache;
import org.mozilla.focus.web.HttpAuthenticationDialogBuilder;
import org.mozilla.focus.widget.AnimatedProgressBar;
import org.mozilla.focus.widget.BackKeyHandleable;
import org.mozilla.focus.widget.FindInPage;
import org.mozilla.permissionhandler.PermissionHandle;
import org.mozilla.permissionhandler.PermissionHandler;
import org.mozilla.rocket.chrome.BottomBarItemAdapter;
import org.mozilla.rocket.chrome.BottomBarViewModel;
import org.mozilla.rocket.chrome.ChromeViewModel;
import org.mozilla.rocket.chrome.ChromeViewModel.ScreenCaptureTelemetryData;
import org.mozilla.rocket.content.BaseViewModelFactory;
import org.mozilla.rocket.content.ExtentionKt;
import org.mozilla.rocket.content.view.BottomBar;
import org.mozilla.rocket.download.DownloadIndicatorIntroViewHelper;
import org.mozilla.rocket.download.DownloadIndicatorViewModel;
import org.mozilla.rocket.extension.LiveDataExtensionKt;
import org.mozilla.rocket.landing.PortraitComponent;
import org.mozilla.rocket.landing.PortraitStateModel;
import org.mozilla.rocket.nightmode.themed.ThemedImageView;
import org.mozilla.rocket.nightmode.themed.ThemedLinearLayout;
import org.mozilla.rocket.nightmode.themed.ThemedRelativeLayout;
import org.mozilla.rocket.nightmode.themed.ThemedTextView;
import org.mozilla.rocket.nightmode.themed.ThemedView;
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchActivity;
import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabView;
import org.mozilla.rocket.tabs.TabViewClient;
import org.mozilla.rocket.tabs.TabViewEngineSession;
import org.mozilla.rocket.tabs.TabsSessionProvider;
import org.mozilla.rocket.tabs.utils.TabUtil;
import org.mozilla.rocket.tabs.web.Download;
import org.mozilla.threadutils.ThreadUtils;
import org.mozilla.urlutils.UrlUtils;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import javax.inject.Inject;

import dagger.Lazy;

import static org.mozilla.focus.navigation.ScreenNavigator.BROWSER_FRAGMENT_TAG;
import static org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.WEBVIEW;
import static org.mozilla.rocket.chrome.BottomBarItemAdapter.DOWNLOAD_STATE_DEFAULT;
import static org.mozilla.rocket.chrome.BottomBarItemAdapter.DOWNLOAD_STATE_DOWNLOADING;
import static org.mozilla.rocket.chrome.BottomBarItemAdapter.DOWNLOAD_STATE_UNREAD;
import static org.mozilla.rocket.chrome.BottomBarItemAdapter.DOWNLOAD_STATE_WARNING;

/**
 * Fragment for displaying the browser UI.
 */
public class BrowserFragment extends LocaleAwareFragment implements ScreenNavigator.BrowserScreen,
        LifecycleOwner,
        BackKeyHandleable {

    /**
     * Custom data that is passed when calling {@link SessionManager#addTab(String, Bundle)}
     */
    public static final String EXTRA_NEW_TAB_SRC = "extra_bkg_tab_src";
    public static final int SRC_CONTEXT_MENU = 0;

    private static final Handler HANDLER = new Handler();

    private static final int ANIMATION_DURATION = 300;

    private static final int SITE_GLOBE = 0;
    private static final int SITE_LOCK = 1;

    @Inject
    Lazy<DownloadIndicatorViewModel> downloadIndicatorViewModelCreator;
    @Inject
    Lazy<BottomBarViewModel> bottomBarViewModelCreator;
    @Inject
    Lazy<ChromeViewModel> chromeViewModelCreator;
    @Inject
    Lazy<ShoppingSearchPromptViewModel> promptMessageViewModelCreator;

    private int systemVisibility = ViewUtils.SYSTEM_UI_VISIBILITY_NONE;

    private DownloadCallback downloadCallback = new DownloadCallback();

    private FindInPage findInPage;

    private static final int BUNDLE_MAX_SIZE = 300 * 1000; // 300K

    private ViewGroup webViewSlot;
    private SessionManager sessionManager;

    private ThemedRelativeLayout backgroundView;
    private TransitionDrawable backgroundTransition;
    private ThemedTextView urlView;
    private AnimatedProgressBar progressView;
    private ThemedImageView siteIdentity;
    private Dialog webContextMenu;

    private View mainContent;
    private int mainContentBottomMargin;
    private BottomBar bottomBar;

    //GeoLocationPermission
    private String geolocationOrigin;
    private GeolocationPermissions.Callback geolocationCallback;
    private AlertDialog geoDialog;

    /**
     * Container for custom video views shown in fullscreen mode.
     */
    private ViewGroup videoContainer;

    /**
     * Container containing the browser chrome and web content.
     */
    private ThemedLinearLayout browserContainer;

    private TabView.FullscreenCallback fullscreenCallback;

    private boolean isLoading = false;

    // Set an initial WeakReference so we never have to handle loadStateListenerWeakReference being null
    // (i.e. so we can always just .get()).
    private WeakReference<LoadStateListener> loadStateListenerWeakReference = new WeakReference<>(null);

    private CaptureRunnable.CaptureStateListener captureStateListener;

    // pending action for file-choosing
    private FileChooseAction fileChooseAction;

    private PermissionHandler permissionHandler;
    private static final int ACTION_DOWNLOAD = 0;
    private static final int ACTION_PICK_FILE = 1;
    private static final int ACTION_GEO_LOCATION = 2;
    private static final int ACTION_CAPTURE = 3;

    private boolean hasPendingScreenCaptureTask = false;
    private ScreenCaptureTelemetryData pendingScreenCaptureTelemetryData;

    private SessionObserver sessionObserver = new SessionObserver();
    final SessionManager.Observer managerObserver = new SessionManagerObserver(sessionObserver);

    private ThemedLinearLayout toolbarRoot;
    private ThemedView bottomMenuDivider;
    private ThemedView urlBarDivider;
    private View downloadIndicatorIntro;
    private ChromeViewModel chromeViewModel;
    private BottomBarViewModel bottomBarViewModel;
    private BottomBarItemAdapter bottomBarItemAdapter;
    private ShoppingSearchPromptViewModel shoppingSearchPromptMessageViewModel;
    private Button shoppingSearchBottomSheetSearchBtn;
    private BottomSheetBehavior shoppingSearchPromptMessageBehavior;
    private ViewStub shoppingSearchViewStub;

    private long landscapeStartTime = 0L;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        ExtentionKt.appComponent(this).inject(this);
        super.onCreate(savedInstanceState);
        bottomBarViewModel = ViewModelProviders.of(requireActivity(), new BaseViewModelFactory<>(bottomBarViewModelCreator::get)).get(BottomBarViewModel.class);
        chromeViewModel = ViewModelProviders.of(requireActivity(), new BaseViewModelFactory<>(chromeViewModelCreator::get)).get(ChromeViewModel.class);
        shoppingSearchPromptMessageViewModel = ViewModelProviders.of(requireActivity(), new BaseViewModelFactory<>(promptMessageViewModelCreator::get)).get(ShoppingSearchPromptViewModel.class);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            permissionHandler.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        permissionHandler = new PermissionHandler(new PermissionHandle() {
            @Override
            public void doActionDirect(String permission, int actionId, Parcelable params) {
                switch (actionId) {
                    case ACTION_DOWNLOAD:
                        if (getContext() == null) {
                            Log.w(BROWSER_FRAGMENT_TAG, "No context to use, abort callback onDownloadStart");
                            return;
                        }

                        Download download = (Download) params;

                        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            // We do have the permission to write to the external storage. Proceed with the download.
                            queueDownload(download);
                        }
                        break;
                    case ACTION_PICK_FILE:
                        fileChooseAction.startChooserActivity();
                        break;
                    case ACTION_GEO_LOCATION:
                        showGeolocationPermissionPrompt();
                        break;
                    case ACTION_CAPTURE:
                        showLoadingAndCapture((ScreenCaptureTelemetryData) params);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown actionId");
                }
            }

            private void actionDownloadGranted(Parcelable parcelable) {
                Download download = (Download) parcelable;
                queueDownload(download);
            }

            private void actionPickFileGranted() {
                if (fileChooseAction != null) {
                    fileChooseAction.startChooserActivity();
                }
            }

            private void actionCaptureGranted(ScreenCaptureTelemetryData telemetryData) {
                setPendingScreenCaptureTask(telemetryData);
            }

            private void doActionGrantedOrSetting(String permission, int actionId, Parcelable params) {
                switch (actionId) {
                    case ACTION_DOWNLOAD:
                        actionDownloadGranted(params);
                        break;
                    case ACTION_PICK_FILE:
                        actionPickFileGranted();
                        break;
                    case ACTION_GEO_LOCATION:
                        showGeolocationPermissionPrompt();
                        break;
                    case ACTION_CAPTURE:
                        actionCaptureGranted((ScreenCaptureTelemetryData) params);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown actionId");
                }
            }

            @Override
            public void doActionGranted(String permission, int actionId, Parcelable params) {
                doActionGrantedOrSetting(permission, actionId, params);
            }

            @Override
            public void doActionSetting(String permission, int actionId, Parcelable params) {
                doActionGrantedOrSetting(permission, actionId, params);
            }

            @Override
            public void doActionNoPermission(String permission, int actionId, Parcelable params) {
                switch (actionId) {
                    case ACTION_DOWNLOAD:
                        // Do nothing
                        break;
                    case ACTION_PICK_FILE:
                        if (fileChooseAction != null) {
                            fileChooseAction.cancel();
                            fileChooseAction = null;
                        }
                        break;
                    case ACTION_GEO_LOCATION:
                        if (geolocationCallback != null) {
                            // I'm not sure why it's so. This method already on Main thread.
                            // But if I don't do this, webview will keeps requesting for permission.
                            // See https://github.com/mozilla-tw/Rocket/blob/765f6a1ddbc2b9058813e930f63c62a9797c5fa0/app/src/webkit/java/org/mozilla/focus/webkit/FocusWebChromeClient.java#L126
                            ThreadUtils.postToMainThread(() -> BrowserFragment.this.rejectGeoRequest(false));
                        }
                        break;
                    case ACTION_CAPTURE:
                        // Do nothing
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown actionId");
                }
            }

            @Override
            public Snackbar makeAskAgainSnackBar(int actionId) {
                return PermissionHandler.makeAskAgainSnackBar(BrowserFragment.this, getActivity().findViewById(R.id.container), getAskAgainSnackBarString(actionId));
            }

            private int getAskAgainSnackBarString(int actionId) {
                if (actionId == ACTION_DOWNLOAD || actionId == ACTION_PICK_FILE || actionId == ACTION_CAPTURE) {
                    return R.string.permission_toast_storage;
                } else if (actionId == ACTION_GEO_LOCATION) {
                    return R.string.permission_toast_location;
                } else {
                    throw new IllegalArgumentException("Unknown Action");
                }
            }

            private int getPermissionDeniedToastString(int actionId) {
                if (actionId == ACTION_DOWNLOAD || actionId == ACTION_PICK_FILE || actionId == ACTION_CAPTURE) {
                    return R.string.permission_toast_storage_deny;
                } else if (actionId == ACTION_GEO_LOCATION) {
                    return R.string.permission_toast_location_deny;
                } else {
                    throw new IllegalArgumentException("Unknown Action");
                }
            }

            @Override
            public void requestPermissions(int actionId) {
                switch (actionId) {
                    case ACTION_DOWNLOAD:
                    case ACTION_CAPTURE:
                        BrowserFragment.this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, actionId);
                        break;
                    case ACTION_PICK_FILE:
                        BrowserFragment.this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, actionId);
                        break;
                    case ACTION_GEO_LOCATION:
                        BrowserFragment.this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, actionId);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown Action");
                }
            }

            @Override
            public void permissionDeniedToast(int actionId) {
                Toast.makeText(getContext(), getPermissionDeniedToastString(actionId), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onPause() {
        sessionManager.pause();
        super.onPause();
    }

    @Override
    public void applyLocale() {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        final WebView unneeded = new WebView(getContext());
        unneeded.destroy();
    }

    @Override
    public void onResume() {
        sessionManager.resume();
        super.onResume();
        if (hasPendingScreenCaptureTask) {
            showLoadingAndCapture(pendingScreenCaptureTelemetryData);
            clearPendingScreenCaptureTask();
        }
    }

    private void updateURL(final String url) {
        if (UrlUtils.isInternalErrorURL(url)) {
            return;
        }

        urlView.setText(UrlUtils.stripUserInfo(url));
        shoppingSearchPromptMessageViewModel.checkIsShoppingSite(url);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_browser, container, false);

        videoContainer = view.findViewById(R.id.video_container);
        browserContainer = view.findViewById(R.id.browser_container);

        mainContent = view.findViewById(R.id.main_content);
        ViewGroup.MarginLayoutParams params = getMarginLayoutParams(mainContent);
        if (params != null) {
            mainContentBottomMargin = params.bottomMargin;
        }

        urlView = view.findViewById(R.id.display_url);

        backgroundView = view.findViewById(R.id.background);
        view.findViewById(R.id.appbar).setOnApplyWindowInsetsListener((v, insets) -> {
            ((RelativeLayout.LayoutParams) v.getLayoutParams()).topMargin = insets.getSystemWindowInsetTop();
            return insets;
        });
        backgroundTransition = (TransitionDrawable) backgroundView.getBackground();

        observeChromeAction();
        setupBottomBar(view);

        toolbarRoot = view.findViewById(R.id.toolbar_root);
        bottomMenuDivider = view.findViewById(R.id.bottom_menu_divider);
        urlBarDivider = view.findViewById(R.id.url_bar_divider);

        siteIdentity = view.findViewById(R.id.site_identity);
        findInPage = new FindInPage(view);

        progressView = view.findViewById(R.id.progress);
        initialiseNormalBrowserUi();

        webViewSlot = view.findViewById(R.id.webview_slot);

        sessionManager = TabsSessionProvider.getOrThrow(getActivity());

        sessionManager.register(this.managerObserver, this, false);

        shoppingSearchViewStub = view.findViewById(R.id.shopping_search_stub);
        observeShoppingSearchPromptMessageViewModel();

        observeNightMode();

        return view;
    }

    private void observeShoppingSearchPromptMessageViewModel() {
        shoppingSearchPromptMessageViewModel.getOpenShoppingSearch().observe(this,
                unit -> {
                    startActivity(ShoppingSearchActivity.Companion.getStartIntent(getContext()));
                    ScreenNavigator.get(getContext()).popToHomeScreen(false);

                    String feed = shoppingSearchPromptMessageViewModel.getMatchedShoppingSiteTitle().getValue();
                    if (feed != null) {
                        TelemetryWrapper.clickTabSwipeDrawer(TelemetryWrapper.Extra_Value.SHOPPING, feed);
                    }
                });

        shoppingSearchPromptMessageViewModel.isUrlShoppingSite().observe(this, isShoppingSite -> {
            int sheetState = BottomSheetBehavior.STATE_COLLAPSED;

            if (isShoppingSite != null && isShoppingSite) {
                if (shoppingSearchViewStub.getParent() != null) {
                    setupShoppingSearchPrompt(shoppingSearchViewStub.inflate());
                }

                sheetState = BottomSheetBehavior.STATE_EXPANDED;
            }

            changeShoppingSearchPromptMessageState(sheetState);
        });

        shoppingSearchPromptMessageViewModel.getShoppingSiteList().observe(this, unit -> {
            shoppingSearchPromptMessageViewModel.checkIsShoppingSite(getUrl());
        });
    }
    private void setupShoppingSearchPrompt(View view) {
        shoppingSearchPromptMessageBehavior = BottomSheetBehavior.from(view.findViewById(R.id.bottom_sheet));

        shoppingSearchBottomSheetSearchBtn = view.findViewById(R.id.bottom_sheet_search);
        shoppingSearchBottomSheetSearchBtn.setOnClickListener(v -> shoppingSearchPromptMessageViewModel.getOpenShoppingSearch().call());
    }

    private void changeShoppingSearchPromptMessageState(int state) {
        if (shoppingSearchPromptMessageBehavior != null) {
            shoppingSearchPromptMessageBehavior.setState(state);
        }
    }

    private void observeChromeAction() {
        chromeViewModel.isTurboModeEnabled().observe(this, this::setContentBlockingEnabled);
        chromeViewModel.isBlockImageEnabled().observe(this, this::setImageBlockingEnabled);
        chromeViewModel.isBlockJavaScriptEnabled().observe(this, this::setJavaScriptBlockingEnabled);
        chromeViewModel.getDoScreenshot().observe(this, telemetryData -> {
            permissionHandler.tryAction(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, ACTION_CAPTURE, telemetryData);
        });
        chromeViewModel.getRefreshOrStop().observe(this, unit -> {
            if (isLoading()) {
                stop();
            } else {
                reload();
            }
        });
        chromeViewModel.getGoNext().observe(this, unit -> {
            if (canGoForward()) {
                goForward();
            }
        });
        chromeViewModel.getShowFindInPage().observe(this, unit -> {
            ScreenNavigator.NavigationState state = chromeViewModel.getNavigationState().getValue();
            if (state != null & state.isBrowser()) {
                showFindInPage();
            }
        });
        chromeViewModel.getCanGoBack().observe(this, unit -> {
            if (canGoBack()) {
                changeShoppingSearchPromptMessageState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    private void observeNightMode() {
        chromeViewModel.isNightMode().observe(this,
                nightModeSettings -> setNightModeEnabled(nightModeSettings.isEnabled()));
    }

    private void setupBottomBar(View rootView) {
        bottomBar = rootView.findViewById(R.id.browser_bottom_bar);
        bottomBar.setOnItemClickListener((type, position) -> {
            switch (type) {
                case BottomBarItemAdapter.TYPE_TAB_COUNTER:
                    chromeViewModel.getShowTabTray().call();
                    TelemetryWrapper.showTabTrayToolbar(WEBVIEW, position);
                    break;
                case BottomBarItemAdapter.TYPE_MENU:
                    chromeViewModel.getShowMenu().call();
                    TelemetryWrapper.showMenuToolbar(WEBVIEW, position);
                    break;
                case BottomBarItemAdapter.TYPE_HOME:
                    chromeViewModel.getShowNewTab().call();
                    TelemetryWrapper.clickAddTabToolbar(WEBVIEW, position);
                    break;
                case BottomBarItemAdapter.TYPE_SEARCH:
                    chromeViewModel.getShowUrlInput().setValue(getUrl());
                    TelemetryWrapper.clickToolbarSearch(WEBVIEW, position);
                    break;
                case BottomBarItemAdapter.TYPE_CAPTURE:
                    chromeViewModel.onDoScreenshot(new ScreenCaptureTelemetryData(WEBVIEW, position));
                    // move Telemetry to ScreenCaptureTask doInBackground() cause we need to init category first.
                    break;
                case BottomBarItemAdapter.TYPE_PIN_SHORTCUT:
                    chromeViewModel.getPinShortcut().call();
                    TelemetryWrapper.clickAddToHome(WEBVIEW, position);
                    break;
                case BottomBarItemAdapter.TYPE_BOOKMARK:
                    boolean isActivated = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_BOOKMARK).getView().isActivated();
                    TelemetryWrapper.clickToolbarBookmark(!isActivated, WEBVIEW, position);
                    chromeViewModel.toggleBookmark();
                    break;
                case BottomBarItemAdapter.TYPE_REFRESH:
                    chromeViewModel.getRefreshOrStop().call();
                    TelemetryWrapper.clickToolbarReload(WEBVIEW, position);
                    break;
                case BottomBarItemAdapter.TYPE_SHARE:
                    chromeViewModel.getShare().call();
                    TelemetryWrapper.clickToolbarShare(WEBVIEW, position);
                    break;
                case BottomBarItemAdapter.TYPE_NEXT:
                    chromeViewModel.getGoNext().call();
                    TelemetryWrapper.clickToolbarForward(WEBVIEW, position);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled bottom bar item, type: " + type);
            }
        });
        bottomBar.setOnItemLongClickListener((type, position) -> {
            if (type == BottomBarItemAdapter.TYPE_MENU) {
                // Long press menu always show download panel
                chromeViewModel.getShowDownloadPanel().call();
                TelemetryWrapper.longPressDownloadIndicator();
                return true;
            }
            return false;
        });
        bottomBarItemAdapter = new BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.Light.INSTANCE);
        bottomBarViewModel.getItems().observe(this, bottomBarItemAdapter::setItems);

        LiveDataExtensionKt.switchFrom(chromeViewModel.isNightMode(), bottomBarViewModel.getItems())
                .observe(this, nightModeSettings -> bottomBarItemAdapter.setNightMode(nightModeSettings.isEnabled()));
        LiveDataExtensionKt.switchFrom(chromeViewModel.getTabCount(), bottomBarViewModel.getItems())
                .observe(this, count -> bottomBarItemAdapter.setTabCount(count, true));
        LiveDataExtensionKt.switchFrom(chromeViewModel.isRefreshing(), bottomBarViewModel.getItems())
                .observe(this, bottomBarItemAdapter::setRefreshing);
        LiveDataExtensionKt.switchFrom(chromeViewModel.getCanGoForward(), bottomBarViewModel.getItems())
                .observe(this, bottomBarItemAdapter::setCanGoForward);
        LiveDataExtensionKt.switchFrom(chromeViewModel.isCurrentUrlBookmarked(), bottomBarViewModel.getItems())
                .observe(this, bottomBarItemAdapter::setBookmark);

        setupDownloadIndicator(rootView);
    }

    private void setupDownloadIndicator(View rootView) {
        final ViewGroup browserRoot = rootView.findViewById(R.id.browser_root_view);

        DownloadIndicatorViewModel downloadIndicatorViewModel =
                ViewModelProviders.of(requireActivity(), new BaseViewModelFactory<>(downloadIndicatorViewModelCreator::get)).get(DownloadIndicatorViewModel.class);
        LiveDataExtensionKt.switchFrom(downloadIndicatorViewModel.getDownloadIndicatorObservable(), bottomBarViewModel.getItems())
                .observe(this, status -> {
                    switch (status) {
                        case DOWNLOADING:
                            bottomBarItemAdapter.setDownloadState(DOWNLOAD_STATE_DOWNLOADING);
                            break;
                        case UNREAD:
                            bottomBarItemAdapter.setDownloadState(DOWNLOAD_STATE_UNREAD);
                            break;
                        case WARNING:
                            bottomBarItemAdapter.setDownloadState(DOWNLOAD_STATE_WARNING);
                            break;
                        case DEFAULT:
                            bottomBarItemAdapter.setDownloadState(DOWNLOAD_STATE_DEFAULT);
                            break;
                    }
                    final Settings.EventHistory eventHistory = Settings.getInstance(getActivity()).getEventHistory();
                    if (!eventHistory.contains(Settings.Event.ShowDownloadIndicatorIntro) && status != DownloadIndicatorViewModel.Status.DEFAULT) {
                        eventHistory.add(Settings.Event.ShowDownloadIndicatorIntro);
                        BottomBar.BottomBarItem menuItem = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_MENU);
                        if (menuItem != null && menuItem.getView() != null) {
                            DownloadIndicatorIntroViewHelper.INSTANCE.initDownloadIndicatorIntroView(this, menuItem.getView(), browserRoot, viewRef -> downloadIndicatorIntro = viewRef);
                        }
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View container, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(container, savedInstanceState);

        // restore WebView state
        if (savedInstanceState != null) {
            // Fragment was destroyed
            // FIXME: Obviously, only restore current tab is not enough
            final Session focusTab = sessionManager.getFocusSession();
            if (focusTab != null) {
                TabView tabView = focusTab.getEngineSession().getTabView();
                if (tabView != null) {
                    tabView.restoreViewState(savedInstanceState);
                } else {
                    // Focus to tab again to force initialization.
                    sessionManager.switchToTab(focusTab.getId());
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        permissionHandler.onActivityResult(getActivity(), requestCode, resultCode, data);
        if (requestCode == FileChooseAction.REQUEST_CODE_CHOOSE_FILE) {
            final boolean done = (fileChooseAction == null) || fileChooseAction.onFileChose(resultCode, data);
            if (done) {
                fileChooseAction = null;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            toolbarRoot.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            bottomMenuDivider.setVisibility(View.GONE);
            updateMainContentBottomMargin(0);
            onLandscapeModeStart();
        } else {
            toolbarRoot.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            bottomMenuDivider.setVisibility(View.VISIBLE);
            updateMainContentBottomMargin(mainContentBottomMargin);
            onLandscapeModeFinish();
        }
    }

    @Nullable
    private ViewGroup.MarginLayoutParams getMarginLayoutParams(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            return (ViewGroup.MarginLayoutParams) params;
        }
        return null;
    }

    private void updateMainContentBottomMargin(int marginBottom) {
        ViewGroup.MarginLayoutParams params = getMarginLayoutParams(mainContent);
        if (params != null) {
            params.bottomMargin = marginBottom;
        }
    }

    private void onLandscapeModeStart() {
        landscapeStartTime = System.currentTimeMillis();
        TelemetryWrapper.enterLandscapeMode();
    }

    private void onLandscapeModeFinish() {
        if (landscapeStartTime == 0L) {
            return;
        }
        long duration = System.currentTimeMillis() - landscapeStartTime;
        TelemetryWrapper.exitLandscapeMode(duration);
        landscapeStartTime = 0L;
    }

    public void goBackground() {
        final Session current = sessionManager.getFocusSession();
        if (current != null) {
            TabViewEngineSession es = current.getEngineSession();
            if (es != null) {
                es.detach();
                final TabView tabView = es.getTabView();
                if (tabView != null) {
                    webViewSlot.removeView(tabView.getView());
                }
            }
        }
    }

    public void goForeground() {
        final Session current = sessionManager.getFocusSession();
        if (webViewSlot.getChildCount() == 0 && current != null) {
            TabViewEngineSession es = current.getEngineSession();
            if (es != null) {
                final TabView tabView = es.getTabView();
                if (tabView != null) {
                    webViewSlot.addView(tabView.getView());
                }
            }
        }
    }

    private void initialiseNormalBrowserUi() {
        urlView.setOnClickListener(v -> {
            chromeViewModel.getShowUrlInput().setValue(getUrl());
            TelemetryWrapper.clickUrlbar();
        });
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        permissionHandler.onSaveInstanceState(outState);
        if (sessionManager.getFocusSession() != null) {
            final TabViewEngineSession es = sessionManager.getFocusSession().getEngineSession();
            if (es != null && es.getTabView() != null) {
                es.getTabView().saveViewState(outState);
            }
        }

        // Workaround for #1107 TransactionTooLargeException
        // since Android N, system throws a exception rather than just a warning(then drop bundle)
        // To set a threshold for dropping WebView state manually
        // refer: https://issuetracker.google.com/issues/37103380
        final String key = "WEBVIEW_CHROMIUM_STATE";
        if (outState.containsKey(key)) {
            final int size = outState.getByteArray(key).length;
            if (size > BUNDLE_MAX_SIZE) {
                outState.remove(key);
            }
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        if (systemVisibility != ViewUtils.SYSTEM_UI_VISIBILITY_NONE) {
            final Session tab = sessionManager.getFocusSession();
            if (tab != null) {
                final TabView tabView = tab.getEngineSession().getTabView();
                if (tabView != null) {
                    tabView.performExitFullScreen();
                }
            }
        }
        dismissGeoDialog();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        sessionManager.unregister(this.managerObserver);
        super.onDestroyView();
    }

    public void setContentBlockingEnabled(boolean enabled) {
        // TODO: Better if we can move this logic to some setting-like classes, and provider interface
        // for configuring blocking function of each tab.
        for (final Session session : sessionManager.getTabs()) {
            final TabViewEngineSession es = session.getEngineSession();
            if (es != null && es.getTabView() != null) {
                es.getTabView().setContentBlockingEnabled(enabled);
            }
        }
    }

    public void setImageBlockingEnabled(boolean enabled) {
        // TODO: Better if we can move this logic to some setting-like classes, and provider interface
        // for configuring blocking function of each tab.
        for (final Session session : sessionManager.getTabs()) {
            final TabViewEngineSession es = session.getEngineSession();
            if (es != null && es.getTabView() != null) {
                es.getTabView().setImageBlockingEnabled(enabled);
            }
        }
    }

    private void setJavaScriptBlockingEnabled(boolean enabled) {
        // TODO: Better if we can move this logic to some setting-like classes, and provider interface
        // for configuring JavaScript blocking function of each tab.
        for (final Session session : sessionManager.getTabs()) {
            final TabViewEngineSession es = session.getEngineSession();
            if (es != null && es.getTabView() != null) {
                es.getTabView().setJavaScriptBlockingEnabled(enabled);
            }
        }
    }

    public interface LoadStateListener {
        void isLoadingChanged(boolean isLoading);
    }

    @VisibleForTesting
    /**
     * Set a (singular) LoadStateListener. Only one listener is supported at any given time. Setting
     * a new listener means any previously set listeners will be dropped. This is only intended
     * to be used by NavigationItemViewHolder. If you want to use this method for any other
     * parts of the codebase, please extend it to handle a list of listeners. (We would also need
     * to automatically clean up expired listeners from that list, probably when adding to that list.)
     *
     * @param listener The listener to notify of load state changes. Only a weak reference will be kept,
     *                 no more calls will be sent once the listener is garbage collected.
     */
    public void setIsLoadingListener(final LoadStateListener listener) {
        loadStateListenerWeakReference = new WeakReference<>(listener);
    }

    @VisibleForTesting
    public void setCaptureStateListener(final CaptureRunnable.CaptureStateListener listener) {
        captureStateListener = listener;
    }

    public CaptureRunnable.CaptureStateListener getCaptureStateListener() {
        return captureStateListener;
    }

    private void setPendingScreenCaptureTask(ScreenCaptureTelemetryData telemetryData) {
        hasPendingScreenCaptureTask = true;
        pendingScreenCaptureTelemetryData = telemetryData;
    }

    private void clearPendingScreenCaptureTask() {
        hasPendingScreenCaptureTask = false;
        pendingScreenCaptureTelemetryData = null;
    }

    private void showLoadingAndCapture(ScreenCaptureTelemetryData telemetryData) {
        if (!isResumed()) {
            return;
        }
        clearPendingScreenCaptureTask();
        final ScreenCaptureDialogFragment capturingFragment = ScreenCaptureDialogFragment.newInstance();

        PortraitStateModel portraitState = getPortraitStateModel();
        if (portraitState != null) {
            portraitState.request(PortraitComponent.ScreenCapture.INSTANCE);
            capturingFragment.addOnDismissListener(dialog ->
                    portraitState.cancelRequest(PortraitComponent.ScreenCapture.INSTANCE));
        }

        capturingFragment.show(getChildFragmentManager(), "capturingFragment");

        final int WAIT_INTERVAL = 150;
        // Post delay to wait for Dialog to show
        HANDLER.postDelayed(new CaptureRunnable(getContext(), this, capturingFragment, getActivity().findViewById(R.id.container), telemetryData), WAIT_INTERVAL);
    }

    private void updateIsLoading(final boolean isLoading) {
        this.isLoading = isLoading;
        final BrowserFragment.LoadStateListener currentListener = loadStateListenerWeakReference.get();
        if (currentListener != null) {
            currentListener.isLoadingChanged(isLoading);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionHandler.onRequestPermissionsResult(getContext(), requestCode, permissions, grantResults);
    }

    /**
     * Use Android's Download Manager to queue this download.
     */
    private void queueDownload(Download download) {
        Activity activity = getActivity();
        if (activity == null || download == null) {
            return;
        }

        new EnqueueDownloadTask(getActivity(), download, getUrl()).execute();
    }

    /*
     * show webview geolocation permission prompt
     */
    private void showGeolocationPermissionPrompt() {
        if (!isPopupWindowAllowed()) {
            return;
        }

        if (geolocationCallback == null) {
            return;
        }
        if (geoDialog != null && geoDialog.isShowing()) {
            return;
        }

        Boolean allowed = GeoPermissionCache.getAllowed(geolocationOrigin);
        if (allowed != null) {
            geolocationCallback.invoke(geolocationOrigin, allowed, false);
        } else {
            geoDialog = buildGeoPromptDialog();
            geoDialog.show();
        }
    }

    public void dismissGeoDialog() {
        if (geoDialog != null) {
            geoDialog.dismiss();
            geoDialog = null;
        }
    }

    @VisibleForTesting
    public AlertDialog buildGeoPromptDialog() {
        View customContent = LayoutInflater.from(getContext()).inflate(R.layout.dialog_permission_request, null);
        CheckedTextView checkBox = customContent.findViewById(R.id.cache_my_decision);
        checkBox.setText(getString(R.string.geolocation_dialog_message_cache_it, getString(R.string.app_name)));
        checkBox.setOnClickListener(v -> {
            checkBox.toggle();
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(customContent)
                .setMessage(getString(R.string.geolocation_dialog_message, geolocationOrigin))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.geolocation_dialog_allow), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        acceptGeoRequest(checkBox.isChecked());
                    }
                })
                .setNegativeButton(getString(R.string.geolocation_dialog_block), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        rejectGeoRequest(checkBox.isChecked());
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        rejectGeoRequest(false);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        rejectGeoRequest(false);
                    }
                });
        return builder.create();
    }

    private void acceptGeoRequest(boolean cacheIt) {
        if (geolocationCallback == null) {
            return;
        }
        if (cacheIt) {
            GeoPermissionCache.putAllowed(geolocationOrigin, Boolean.TRUE);
        }
        geolocationCallback.invoke(geolocationOrigin, true, false);
        geolocationOrigin = "";
        geolocationCallback = null;
    }

    private void rejectGeoRequest(boolean cacheIt) {
        if (geolocationCallback == null) {
            return;
        }
        if (cacheIt) {
            GeoPermissionCache.putAllowed(geolocationOrigin, Boolean.FALSE);
        }
        geolocationCallback.invoke(geolocationOrigin, false, false);
        geolocationOrigin = "";
        geolocationCallback = null;
    }

    private boolean isStartedFromExternalApp() {
        final Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        // No SafeIntent needed here because intent.getAction() is safe (SafeIntent simply calls intent.getAction()
        // without any wrapping):
        final Intent intent = activity.getIntent();
        boolean isFromInternal = intent != null && intent.getBooleanExtra(IntentUtils.EXTRA_IS_INTERNAL_REQUEST, false);
        return intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && !isFromInternal;
    }

    public boolean onBackPressed() {
        if (findInPage.onBackPressed()) {
            return true;
        }

        if (canGoBack()) {
            // Go back in web history
            goBack();
        } else {
            final Session focus = sessionManager.getFocusSession();
            if (focus == null) {
                return false;
            } else if (focus.isFromExternal() || focus.hasParentTab()) {
                sessionManager.closeTab(focus.getId());
            } else {
                ScreenNavigator.get(getContext()).popToHomeScreen(true);
            }
        }

        return true;
    }

    /**
     * @param url                 target url
     * @param openNewTab          whether to load url in a new tab or not
     * @param isFromExternal      if this url is started from external VIEW intent
     * @param onViewReadyCallback callback to notify that web view is ready for showing.
     */
    public void loadUrl(@NonNull final String url, boolean openNewTab,
                        boolean isFromExternal, final Runnable onViewReadyCallback) {
        updateURL(url);
        if (SupportUtils.isUrl(url)) {
            if (openNewTab) {
                sessionManager.addTab(url, TabUtil.argument(null, isFromExternal, true));
                // Per spec, if download indicator intro view is showed when new tabb is opened, just dismiss it anyway.
                dismissDownloadIndicatorIntroView();
                // In case we call SessionManager#addTab(), which is an async operation calls back in the next
                // message loop. By posting this runnable we can call back in the same message loop with
                // TabsContentListener#onFocusChanged(), which is when the view is ready and being attached.
                ThreadUtils.postToMainThread(onViewReadyCallback);
            } else {
                Session currentTab = sessionManager.getFocusSession();
                if (currentTab != null && currentTab.getEngineSession().getTabView() != null) {
                    currentTab.getEngineSession().getTabView().loadUrl(url);
                    onViewReadyCallback.run();
                } else {
                    sessionManager.addTab(url, TabUtil.argument(null, isFromExternal, true));
                    ThreadUtils.postToMainThread(onViewReadyCallback);
                }
            }
        } else if (AppConstants.isDevBuild()) {
            // throw exception to highlight this issue, except release build.
            throw new RuntimeException("trying to open a invalid url: " + url);
        }
    }

    public void switchToTab(final String tabId) {
        if (!TextUtils.isEmpty(tabId)) {
            sessionManager.switchToTab(tabId);
        }
    }

    @NonNull
    public String getUrl() {
        // getUrl() is used for things like sharing the current URL. We could try to use the webview,
        // but sometimes it's null, and sometimes it returns a null URL. Sometimes it returns a data:
        // URL for error pages. The URL we show in the toolbar is (A) always correct and (B) what the
        // user is probably expecting to share, so lets use that here:
        return urlView.getText().toString();
    }

    public boolean canGoForward() {
        return sessionManager.getFocusSession() != null
                && sessionManager.getFocusSession().getCanGoForward();
    }

    public boolean isLoading() {
        return isLoading;
    }

    public boolean canGoBack() {
        return sessionManager.getFocusSession() != null
                && sessionManager.getFocusSession().getCanGoBack();
    }

    public void goBack() {
        final Session currentTab = sessionManager.getFocusSession();
        if (currentTab != null) {
            final TabView current = currentTab.getEngineSession().getTabView();
            // The Session.canGoBack property is mainly for UI display purpose and is only sampled
            // at onNavigationStateChange which is called at onPageFinished, onPageStarted and
            // onReceivedTitle. We do some sanity check here.
            if (current == null || !current.canGoBack()) {
                return;
            }
            WebBackForwardList webBackForwardList = ((WebView) current).copyBackForwardList();
            WebHistoryItem item = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() - 1);
            updateURL(item.getUrl());
            current.goBack();
        }
    }

    public void goForward() {
        final Session currentTab = sessionManager.getFocusSession();
        if (currentTab != null) {
            final TabView current = currentTab.getEngineSession().getTabView();
            if (current == null) {
                return;
            }
            WebBackForwardList webBackForwardList = ((WebView) current).copyBackForwardList();
            WebHistoryItem item = webBackForwardList.getItemAtIndex(webBackForwardList.getCurrentIndex() + 1);
            updateURL(item.getUrl());
            current.goForward();
        }
    }

    public void reload() {
        final Session currentTab = sessionManager.getFocusSession();
        if (currentTab != null) {
            final TabView current = currentTab.getEngineSession().getTabView();
            if (current == null) {
                return;
            }
            current.reload();
        }
    }

    public void stop() {
        final Session currentTab = sessionManager.getFocusSession();
        if (currentTab != null) {
            final TabView current = currentTab.getEngineSession().getTabView();
            if (current == null) {
                return;
            }
            current.stopLoading();
        }
    }

    public interface ScreenshotCallback {
        void onCaptureComplete(String title, String url, Bitmap bitmap);
    }

    public boolean capturePage(@NonNull ScreenshotCallback callback) {
        final Session currentTab = sessionManager.getFocusSession();
        // Failed to get WebView
        if (currentTab == null) {
            return false;
        }
        final TabView current = currentTab.getEngineSession().getTabView();
        if (current == null || !(current instanceof WebView)) {
            return false;
        }
        WebView webView = (WebView) current;
        Bitmap content = getPageBitmap(webView);
        // Failed to capture
        if (content == null) {
            return false;
        }
        callback.onCaptureComplete(current.getTitle(), current.getUrl(), content);
        return true;
    }

    public void dismissWebContextMenu() {
        if (webContextMenu != null) {
            webContextMenu.dismiss();
            webContextMenu = null;
        }
    }

    private Bitmap getPageBitmap(WebView webView) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        try {
            Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), (int) (webView.getContentHeight() * displaymetrics.density), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);
            return bitmap;
            // OOM may occur, even if OOMError is not thrown, operations during Bitmap creation may
            // throw other Exceptions such as NPE when the bitmap is very large.
        } catch (Exception | OutOfMemoryError ex) {
            return null;
        }

    }

    private boolean isPopupWindowAllowed() {
        return ScreenNavigator.get(getContext()).isBrowserInForeground() && !TabTray.isShowing(getFragmentManager());
    }

    public void showFindInPage() {
        final Session focusTab = sessionManager.getFocusSession();
        if (focusTab != null) {
            PortraitStateModel portraitState = getPortraitStateModel();
            if (portraitState != null) {
                portraitState.request(PortraitComponent.FindInPage.INSTANCE);
                findInPage.setOnDismissListener(view -> {
                    portraitState.cancelRequest(PortraitComponent.FindInPage.INSTANCE);
                    return null;
                });
            }

            findInPage.show(focusTab);
            TelemetryWrapper.findInPage(TelemetryWrapper.FIND_IN_PAGE.OPEN_BY_MENU);
        }
    }

    private PortraitStateModel getPortraitStateModel() {
        Activity activity = getActivity();
        if (activity == null) {
            return null;
        }

        if (activity instanceof MainActivity) {
            return ((MainActivity) activity).getPortraitStateModel();
        } else {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("Only MainActivity has portrait state model");
            } else {
                return null;
            }
        }
    }

    private void hideFindInPage() {
        findInPage.hide();
    }

    class SessionObserver implements Session.Observer, TabViewEngineSession.Client {
        @Nullable
        private Session session;
        private HistoryInserter historyInserter = new HistoryInserter();

        // Some url may report progress from 0 again for the same url. filter them out to avoid
        // progress bar regression when scrolling.
        private String loadedUrl = null;


        @Override
        public void onLoadingStateChanged(@NonNull Session session, boolean loading) {
            BrowserFragment.this.isLoading = loading;
            if (loading) {
                historyInserter.onTabStarted(session);
            } else {
                historyInserter.onTabFinished(session);
            }

            if (!isForegroundSession(session)) {
                return;
            }

            if (loading) {
                loadedUrl = null;
                chromeViewModel.onPageLoadingStarted();
                updateIsLoading(true);
                updateURL(session.getUrl());
                backgroundTransition.resetTransition();
            } else {
                // The URL which is supplied in onTabFinished() could be fake (see #301), but webview's
                // URL is always correct _except_ for error pages
                updateUrlFromWebView(session);
                chromeViewModel.onPageLoadingStopped();
                updateIsLoading(false);
                backgroundTransition.startTransition(ANIMATION_DURATION);
            }
        }

        @Override
        public void onSecurityChanged(@NonNull Session session, boolean isSecure) {
            siteIdentity.setImageLevel(isSecure ? SITE_LOCK : SITE_GLOBE);
        }

        @Override
        public void onUrlChanged(@NonNull Session session, @Nullable String url) {
            chromeViewModel.onFocusedUrlChanged(url);
            if (!isForegroundSession(session)) {
                return;
            }
            updateURL(url);
        }

        @Override
        public boolean handleExternalUrl(@Nullable String url) {
            if (getContext() == null) {
                Log.w(BROWSER_FRAGMENT_TAG, "No context to use, abort callback handleExternalUrl");
                return false;
            }

            return IntentUtils.handleExternalUri(getContext(), url);
        }

        @Override
        public void updateFailingUrl(@Nullable String url, boolean updateFromError) {
            if (session == null) {
                return;
            }
            historyInserter.updateFailingUrl(session, url, updateFromError);
        }

        @Override
        public void onProgress(@NonNull Session session, int progress) {
            if (!isForegroundSession(session)) {
                return;
            }

            hideFindInPage();
            if (sessionManager.getFocusSession() != null) {
                final String currentUrl = sessionManager.getFocusSession().getUrl();
                final boolean progressIsForLoadedUrl = TextUtils.equals(currentUrl, loadedUrl);
                // Some new url may give 100 directly and then start from 0 again. don't treat
                // as loaded for these urls;
                final boolean urlBarLoadingToFinished =
                        progressView.getMax() != progressView.getProgress() && progress == progressView.getMax();
                if (urlBarLoadingToFinished) {
                    loadedUrl = currentUrl;
                }
                if (progressIsForLoadedUrl) {
                    return;
                }
            }
            progressView.setProgress(progress);
        }

        @Override
        public boolean onShowFileChooser(@NonNull TabViewEngineSession es,
                                         @NonNull ValueCallback<Uri[]> filePathCallback,
                                         @NonNull WebChromeClient.FileChooserParams fileChooserParams) {
            if (!isForegroundSession(session)) {
                return false;
            }

            TelemetryWrapper.browseFilePermissionEvent();
            try {
                BrowserFragment.this.fileChooseAction = new FileChooseAction(BrowserFragment.this, filePathCallback, fileChooserParams);
                permissionHandler.tryAction(BrowserFragment.this, Manifest.permission.READ_EXTERNAL_STORAGE, BrowserFragment.ACTION_PICK_FILE, null);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        public void onTitleChanged(@NonNull Session session, @Nullable String title) {
            chromeViewModel.onFocusedTitleChanged(title);
            if (session == null) {
                return;
            }
            if (!isForegroundSession(session)) {
                return;
            }
            if (!BrowserFragment.this.getUrl().equals(session.getUrl())) {
                updateURL(session.getUrl());
            }
        }

        @Override
        public void onReceivedIcon(@Nullable Bitmap icon) {
        }

        @Override
        public void onLongPress(@NonNull Session session, @NonNull TabView.HitTarget hitTarget) {
            if (getActivity() == null) {
                Log.w(BROWSER_FRAGMENT_TAG, "No context to use, abort callback onLongPress");
                return;
            }

            webContextMenu = WebContextMenu.show(false, getActivity(), downloadCallback, hitTarget);
        }

        @Override
        public void onEnterFullScreen(@NonNull TabView.FullscreenCallback callback,
                                      @Nullable View fullscreenContentView) {
            if (session == null) {
                return;
            }
            if (!isForegroundSession(session)) {
                callback.fullScreenExited();
                return;
            }

            fullscreenCallback = callback;

            if (session.getEngineSession().getTabView() != null && fullscreenContentView != null) {
                // Hide browser UI and web content
                browserContainer.setVisibility(View.INVISIBLE);

                // Add view to video container and make it visible
                final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                videoContainer.addView(fullscreenContentView, params);
                videoContainer.setVisibility(View.VISIBLE);

                // Switch to immersive mode: Hide system bars other UI controls
                systemVisibility = ViewUtils.switchToImmersiveMode(getActivity());
            }
        }

        @Override
        public void onExitFullScreen() {
            if (session == null) {
                return;
            }
            // Remove custom video views and hide container
            videoContainer.removeAllViews();
            videoContainer.setVisibility(View.GONE);

            // Show browser UI and web content again
            browserContainer.setVisibility(View.VISIBLE);

            if (systemVisibility != ViewUtils.SYSTEM_UI_VISIBILITY_NONE) {
                ViewUtils.exitImmersiveMode(systemVisibility, getActivity());
            }

            // Notify renderer that we left fullscreen mode.
            if (fullscreenCallback != null) {
                fullscreenCallback.fullScreenExited();
                fullscreenCallback = null;
            }

            // WebView gets focus, but unable to open the keyboard after exit Fullscreen for Android 7.0+
            // We guess some component in WebView might lock focus
            // So when user touches the input text box on Webview, it will not trigger to open the keyboard
            // It may be a WebView bug.
            // The workaround is clearing WebView focus
            // The WebView will be normal when it gets focus again.
            // If android change behavior after, can remove this.
            if (session.getEngineSession().getTabView() instanceof WebView) {
                ((WebView) session.getEngineSession().getTabView()).clearFocus();
            }
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(@NonNull String origin,
                                                       @Nullable GeolocationPermissions.Callback callback) {
            if (session == null) {
                return;
            }
            if (!isForegroundSession(session) || !isPopupWindowAllowed()) {
                return;
            }

            geolocationOrigin = origin;
            geolocationCallback = callback;
            permissionHandler.tryAction(BrowserFragment.this,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    ACTION_GEO_LOCATION,
                    null);
        }

        void changeSession(@Nullable Session nextSession) {
            if (this.session != null) {
                this.session.unregister(this);
            }
            this.session = nextSession;
            if (this.session != null) {
                this.session.register(this);
            }
        }


        private void updateUrlFromWebView(@NonNull Session source) {
            if (sessionManager.getFocusSession() != null) {
                final String viewURL = sessionManager.getFocusSession().getUrl();
                onUrlChanged(source, viewURL);
            }
        }

        private boolean isForegroundSession(Session tab) {
            return sessionManager.getFocusSession() == tab;
        }

        @Override
        public void onFindResult(@NotNull Session session, @NotNull mozilla.components.browser.session.Session.FindResult result) {
            findInPage.onFindResultReceived(result);
        }

        @Override
        public boolean onDownload(@NotNull Session session, @NotNull mozilla.components.browser.session.Download download) {
            FragmentActivity activity = getActivity();
            if (activity == null
                    || !activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                return false;
            }

            Download d = new Download(download.getUrl(),
                    download.getFileName(),
                    download.getUserAgent(),
                    "",
                    download.getContentType(),
                    download.getContentLength(),
                    false);
            permissionHandler.tryAction(BrowserFragment.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, ACTION_DOWNLOAD, d);
            return true;
        }

        @Override
        public void onNavigationStateChanged(@NotNull Session session, boolean canGoBack, boolean canGoForward) {
            chromeViewModel.onNavigationStateChanged(canGoBack, canGoForward);
        }

        @Override
        public void onHttpAuthRequest(@NotNull TabViewClient.HttpAuthCallback callback, @Nullable String host, @Nullable String realm) {
            HttpAuthenticationDialogBuilder builder = new HttpAuthenticationDialogBuilder.Builder(getActivity(), host, realm)
                    .setOkListener((host1, realm1, username, password) -> callback.proceed(username, password))
                    .setCancelListener(callback::cancel)
                    .build();

            builder.createDialog();
            builder.show();
        }
    }

    class SessionManagerObserver implements SessionManager.Observer {
        private ValueAnimator tabTransitionAnimator;
        private SessionObserver sessionObserver;

        SessionManagerObserver(SessionObserver observer) {
            this.sessionObserver = observer;
        }

        @Override
        public void onFocusChanged(@Nullable final Session tab, SessionManager.Factor factor) {
            chromeViewModel.onFocusedUrlChanged(tab != null ? tab.getUrl() : null);
            chromeViewModel.onFocusedTitleChanged(tab != null ? tab.getTitle() : null);
            if (tab == null) {
                if (factor == SessionManager.Factor.FACTOR_NO_FOCUS && !isStartedFromExternalApp()) {
                    ScreenNavigator.get(getContext()).popToHomeScreen(true);
                } else {
                    getActivity().finish();
                }
            } else {
                transitToTab(tab);
                refreshChrome(tab);
            }
        }

        @Override
        public void onSessionAdded(@NonNull final Session tab, @Nullable final Bundle arguments) {
            if (arguments == null) {
                return;
            }

            int src = arguments.getInt(EXTRA_NEW_TAB_SRC, -1);
            switch (src) {
                case SRC_CONTEXT_MENU:
                    onTabAddedByContextMenu(tab, arguments);
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onSessionCountChanged(int count) {
            chromeViewModel.onTabCountChanged(count);
        }

        private void transitToTab(@NonNull Session targetTab) {
            final TabView tabView = targetTab.getEngineSession().getTabView();
            if (tabView == null) {
                throw new RuntimeException("Tabview should be created at this moment and never be null");
            }
            // ensure it does not have attach to parent earlier.
            if (targetTab.getEngineSession() != null) {
                targetTab.getEngineSession().detach();
            }

            @Nullable final View outView = findExistingTabView(webViewSlot);
            webViewSlot.removeView(outView);

            final View inView = tabView.getView();
            webViewSlot.addView(inView);

            this.sessionObserver.changeSession(targetTab);

            startTransitionAnimation(null, inView, null);
        }

        private void refreshChrome(Session tab) {
            geolocationOrigin = "";
            geolocationCallback = null;

            dismissGeoDialog();

            updateURL(tab.getUrl());
            progressView.setProgress(0);

            int identity = (tab.getSecurityInfo().getSecure()) ? SITE_LOCK : SITE_GLOBE;
            siteIdentity.setImageLevel(identity);

            hideFindInPage();

            Session current = sessionManager.getFocusSession();
            if (current != null) {
                chromeViewModel.onNavigationStateChanged(canGoBack(), canGoForward());
            }
            // check if newer config exists whenever navigating to Browser screen
            bottomBarViewModel.refresh();
        }

        @SuppressWarnings("SameParameterValue")
        private void startTransitionAnimation(@Nullable final View outView, @NonNull final View inView,
                                              @Nullable final Runnable finishCallback) {
            stopTabTransition();

            inView.setAlpha(0f);
            if (outView != null) {
                outView.setAlpha(1f);
            }

            int duration = inView.getResources().getInteger(R.integer.tab_transition_time);
            tabTransitionAnimator = ValueAnimator.ofFloat(0, 1).setDuration(duration);
            tabTransitionAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float alpha = (float) animation.getAnimatedValue();
                    if (outView != null) {
                        outView.setAlpha(1 - alpha);
                    }
                    inView.setAlpha(alpha);
                }
            });
            tabTransitionAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (finishCallback != null) {
                        finishCallback.run();
                    }
                    inView.setAlpha(1f);
                    if (outView != null) {
                        outView.setAlpha(1f);
                    }
                }
            });
            tabTransitionAnimator.start();
        }

        @Nullable
        private View findExistingTabView(ViewGroup parent) {
            int viewCount = parent.getChildCount();
            for (int childIdx = 0; childIdx < viewCount; ++childIdx) {
                View childView = parent.getChildAt(childIdx);
                if (childView instanceof TabView) {
                    return ((TabView) childView).getView();
                }
            }
            return null;
        }

        private void stopTabTransition() {
            if (tabTransitionAnimator != null && tabTransitionAnimator.isRunning()) {
                tabTransitionAnimator.end();
            }
        }

        private void onTabAddedByContextMenu(@NonNull final Session tab, @NonNull Bundle arguments) {
            if (!TabUtil.toFocus(arguments)) {
                Snackbar.make(webViewSlot, R.string.new_background_tab_hint, Snackbar.LENGTH_LONG)
                        .setAction(R.string.new_background_tab_switch, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                sessionManager.switchToTab(tab.getId());
                            }
                        }).show();
            }
        }

        @Override
        public void updateFailingUrl(@org.jetbrains.annotations.Nullable String url, boolean updateFromError) {
            sessionObserver.updateFailingUrl(url, updateFromError);
        }

        @Override
        public boolean handleExternalUrl(@org.jetbrains.annotations.Nullable String url) {
            return sessionObserver.handleExternalUrl(url);
        }

        @Override
        public boolean onShowFileChooser(@NotNull TabViewEngineSession es, @org.jetbrains.annotations.Nullable ValueCallback<Uri[]> filePathCallback, @org.jetbrains.annotations.Nullable WebChromeClient.FileChooserParams fileChooserParams) {
            return sessionObserver.onShowFileChooser(es, filePathCallback, fileChooserParams);
        }

        @Override
        public void onHttpAuthRequest(@NotNull TabViewClient.HttpAuthCallback callback, @Nullable String host, @Nullable String realm) {
            sessionObserver.onHttpAuthRequest(callback, host, realm);
        }
    }

    class DownloadCallback implements org.mozilla.rocket.tabs.web.DownloadCallback {

        @Override
        public void onDownloadStart(@NonNull Download download) {
            FragmentActivity activity = getActivity();
            if (activity == null
                    || !activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
                return;
            }

            permissionHandler.tryAction(BrowserFragment.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, ACTION_DOWNLOAD, download);
        }
    }

    /**
     * TODO: This class records some intermediate data of each tab to avoid inserting duplicate
     * history, maybe it'd be better to make these data as per-tab data
     */
    private final class HistoryInserter {
        private WeakHashMap<Session, String> failingUrls = new WeakHashMap<>();

        // Some url may have two onPageFinished for the same url. filter them out to avoid
        // adding twice to the history.
        private WeakHashMap<Session, String> lastInsertedUrls = new WeakHashMap<>();

        void onTabStarted(@NonNull Session tab) {
            lastInsertedUrls.remove(tab);
        }

        void onTabFinished(@NonNull Session tab) {
            insertBrowsingHistory(tab);
        }

        void updateFailingUrl(@NonNull Session tab, String url, boolean updateFromError) {
            String failingUrl = failingUrls.get(tab);
            if (!updateFromError && !url.equals(failingUrl)) {
                failingUrls.remove(tab);
            } else {
                failingUrls.put(tab, url);
            }
        }

        private void insertBrowsingHistory(Session tab) {
            String urlToBeInserted = getUrl();
            @NonNull String lastInsertedUrl = getLastInsertedUrl(tab);

            if (TextUtils.isEmpty(urlToBeInserted)) {
                return;
            }

            if (urlToBeInserted.equals(getFailingUrl(tab))) {
                return;
            }

            if (urlToBeInserted.equals(lastInsertedUrl)) {
                return;
            }

            TabView tabView = tab.getEngineSession().getTabView();
            if (tabView != null) {
                tabView.insertBrowsingHistory();
            }
            lastInsertedUrls.put(tab, urlToBeInserted);
        }

        private String getFailingUrl(Session tab) {
            String url = failingUrls.get(tab);
            return TextUtils.isEmpty(url) ? "" : url;
        }

        private String getLastInsertedUrl(Session tab) {
            String url = lastInsertedUrls.get(tab);
            return TextUtils.isEmpty(url) ? "" : url;
        }
    }

    public void checkToShowMyShotOnBoarding() {
        chromeViewModel.checkToShowMyShotOnBoarding();
    }

    @Override
    public Fragment getFragment() {
        return this;
    }

    private void setNightModeEnabled(boolean enable) {
        browserContainer.setNightMode(enable);

        toolbarRoot.setNightMode(enable);
        urlView.setNightMode(enable);
        siteIdentity.setNightMode(enable);

        bottomMenuDivider.setNightMode(enable);
        backgroundView.setNightMode(enable);
        urlBarDivider.setNightMode(enable);

        ViewUtils.updateStatusBarStyle(!enable, getActivity().getWindow());
    }

    private void dismissDownloadIndicatorIntroView() {
        if (downloadIndicatorIntro != null) {
            downloadIndicatorIntro.setVisibility(View.GONE);
        }
    }
}

