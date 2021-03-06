/*
 * Copyright 2015 Simone Casagranda.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alchemiasoft.common.content;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.alchemiasoft.common.lib.Constants;
import com.alchemiasoft.common.model.Book;
import com.alchemiasoft.common.util.ResUtil;

import org.json.JSONArray;

import java.util.List;

/**
 * SQLiteOpenHelper that creates the SQLite database for the Book application.
 * <p/>
 * Created by Simone Casagranda on 20/12/14.
 */
public class BookDBOpenHelper extends SQLiteOpenHelper {

    /**
     * Tag used for logging.
     */
    private static final String TAG_LOG = BookDBOpenHelper.class.getSimpleName();

    private final Context mContext;

    public BookDBOpenHelper(Context context) {
        super(context, BookDB.NAME, null, BookDB.VERSION);
        mContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.beginTransaction();

            db.execSQL(BookDB.Book.CREATE_TABLE);

            String input = null;
            try {
                input = ResUtil.assetAsString(mContext, Constants.BOOKS_PATH);
            } catch (Exception e) {
                Log.e(TAG_LOG, "Cannot read the input at assets/" + Constants.BOOKS_PATH);
            }
            // Adding the default entries
            if (!TextUtils.isEmpty(input)) {
                Log.d(TAG_LOG, "Adding into the DB: " + input);
                final JSONArray arr = new JSONArray(input);
                final List<Book> books = Book.allFrom(arr);
                // Adding the all the books as a batch
                for (final Book book : books) {
                    db.insert(BookDB.Book.TABLE, null, book.toValues());
                }
            }

            db.setTransactionSuccessful();
            Log.i(TAG_LOG, "Successfully created " + BookDB.NAME);
        } catch (Exception e) {
            Log.e(TAG_LOG, "Error creating " + BookDB.NAME + ": ", e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.beginTransaction();

            db.execSQL(BookDB.Book.DELETE_TABLE);

            onCreate(db);

            db.setTransactionSuccessful();
            Log.i(TAG_LOG, "Successfully upgraded " + BookDB.NAME);
        } catch (Exception e) {
            Log.e(TAG_LOG, "Error creating " + BookDB.NAME + ": ", e);
        } finally {
            db.endTransaction();
        }
    }
}
