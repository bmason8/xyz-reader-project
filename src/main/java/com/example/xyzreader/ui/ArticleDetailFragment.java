package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.NestedScrollView.OnScrollChangeListener;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ArticleLoader.Query;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;

    private Toolbar mToolbar;
    private ImageView mPhotoView;
    private String mTitle;
    private NestedScrollView mNestedScrollView;
    private FloatingActionButton mShareFabButton;
    private FloatingActionButton mShareFabBottomEnd;

    String allBodyText;
    int allBodyTextLength;
    int subStringIndexBeginPosition, subStringIndexEndPosition;
    boolean isThereMoreTextToLoad;

    TextView titleView, bylineView, bodyView;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);

                if (savedInstanceState != null) {

                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int height = displayMetrics.heightPixels;
                    int width = displayMetrics.widthPixels;

                    final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
                    if (position != null) {
                        mNestedScrollView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mNestedScrollView.scrollTo(position[0], position[1]);
                            }
                        }, 300);
                    }


//            bodyView.setText(savedInstanceState.getString("BODY_TEXT"));
//            final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
//            if (position != null)
//                mNestedScrollView.post(new Runnable() {
//                    public void run() {
//                        mNestedScrollView.scrollTo(position[0], position[1]);
//                    }
//                });
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        titleView = (TextView) mRootView.findViewById(R.id.article_title);
        bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        mNestedScrollView = mRootView.findViewById(R.id.nested_scrollView);
        mShareFabButton = mRootView.findViewById(R.id.share_fab);
        mShareFabBottomEnd = mRootView.findViewById(R.id.share_fab_bottom_end);

        final Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        mPhotoView.startAnimation(fadeInAnimation);
        Animation slideUpAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up);
        bodyView.startAnimation(slideUpAnimation);



        subStringIndexBeginPosition = 0;
        subStringIndexEndPosition = 5000;
        isThereMoreTextToLoad = true;

        final CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar_layout);
        mToolbar = (Toolbar) mRootView.findViewById(R.id.main_toolbar);
        AppBarLayout appBarLayout = (AppBarLayout) mRootView.findViewById(R.id.photo_appBar);

        // https://stackoverflow.com/questions/31682310/android-collapsingtoolbarlayout-collapse-listener
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

                if (Math.abs(verticalOffset)-appBarLayout.getTotalScrollRange() == 0)
                {
                    //  Collapsed
                    mToolbar.setTitle(mTitle);
                }
                else if (verticalOffset == 0) {
                    //Expanded
                    collapsingToolbarLayout.setTitle("");
                    if (mShareFabBottomEnd.getVisibility() == View.VISIBLE) {
                        Animation fadeOutAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out_fab);
                        mShareFabBottomEnd.startAnimation(fadeOutAnimation);
                        mShareFabBottomEnd.setVisibility(View.INVISIBLE);
                    }

                } else if (verticalOffset < -200) {

                    if (mShareFabBottomEnd.getVisibility() == View.INVISIBLE) {
                    Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in_fab);
                    mShareFabBottomEnd.startAnimation(fadeInAnimation);
                    mShareFabBottomEnd.setVisibility(View.VISIBLE);
                    mShareFabBottomEnd.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startShareIntent();
                        }
                    });
                    }
                }
            }
        });

        if (mNestedScrollView != null) {
            mNestedScrollView.setOnScrollChangeListener(new OnScrollChangeListener() {
                @Override
                public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
                        if (mCursor != null && isThereMoreTextToLoad) {
                            // https://stackoverflow.com/questions/5414657/extract-substring-from-a-string
                            if (subStringIndexEndPosition > allBodyTextLength) {
                                subStringIndexEndPosition = allBodyTextLength ;
                                String moreText = (allBodyText.substring(subStringIndexBeginPosition));
                                bodyView.append(moreText);
                                isThereMoreTextToLoad = false;
                                showShareSnackBar();
                            } else {
                                String moreText = (allBodyText.substring(subStringIndexBeginPosition, subStringIndexEndPosition));
                                bodyView.append(moreText);
                                subStringIndexBeginPosition = subStringIndexEndPosition;
                                subStringIndexEndPosition = subStringIndexEndPosition + 5000;
                            }
                        }
                    }

                }

            });
        }

        mShareFabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startShareIntent();
            }
        });

        return mRootView;
    }

    // https://stackoverflow.com/questions/29208086/save-the-position-of-scrollview-when-the-orientation-changes/29208325
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("BODY_TEXT", bodyView.getText().toString());
        outState.putIntArray("ARTICLE_SCROLL_POSITION", new int[] {mNestedScrollView.getScrollX(), mNestedScrollView.getScrollY()});
    }

    // https://stackoverflow.com/questions/17792132/how-does-onviewstaterestored-from-fragments-work
