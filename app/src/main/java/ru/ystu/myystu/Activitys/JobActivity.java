package ru.ystu.myystu.Activitys;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.Parcelable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import ru.ystu.myystu.AdaptersData.JobItemsData;
import ru.ystu.myystu.AdaptersData.UpdateItemsTitleData;
import ru.ystu.myystu.Application;
import ru.ystu.myystu.Database.AppDatabase;
import ru.ystu.myystu.Database.Data.CountersData;
import ru.ystu.myystu.Network.LoadLists.GetListJobFromURL;
import ru.ystu.myystu.R;
import ru.ystu.myystu.Adapters.JobItemsAdapter;
import ru.ystu.myystu.Utils.BellHelper;
import ru.ystu.myystu.Utils.BottomFloatingButton.BottomFloatingButton;
import ru.ystu.myystu.Utils.Converter;
import ru.ystu.myystu.Utils.ErrorMessage;
import ru.ystu.myystu.Utils.IntentHelper;
import ru.ystu.myystu.Utils.LightStatusBar;
import ru.ystu.myystu.Utils.NetworkInformation;
import ru.ystu.myystu.Utils.PaddingHelper;
import ru.ystu.myystu.Utils.SettingsController;

public class JobActivity extends AppCompatActivity {

    private Context mContext;
    private ConstraintLayout mainLayout;
    private final String url = "https://www.ystu.ru/information/students/trudoustroystvo/";         // Url страницы трудоустройство сайта ЯГТУ
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mRecyclerViewAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<Parcelable> mList;
    private ArrayList<Parcelable> updateList;
    private Parcelable mRecyclerState;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CompositeDisposable mDisposables;
    private GetListJobFromURL getListJobFromURL;
    private AppDatabase db;
    private boolean isUpdate =false;
    private BottomFloatingButton bfb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job);

        mContext = this;
        mainLayout = findViewById(R.id.main_layout_job);

        if (SettingsController.isDarkTheme(this)) {
            LightStatusBar.setLight(false, false, this, true);
        } else {
            LightStatusBar.setLight(true, true, this, true);
        }

        final AppBarLayout appBarLayout = findViewById(R.id.appBar_job);
        final Toolbar mToolbar = findViewById(R.id.toolBar_job);
        setSupportActionBar(mToolbar);

        mToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        mToolbar.setNavigationOnClickListener(view -> onBackPressed());
        mToolbar.setOnClickListener(e -> {
            if(((LinearLayoutManager)mLayoutManager).findFirstVisibleItemPosition() > 0 && mRecyclerView != null){
                if(((LinearLayoutManager)mLayoutManager).findFirstVisibleItemPosition() < 10)
                    mRecyclerView.smoothScrollToPosition(0);
                else
                    mRecyclerView.scrollToPosition(0);

            }
        });

        mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView = findViewById(R.id.recycler_job_items);
        mSwipeRefreshLayout = findViewById(R.id.refresh_job);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorBackgroundTwo));
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent,
                R.color.colorPrimary);

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isUpdate) {
                getJob();
            } else {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
        mSwipeRefreshLayout.setProgressViewOffset(true, 0, (int) Converter.convertDpToPixel(70, mContext));

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);

        PaddingHelper.setPaddingStatusBarAndToolBar(mContext, mRecyclerView, true);
        PaddingHelper.setOffsetRefreshLayout(mContext, mSwipeRefreshLayout);
        PaddingHelper.setMarginsAppBar(appBarLayout);

        mDisposables = new CompositeDisposable();
        getListJobFromURL = new GetListJobFromURL();

        if (db == null || !db.isOpen())
            db = Application.getInstance().getDatabase();

        if(savedInstanceState == null){
            if (getIntent().getExtras() != null){
                isUpdate = getIntent().getExtras().getBoolean("isUpdate", false);
            }
            getJob();
        } else{
            isUpdate = savedInstanceState.getBoolean("isUpdate", false);
            mList = savedInstanceState.getParcelableArrayList("mList");
            updateList = savedInstanceState.getParcelableArrayList("updateList");

            if (isUpdate) {
                mRecyclerViewAdapter = new JobItemsAdapter(updateList, this);

                bfb = BottomFloatingButton.onSaveInstance.getBottomFloatingButton();
                bfb.updateFragmentManager(getSupportFragmentManager());
                bfb.setOnClickListener(() -> {
                    isUpdate = false;
                    mRecyclerViewAdapter = new JobItemsAdapter(mList, mContext);
                    mRecyclerViewAdapter.setHasStableIds(true);
                    mRecyclerView.setAdapter(mRecyclerViewAdapter);
                    setRecyclerViewAnim(mRecyclerView);
                });
            } else {
                mRecyclerViewAdapter = new JobItemsAdapter(mList, this);
            }

            mRecyclerView.setAdapter(mRecyclerViewAdapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mRecyclerState != null)
            mLayoutManager.onRestoreInstanceState(mRecyclerState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing() && !SettingsController.isEnabledAnim(this)) {
            overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDisposables != null)
            mDisposables.dispose();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_job, menu);
        LightStatusBar.setToolBarIconColor(mContext, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.menu_job_openInBrowser) {
            IntentHelper.openInBrowser(mContext, url);
        }

        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        mRecyclerState = mLayoutManager.onSaveInstanceState();
        outState.putParcelable("recyclerViewState", mRecyclerState);
        outState.putParcelableArrayList("mList", (ArrayList<? extends Parcelable>) mList);
        outState.putParcelableArrayList("updateList", updateList);
        outState.putBoolean("isUpdate", isUpdate);
        if (bfb != null) {
            BottomFloatingButton.onSaveInstance.setBottomFloatingButton(bfb);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mRecyclerState = savedInstanceState.getParcelable("recyclerViewState");
    }

    // Загрузка html страницы и ее парсинг
    private void getJob(){

        if(mList == null) {
            mList = new ArrayList<>();
            updateList = new ArrayList<>();
        } else if (!isUpdate) {
            mList.clear();
            updateList.clear();
        }

        mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(true));

        if (NetworkInformation.hasConnection()) {
            final Single<List<Parcelable>> mSingleJobList = getListJobFromURL.getSingleJobList(url, mList);

            mDisposables.add(mSingleJobList
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<List<Parcelable>>() {
                        @Override
                        public void onSuccess(List<Parcelable> jobItemsData) {
                            mList = jobItemsData;

                            if (isUpdate) {

                                AtomicReference<ArrayList<Parcelable>> temp = new AtomicReference<>();
                                final Thread thread = new Thread(() -> temp.set(getUpdateList(jobItemsData)));
                                thread.start();

                                try {
                                    thread.join();
                                    updateList = temp.get();
                                    if (updateList.size() > 1) {
                                        mRecyclerViewAdapter = new JobItemsAdapter(updateList, mContext);
                                    } else {
                                        mRecyclerViewAdapter = new JobItemsAdapter(mList, mContext);
                                        isUpdate = false;
                                    }

                                    mRecyclerViewAdapter.setHasStableIds(true);
                                    mRecyclerView.post(() -> {
                                        mRecyclerView.setAdapter(mRecyclerViewAdapter);
                                        setRecyclerViewAnim(mRecyclerView);
                                        if (updateList.size() > 1) {
                                            bfb = new BottomFloatingButton(mContext, mainLayout, mContext.getString(R.string.bfb_all_job));
                                            bfb.setIcon(R.drawable.ic_level_back);
                                            bfb.setShowDelay(2000);
                                            bfb.setAnimation(SettingsController.isEnabledAnim(mContext));
                                            bfb.setOnClickListener(() -> {
                                                isUpdate = false;
                                                mRecyclerViewAdapter = new JobItemsAdapter(mList, mContext);
                                                mRecyclerViewAdapter.setHasStableIds(true);
                                                mRecyclerView.setAdapter(mRecyclerViewAdapter);
                                                setRecyclerViewAnim(mRecyclerView);
                                            });
                                            bfb.show();
                                        }
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                setUpdateTag(mList);
                            }

                            new Thread(() -> {
                                try {
                                    if (db.getOpenHelper().getWritableDatabase().isOpen()) {
                                        // Удаляем все записи, если они есть
                                        if (db.jobItemsDao().getCount() > 0) {
                                            db.jobItemsDao().deleteAll();
                                        }

                                        // Добавляем новые записи
                                        for (Parcelable parcelable : jobItemsData) {
                                            if (parcelable instanceof JobItemsData) {
                                                db.jobItemsDao().insert((JobItemsData) parcelable);
                                            }
                                        }

                                        // Обновляем счетчики
                                        // Если нет счетчика, создаем
                                        if (!db.countersDao().isExistsCounter("JOB")) {
                                            final CountersData countersData = new CountersData();
                                            countersData.setType("JOB");
                                            countersData.setCount(0);
                                            db.countersDao().insertCounter(countersData);
                                        } else {
                                            db.countersDao().setCount("JOB", 0);
                                        }
                                        runOnUiThread(BellHelper.UpdateListController::updateItems);
                                    }
                                } catch (SQLiteException e) {
                                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show());
                                }
                            }).start();

                            mSwipeRefreshLayout.setRefreshing(false);
                        }

                        @Override
                        public void onError(Throwable e) {

                            mSwipeRefreshLayout.setRefreshing(false);

                            if(e.getMessage().equals("Not found")){
                                ErrorMessage.show(mainLayout, 1,
                                        mContext.getResources().getString(R.string.error_message_job_not_found),
                                        mContext);
                            } else {
                                ErrorMessage.show(mainLayout, -1, e.getMessage(), mContext);
                            }
                        }
                    }));
        } else {
            new Thread(() -> {
                try {
                    if (db.getOpenHelper().getReadableDatabase().isOpen() && db.jobItemsDao().getCount() > 0) {
                        if (mList.size() > 0)
                            mList.clear();

                        mList.addAll(db.jobItemsDao().getAllJobItems());

                        mRecyclerViewAdapter = new JobItemsAdapter(mList, this);
                        mRecyclerView.post(() -> {
                            mRecyclerView.setAdapter(mRecyclerViewAdapter);
                            setRecyclerViewAnim(mRecyclerView);
                            // SnackBar с предупреждением об отсутствие интернета
                            final Snackbar snackbar = Snackbar
                                    .make(
                                            mainLayout,
                                            getResources().getString(R.string.toast_no_connection_the_internet),
                                            Snackbar.LENGTH_INDEFINITE)
                                    .setAction(
                                            getResources().getString(R.string.error_message_refresh),
                                            view -> {
                                                // Обновление данных
                                                getJob();
                                            });

                            ((TextView)snackbar
                                    .getView()
                                    .findViewById(com.google.android.material.R.id.snackbar_text))
                                    .setTextColor(getResources().getColor(R.color.colorTextBlack));

                            snackbar.show();

                            mSwipeRefreshLayout.setRefreshing(false);
                        });

                    } else {
                        runOnUiThread(() -> {
                            ErrorMessage.show(mainLayout, 0, null, mContext);
                            mSwipeRefreshLayout.setRefreshing(false);
                        });
                    }
                } catch (SQLiteException e) {
                    runOnUiThread(() -> {
                        ErrorMessage.show(mainLayout, -1, e.getMessage(), mContext);
                        mSwipeRefreshLayout.setRefreshing(false);
                    });
                }
            }).start();
        }
    }

    private void setRecyclerViewAnim (final RecyclerView recyclerView) {
        if (SettingsController.isEnabledAnim(this)) {
            final Context context = recyclerView.getContext();
            final LayoutAnimationController controller =
                    AnimationUtils.loadLayoutAnimation(context, R.anim.layout_main_recyclerview_show);
            recyclerView.setLayoutAnimation(controller);
        } else {
            recyclerView.clearAnimation();
        }
    }

    private ArrayList<Parcelable> getUpdateList (List<Parcelable> mList) {

        final ArrayList<Parcelable> tempList = new ArrayList<>();
        tempList.add(new UpdateItemsTitleData(getResources().getString(R.string.other_updateFindTitle), R.drawable.ic_update));

        try {
            if (db.getOpenHelper().getReadableDatabase().isOpen() && db.jobItemsDao().getCount() > 0) {

                for (int i = 0; i < mList.size(); i++) {
                    if (mList.get(i) instanceof JobItemsData) {
                        final String organization = ((JobItemsData) mList.get(i)).getOrganization();
                        if (organization != null && !db.jobItemsDao().isExistsJobByName(organization)) {
                            final JobItemsData eventItem = (JobItemsData) mList.get(i);
                            eventItem.setNew(true);
                            tempList.add(eventItem);
                        }
                    }
                }
            }
        } catch (SQLiteException e) {
            runOnUiThread(() -> mSwipeRefreshLayout.setRefreshing(false));
        }

        return tempList;
    }

    private void setUpdateTag (List<Parcelable> mList) {
        final Thread thread = new Thread(() -> {
            try {
                if (db.getOpenHelper().getReadableDatabase().isOpen() && db.jobItemsDao().getCount() > 0) {

                    for (int i = 0; i < mList.size(); i++) {
                        if (mList.get(i) instanceof JobItemsData) {
                            final String organization = ((JobItemsData) mList.get(i)).getOrganization();
                            if (organization != null && !db.jobItemsDao().isExistsJobByName(organization)) {
                                ((JobItemsData) mList.get(i)).setNew(true);
                            } else {
                                ((JobItemsData) mList.get(i)).setNew(false);
                            }
                        }
                    }
                }
            } catch (SQLiteException ignored) { }

        });
        thread.start();

        try {
            thread.join();
            mRecyclerViewAdapter = new JobItemsAdapter(mList, mContext);
            mRecyclerViewAdapter.setHasStableIds(true);
            mRecyclerView.post(() -> {
                mRecyclerView.setAdapter(mRecyclerViewAdapter);
                setRecyclerViewAnim(mRecyclerView);
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
