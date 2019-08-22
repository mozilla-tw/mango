/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Keep;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.RecyclerViewTestUtils;
import org.mozilla.rocket.content.ExtentionKt;

import java.util.List;
import java.util.Random;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.mozilla.focus.utils.RecyclerViewTestUtils.atPosition;

@Keep
@RunWith(AndroidJUnit4.class)
public class RemoveTopSitesTest {

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private List<Site> siteList;
    private Context context;
    private String removeLabel;

    @Before
    public void setUp() throws JSONException {
        AndroidTestUtils.beforeTest();
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        removeLabel = context.getString(R.string.remove);
        prepareTopSiteList();
        activityTestRule.launchActivity(new Intent());
    }

    /**
     * Test case no: TC0086
     * Test case name: One top site deleted
     * Steps:
     * 1. Launch app
     * 2. long click to delete top site
     */
    @Test
    @Ignore("fix this after top sites implemented")
    public void deleteTopSite_deleteSuccessfully() {

        // Pick a test site to delete
        final int siteIndex = new Random().nextInt(siteList.size());
        final Site testSite = siteList.get(siteIndex);

        onView(withId(R.id.main_list)).check(matches(isDisplayed()));

        // Check the title of test site is matched
        onView(withId(R.id.main_list))
                .check(matches(atPosition(siteIndex, hasDescendant(withText(testSite.getTitle())))));

        // Long click the test site
        onView(ViewMatchers.withId(R.id.main_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(siteIndex, longClick()));

        // Check the remove button is displayed
        onView(withText(removeLabel)).check(matches(isDisplayed()));

        // Click the remove button
        onView(withText(removeLabel))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // Check the test site is removed
        onView(withId(R.id.main_list))
                .check(matches(not(atPosition(siteIndex, hasDescendant(withText(testSite.getTitle()))))));
    }

    /**
     * Test case no: TC0172
     * Test case name: cancel removing top site action
     * Steps:
     * 1. Launch app
     * 2. Long click to remove one top site
     * 3. Press back key
     */
    @Test
    @Ignore("fix this after top sites implemented")
    public void deleteTopSiteAndCancel_topSiteIsStillThere() {

        // Pick a test site to test
        final int siteIndex = new Random().nextInt(siteList.size());
        final Site testSite = siteList.get(siteIndex);

        onView(withId(R.id.main_list)).check(matches(isDisplayed()));

        // Check the title of test site is matched
        onView(withId(R.id.main_list))
                .check(matches(atPosition(siteIndex, hasDescendant(withText(testSite.getTitle())))));

        // Long click the test site
        onView(ViewMatchers.withId(R.id.main_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(siteIndex, longClick()));

        // Check the remove button is displayed
        onView(withText(removeLabel))
                .check(matches(isDisplayed()));

        // Press the back key
        Espresso.pressBack();

        // Check the title of test site is matched
        onView(withId(R.id.main_list))
                .check(matches(atPosition(siteIndex, hasDescendant(withText(testSite.getTitle())))));
    }

    /**
     * Test case no: TC0007
     * Test case name: All top sites are deleted sequentially
     * Steps:
     * 1. Launch app
     * 2. remove top site
     * 3. repeat step 2 until all topsites removed
     * 4. exit app
     * 5. relaunch app
     */
    @Test
    @Ignore("fix this after top sites implemented")
    public void deleteAllTopSitesAndRelaunchApp_defaultTopSitesAreLoaded() {

        // Get the count of top sites
        final int countTopSite = RecyclerViewTestUtils.getCountFromRecyclerView(R.id.main_list);

        // Iterate each top site and delete it
        for (int i = 0; i < countTopSite; i++) {
            // Long click the first top site
            onView(withId(R.id.main_list))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, longClick()));

            // Check the remove button is displayed
            onView(withText(removeLabel)).check(matches(isDisplayed()));

            // Click the remove button
            onView(withText(removeLabel))
                    .inRoot(RootMatchers.isPlatformPopup())
                    .perform(click());
        }

        // Exit app
        Espresso.pressBackUnconditionally();

        // Relaunch app
        activityTestRule.launchActivity(new Intent());

        // Check if default top sites are loaded again
        Assert.assertTrue(countTopSite == RecyclerViewTestUtils.getCountFromRecyclerView(R.id.main_list));

    }

    private void prepareTopSiteList() {
        siteList =  ExtentionKt.appComponent((Context) getApplicationContext())
                .topSitesRepo().getDefaultSites();

        Assert.assertNotNull(siteList);
        Assert.assertTrue(siteList.size() > 0);
    }

}