//    @Override
//    public void onViewStateRestored(Bundle savedInstanceState) {
//        super.onViewStateRestored(savedInstanceState);
//        if (savedInstanceState != null) {
//            bodyView.setText(savedInstanceState.getString("BODY_TEXT"));
//            final int[] position = savedInstanceState.getIntArray("ARTICLE_SCROLL_POSITION");
//            if (position != null)
//                mNestedScrollView.post(new Runnable() {
//                    public void run() {
//                        mNestedScrollView.scrollTo(position[0], position[1]);
//                    }
//                });
//        }
//    }

    private void startShareIntent() {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                .setType("text/plain")
                .setText("Some sample text")
                .getIntent(), getString(R.string.action_share)));
    }

    private void getBodyText() {
        String bodyText = null;
        try {
            GetTextAndFixLineSpacing getTextAndFixLineSpacing = new GetTextAndFixLineSpacing();
            getTextAndFixLineSpacing.execute();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            bodyView.setText("N/A");
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void loadImageAndSmallText() {
        if (mRootView == null) {
            return;
        }

        if (mCursor != null) {

            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitle = mCursor.getString(Query.TITLE);
            titleView.setText(mTitle);
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString() + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }

            String imageUrl = mCursor.getString(Query.PHOTO_URL);
            Picasso.get().
                    load(imageUrl)
                    .error(R.drawable.image_not_found)
                    .placeholder(R.color.theme_primary)
                    .into(mPhotoView);
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

         loadImageAndSmallText();
        getBodyText();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        loadImageAndSmallText();
    }


    private class GetTextAndFixLineSpacing extends AsyncTask<Void, Integer, String> {

        @Override
        protected String doInBackground(Void... params) {
            allBodyText = String.valueOf((Html.fromHtml(mCursor.getString(Query.BODY).replaceAll("(\r\n\r\n)", "<br/><br/>"))));
            return allBodyText;
        }

        @Override
        protected void onPostExecute(String bodyText) {
            allBodyTextLength = allBodyText.length();
            if (subStringIndexEndPosition > allBodyTextLength) {
                subStringIndexEndPosition = allBodyTextLength;
                isThereMoreTextToLoad = false;
                bodyView.setText(bodyText.substring(subStringIndexBeginPosition, subStringIndexEndPosition));
            } else {
                bodyView.setText(bodyText.substring(subStringIndexBeginPosition, subStringIndexEndPosition));
                subStringIndexBeginPosition = subStringIndexEndPosition;
                subStringIndexEndPosition = subStringIndexEndPosition + 5000;
            }
        }
    }

    private void showShareSnackBar() {
        // https://www.androidhive.info/2015/09/android-material-design-snackbar-example/
        Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), "End of article", Snackbar.LENGTH_LONG)
                .setAction("Share", new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                                .setType("text/plain")
                                .setText("Some sample text")
                                .getIntent(), getString(R.string.action_share)));
                    }
                });
        snackbar.show();
    }
}
