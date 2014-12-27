/*
 * Copyright 2014 Simone Casagranda.
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

package com.alchemiasoft.book.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.alchemiasoft.book.R;
import com.alchemiasoft.book.content.BookDB;
import com.alchemiasoft.book.fragment.base.RecyclerViewFragment;
import com.alchemiasoft.book.loader.BooksLoader;
import com.alchemiasoft.book.model.Book;
import com.alchemiasoft.book.util.ViewUtil;
import com.alchemiasoft.book.widget.SmartSwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays books to the users.
 * <p/>
 * Created by Simone Casagranda on 20/12/14.
 */
public class BooksFragment extends RecyclerViewFragment implements LoaderManager.LoaderCallbacks<List<Book>> {

    /**
     * Interface used to expose the data refresh.
     */
    private static interface OnRefreshDataListener {

        /**
         * Called when the content should be refreshed.
         */
        public void onRefreshData();

    }

    /**
     * Key used to pass the owner filter. This parameter is completely optional.
     */
    private static final String KEY_OWNED = "com.alchemiasoft.book.OWNED";

    /**
     * Number of columns shown to the user.
     */
    private static final int COLUMN_COUNT = 2;

    /**
     * Id used for the book(s) loader.
     */
    private static final int ID_LOADER_BOOKS = 23;

    /**
     * Creates a new instance of BookListFragment.
     *
     * @param context
     * @param owned   true to get only the owned books, false otherwise.
     * @return an instance of BookListFragment ready to be attached.
     */
    public static BooksFragment create(Context context, boolean owned) {
        final BooksFragment fragment = new BooksFragment();
        final Bundle args = new Bundle();
        args.putBoolean(KEY_OWNED, owned);
        fragment.setArguments(args);
        return fragment;
    }

    private BooksAdapter mAdapter;
    private StaggeredGridLayoutManager mLayoutManager;

    private final OnRefreshDataListener mOnRefreshDataListener = new OnRefreshDataListener() {
        @Override
        public void onRefreshData() {
            final Activity activity = getActivity();
            if (activity != null) {
                // Restarting the loader
                getLoaderManager().restartLoader(ID_LOADER_BOOKS, getArguments(), BooksFragment.this);
            }
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.no_books));
        final RecyclerView recyclerView = getRecyclerView();
        // Set up adapter and recycler view
        mAdapter = new BooksAdapter();
        mAdapter.setOnRefreshDataListener(mOnRefreshDataListener);
        setRecyclerAdapter(mAdapter);
        mLayoutManager = new StaggeredGridLayoutManager(COLUMN_COUNT, GridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        setScrollInterceptor(new SmartSwipeRefreshLayout.ScrollInterceptor() {
            @Override
            public boolean canChildScrollUp() {
                // We don't need any scroll update
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Initialising the loader
        getLoaderManager().initLoader(ID_LOADER_BOOKS, getArguments(), this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getLoaderManager().destroyLoader(ID_LOADER_BOOKS);
    }

    @Override
    public Loader<List<Book>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ID_LOADER_BOOKS:
                return new BooksLoader(getActivity(), args.getBoolean(KEY_OWNED));
            default:
                throw new IllegalArgumentException("loader id=" + id + " is not supported!");
        }
    }

    @Override
    public void onLoadFinished(Loader<List<Book>> loader, List<Book> data) {
        mAdapter.swap(data);
        if (isResumed()) {
            setContentShown(true);
        } else {
            setContentShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Book>> loader) {
        mAdapter.swap(null);
    }

    /**
     * Holder that has got references to the UI for a book entry.
     */
    private static class BookHolder extends RecyclerView.ViewHolder {

        private TextView mTitleTextView, mAuthorTextView;
        private View mOwnedView, mOverflowView;

        private BookHolder(View itemView) {
            super(itemView);
            mTitleTextView = (TextView) itemView.findViewById(R.id.title);
            mAuthorTextView = (TextView) itemView.findViewById(R.id.author);
            mOwnedView = itemView.findViewById(R.id.owned_book);
            mOverflowView = itemView.findViewById(R.id.menu_overflow);
        }

    }

    /**
     * Adapter that takes care of displaying the right data in the BookHolder(s)
     * based on the current/expected Book entries.
     */
    private static class BooksAdapter extends RecyclerView.Adapter<BookHolder> {

        /**
         * Books currently shown.
         */
        private final List<Book> mBooks = new ArrayList<>();

        private OnRefreshDataListener mOnRefreshDataListener;

        @Override
        public BookHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            final View view = View.inflate(viewGroup.getContext(), R.layout.item_book, null);
            return new BookHolder(view);
        }

        @Override
        public void onBindViewHolder(final BookHolder bookHolder, int pos) {
            final Book book = mBooks.get(pos);
            bookHolder.mTitleTextView.setText(book.getTitle());
            bookHolder.mAuthorTextView.setText(book.getAuthor());
            if (book.isOwned()) {
                bookHolder.mOwnedView.setBackgroundResource(R.drawable.owned_book);
            } else {
                ViewUtil.setBackground(bookHolder.mOwnedView, null);
            }
            bookHolder.mOverflowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final PopupMenu popup = new PopupMenu(v.getContext(), v, Gravity.BOTTOM);
                    final MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.menu_book, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            final Context context = v.getContext();
                            switch (menuItem.getItemId()) {
                                case R.id.action_buy:
                                    if (book.isOwned()) {
                                        Toast.makeText(context, context.getString(R.string.book_already_owned), Toast.LENGTH_SHORT).show();
                                    } else {
                                        final ContentValues cv = new ContentValues();
                                        cv.put(BookDB.Book.OWNED, 1);
                                        final ContentResolver cr = context.getContentResolver();
                                        if (cr.update(BookDB.Book.create(book.getId()), cv, null, null) > 0 && mOnRefreshDataListener != null) {
                                            // Refreshing the current data
                                            mOnRefreshDataListener.onRefreshData();
                                        }
                                    }
                                    return true;
                                case R.id.action_sell:
                                    if (!book.isOwned()) {
                                        Toast.makeText(context, context.getString(R.string.book_not_owned), Toast.LENGTH_SHORT).show();
                                    } else {
                                        final ContentValues cv = new ContentValues();
                                        cv.put(BookDB.Book.OWNED, 0);
                                        final ContentResolver cr = context.getContentResolver();
                                        if (cr.update(BookDB.Book.create(book.getId()), cv, null, null) > 0 && mOnRefreshDataListener != null) {
                                            // Refreshing the current data
                                            mOnRefreshDataListener.onRefreshData();
                                        }
                                    }
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    popup.show();
                }
            });
        }

        public void setOnRefreshDataListener(OnRefreshDataListener listener) {
            this.mOnRefreshDataListener = listener;
        }

        @Override
        public int getItemCount() {
            return mBooks.size();
        }

        @Override
        public long getItemId(int pos) {
            return mBooks.get(pos).getId();
        }

        public void swap(List<Book> books) {
            mBooks.clear();
            if (books != null) {
                mBooks.addAll(books);
            }
            notifyDataSetChanged();
        }
    }
}