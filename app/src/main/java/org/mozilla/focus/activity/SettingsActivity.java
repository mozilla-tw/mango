/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavHost;
import androidx.navigation.NavInflater;

import org.mozilla.focus.R;
import org.mozilla.rocket.content.portal.ContentFeature;

public class SettingsActivity extends BaseActivity {
    public static final int ACTIVITY_RESULT_LOCALE_CHANGED = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //  Init navigation host with customized startDestination
        dispatchNavigation(getIntent());

        // Ensure all locale specific Strings are initialised on first run, we don't set the title
        // anywhere before now (the title can only be set via AndroidManifest, and ensuring
        // that that loads the correct locale string is tricky).
        applyLocale();
    }

    private void dispatchNavigation(Intent intent) {
        final NavHost navHost = (NavHost) getSupportFragmentManager().findFragmentById(R.id.container);
        final NavController navController = navHost.getNavController();
        final NavInflater navInflater = navController.getNavInflater();
        final NavGraph navGraph = navInflater.inflate(R.navigation.nav_settings);
        if (intent != null && intent.getStringExtra(ContentFeature.EXTRA_CONFIG_NEWS) != null) {
            navGraph.setStartDestination(R.id.settings_news);
        } else {
            navGraph.setStartDestination(R.id.settings_root);
        }
        navController.setGraph(navGraph);
    }

    @Override
    public void applyLocale() {
        setTitle(R.string.menu_settings);
    }
}
