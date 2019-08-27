package org.mozilla.rocket.persistance.History;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.provider.HistoryContract;
import org.mozilla.focus.provider.HistoryDatabaseHelper;
import org.mozilla.rocket.home.topsites.data.TopSitesRepo;

// TODO: 8/23/18
// We're only utilizing Room to migrate, but we have not yet remove the classic / old school
// cursor based HistoryProvider due to schedule. This should be fixed with another re-schema.
// Current plan is to build something like the Place system in Firefox 3
// https://developer.mozilla.org/en-US/docs/Mozilla/Tech/Places/Database

@Database(entities = {Site.class}, version = 3)
public abstract class HistoryDatabase extends RoomDatabase {

    private static final String CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ";
    public static final String CREATE_LEGACY_IF_NOT_EXIST = CREATE_TABLE_IF_NOT_EXISTS +
            HistoryDatabaseHelper.Tables.BROWSING_HISTORY_LEGACY + " (" +
            HistoryContract.BrowsingHistory._ID + " INTEGER PRIMARY KEY NOT NULL," +
            HistoryContract.BrowsingHistory.URL + " TEXT NOT NULL," +
            HistoryContract.BrowsingHistory.FAV_ICON + " BLOB" +
            ");";

    private static volatile HistoryDatabase instance;

    public static HistoryDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (HistoryDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            HistoryDatabase.class, "history.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return instance;
    }

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {

        @Override
        public void migrate(SupportSQLiteDatabase database) {
            final String BROWSING_HISTORY_NEW = "browsing_history_new";

            database.beginTransaction();
            try {
                // Create a legacy table for future migration that cannot be done here since we
                // don't have context. This table will be cleared once user have launched
                // HomeFragment. See org.mozilla.focus.home.HomeFragment.MigrateHistoryRunnable
                database.execSQL(CREATE_LEGACY_IF_NOT_EXIST);
                // Migrate history.
                final int PAGE_SIZE_AT_MIGRATION_VERSION = 50;
                database.execSQL(
                        "INSERT INTO " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY_LEGACY + " (" + HistoryContract.BrowsingHistory._ID +
                                ", " + HistoryContract.BrowsingHistory.FAV_ICON + ", " + HistoryContract.BrowsingHistory.URL +
                                ") SELECT " + HistoryContract.BrowsingHistory._ID +
                                ", " + HistoryContract.BrowsingHistory.FAV_ICON + ", " + HistoryContract.BrowsingHistory.URL +
                                " FROM " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY +
                                " ORDER BY " + HistoryContract.BrowsingHistory.LAST_VIEW_TIMESTAMP + " DESC" +
                                " LIMIT " + PAGE_SIZE_AT_MIGRATION_VERSION);
                // Migrate Top sites.
                database.execSQL(
                        "INSERT OR REPLACE INTO " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY_LEGACY + " (" + HistoryContract.BrowsingHistory._ID +
                                ", " + HistoryContract.BrowsingHistory.FAV_ICON + ", " + HistoryContract.BrowsingHistory.URL +
                                ") SELECT " + HistoryContract.BrowsingHistory._ID +
                                ", " + HistoryContract.BrowsingHistory.FAV_ICON + ", " + HistoryContract.BrowsingHistory.URL +
                                " FROM " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY +
                                " WHERE " + HistoryContract.BrowsingHistory.VIEW_COUNT + " > " + TopSitesRepo.TOP_SITES_QUERY_MIN_VIEW_COUNT +
                                " ORDER BY " + HistoryContract.BrowsingHistory.VIEW_COUNT +
                                // We migrate twice the amount in case user removes top site.
                                " LIMIT " + TopSitesRepo.TOP_SITES_QUERY_LIMIT * 2);
                database.execSQL(CREATE_TABLE_IF_NOT_EXISTS + BROWSING_HISTORY_NEW + " (" +
                        HistoryContract.BrowsingHistory._ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        HistoryContract.BrowsingHistory.TITLE + " TEXT," +
                        HistoryContract.BrowsingHistory.URL + " TEXT NOT NULL," +
                        HistoryContract.BrowsingHistory.VIEW_COUNT + " INTEGER NOT NULL DEFAULT 1," +
                        HistoryContract.BrowsingHistory.LAST_VIEW_TIMESTAMP + " INTEGER NOT NULL," +
                        HistoryContract.BrowsingHistory.FAV_ICON_URI + " TEXT" +
                        ");");
                database.execSQL(
                        "INSERT INTO " + BROWSING_HISTORY_NEW + " (" + HistoryContract.BrowsingHistory._ID +
                                ", " + HistoryContract.BrowsingHistory.TITLE + ", " + HistoryContract.BrowsingHistory.URL +
                                ", " + HistoryContract.BrowsingHistory.VIEW_COUNT + ", " + HistoryContract.BrowsingHistory.LAST_VIEW_TIMESTAMP +
                                ") SELECT " + HistoryContract.BrowsingHistory._ID +
                                ", " + HistoryContract.BrowsingHistory.TITLE + ", " + HistoryContract.BrowsingHistory.URL +
                                ", " + HistoryContract.BrowsingHistory.VIEW_COUNT + ", " + HistoryContract.BrowsingHistory.LAST_VIEW_TIMESTAMP +
                                " FROM " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY);
                database.execSQL("DROP TABLE " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY);
                database.execSQL("ALTER TABLE " + BROWSING_HISTORY_NEW + " RENAME TO " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
    };

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {

        private static final String CREATE_INDEX = "CREATE INDEX IF NOT EXISTS index_browsing_history_view_count ON " +
                HistoryContract.TABLE_NAME + "(" + HistoryContract.BrowsingHistory.VIEW_COUNT + ")";

        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(CREATE_INDEX);
        }
    };
}
