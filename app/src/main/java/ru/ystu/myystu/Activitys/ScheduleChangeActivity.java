package ru.ystu.myystu.Activitys;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ru.ystu.myystu.Adapters.ScheduleChangeAdapter;
import ru.ystu.myystu.AdaptersData.ScheduleChangeData;
import ru.ystu.myystu.R;
import ru.ystu.myystu.Utils.LightStatusBar;
import ru.ystu.myystu.Utils.PaddingHelper;
import ru.ystu.myystu.Utils.SettingsController;

import android.os.Bundle;
import android.os.Parcelable;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleChangeActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private ArrayList<Parcelable> mList;
    private Parcelable mRecyclerState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_change);

        if (SettingsController.isDarkTheme(this)) {
            LightStatusBar.setLight(false, false, this, true);
        } else {
            LightStatusBar.setLight(true, true, this, true);
        }

        final AppBarLayout appBarLayout = findViewById(R.id.appBar_schedule_change);
        final Toolbar mToolbar = findViewById(R.id.toolBar_schedule_change);
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

        mRecyclerView = findViewById(R.id.recycler_schedule_change);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        PaddingHelper.setPaddingStatusBarAndToolBar(this, mRecyclerView, true);
        PaddingHelper.setMarginsAppBar(appBarLayout);

        if(getIntent().getExtras() != null){
            final ArrayList<String> tempList = getIntent().getExtras().getStringArrayList("mList");

            if(mList == null)
                mList = new ArrayList<>();
            else
                mList.clear();

            if (tempList != null) {
                for (String temp : tempList) {

                    final Pattern mPattern = Pattern.compile("([0-9]{2}\\.){2}[0-9]{4}");
                    final Matcher mMatcher = mPattern.matcher(temp);


                    if(mMatcher.lookingAt()) {
                        mMatcher.reset();

                        String date = null;
                        String text = null;
                        while (mMatcher.find()) {
                            date = temp.substring(mMatcher.start(), mMatcher.end());
                            text = temp.substring(mMatcher.end());
                        }

                        if (date != null) {

                            // Обрезаем точки и пробелы вначале
                            while (true) {
                                if (text.startsWith(" ") || text.startsWith(".")) {
                                    text = text.substring(1);
                                } else {
                                    break;
                                }
                            }

                            mList.add(new ScheduleChangeData(date, text));
                        }
                    }
                }
            }

            Collections.reverse(mList);

            RecyclerView.Adapter mRecyclerViewAdapter = new ScheduleChangeAdapter(mList);
            mRecyclerViewAdapter.setHasStableIds(true);
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
        if(isFinishing()) {
            if (SettingsController.isEnabledAnim(this)) {
                overridePendingTransition(R.anim.activity_slide_right_show_reverse, R.anim.activity_slide_left_out_reverse);
            } else {
                overridePendingTransition(0, 0);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        mRecyclerState = mLayoutManager.onSaveInstanceState();
        outState.putParcelable("recyclerViewState", mRecyclerState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mRecyclerState = savedInstanceState.getParcelable("recyclerViewState");
    }
}
