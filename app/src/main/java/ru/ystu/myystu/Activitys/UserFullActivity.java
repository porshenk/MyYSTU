package ru.ystu.myystu.Activitys;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import ru.ystu.myystu.Application;
import ru.ystu.myystu.Database.AppDatabase;
import ru.ystu.myystu.Database.Data.UserFullData;
import ru.ystu.myystu.Network.GetUserInformationFromURL;
import ru.ystu.myystu.R;
import ru.ystu.myystu.Utils.Converter;
import ru.ystu.myystu.Utils.ErrorMessage;
import ru.ystu.myystu.Utils.FrescoHelper;
import ru.ystu.myystu.Utils.IntentHelper;
import ru.ystu.myystu.Utils.LightStatusBar;
import ru.ystu.myystu.Utils.NetworkInformation;
import ru.ystu.myystu.Utils.PaddingHelper;
import ru.ystu.myystu.Utils.PhotoViewSetter;
import ru.ystu.myystu.Utils.SettingsController;
import ru.ystu.myystu.Utils.StringFormatter;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

public class UserFullActivity extends AppCompatActivity {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NestedScrollView scroll;
    private ConstraintLayout mainLayout;

    private Context mContext;

    private ConstraintLayout location;
    private ConstraintLayout phone;
    private ConstraintLayout email;
    private ConstraintLayout detail;
    private AppCompatTextView locationText;
    private AppCompatTextView phoneText;
    private AppCompatTextView emailText;
    private AppCompatTextView detailText;

    private GetUserInformationFromURL getUserInformationFromURL;
    private CompositeDisposable mDisposable;

    private AppDatabase db;

    private int id;
    private String url;
    private String image;
    private String name;
    private String information;
    private String locationStr;
    private String phoneStr;
    private String emailStr;
    private String detailStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_full);

        mContext = this;
        if (SettingsController.isDarkTheme(this)) {
            LightStatusBar.setLight(false, false, this, true);
        } else {
            LightStatusBar.setLight(true, true, this, true);
        }

        mainLayout = findViewById(R.id.main_layout_userFull);
        mSwipeRefreshLayout = findViewById(R.id.refresh_userFull);
        scroll = findViewById(R.id.userFull_scroll);
        SimpleDraweeView imageView = findViewById(R.id.userFull_image);
        AppCompatTextView nameView = findViewById(R.id.userFull_name);
        AppCompatTextView informationView = findViewById(R.id.userFull_information);
        location = findViewById(R.id.userFull_location);
        locationText = findViewById(R.id.userFull_location_text);
        phone = findViewById(R.id.userFull_phone);
        phoneText = findViewById(R.id.userFull_phone_text);
        email = findViewById(R.id.userFull_email);
        emailText = findViewById(R.id.userFull_email_text);
        detail = findViewById(R.id.userFull_detail);
        detailText = findViewById(R.id.userFull_detail_text);
        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorBackgroundTwo));
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent,
                R.color.colorPrimary);
        mSwipeRefreshLayout.setOnRefreshListener(this::getUserInfo);
        mSwipeRefreshLayout.setProgressViewOffset(true, 0, (int) Converter.convertDpToPixel(70, this));

        final AppBarLayout appBarLayout = findViewById(R.id.appBar_user_full);
        final Toolbar mToolbar = findViewById(R.id.toolBar_userFull);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        mToolbar.setNavigationOnClickListener(view -> onBackPressed());
        mToolbar.setOnClickListener(e -> scroll.smoothScrollTo(0, 0));

        PaddingHelper.setPaddingStatusBarAndToolBar(mContext, scroll, true);
        PaddingHelper.setOffsetRefreshLayout(mContext, mSwipeRefreshLayout);
        PaddingHelper.setMarginsAppBar(appBarLayout);

        if(getIntent().getExtras() != null) {
            id = getIntent().getExtras().getInt("id");
            url = getIntent().getExtras().getString("link");
            image = getIntent().getExtras().getString("image");
            name = getIntent().getExtras().getString("name");
            information = getIntent().getExtras().getString("information");
        }

        if (SettingsController.isImageDownload(mContext)) {
            imageView.setImageRequest(FrescoHelper.getImageRequest(mContext, image));
            imageView.setOnClickListener(v -> PhotoViewSetter.setPhoto(this, image));
        }

        nameView.setText(name);
        informationView.setText(information);

        if (db == null || !db.isOpen())
            db = Application.getInstance().getDatabase();

        if(savedInstanceState == null) {
            getUserInfo();
        } else {

            locationStr = savedInstanceState.getString("location");
            phoneStr = savedInstanceState.getString("phone");
            emailStr = savedInstanceState.getString("email");
            detailStr = savedInstanceState.getString("detail");

            setInformation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            if (SettingsController.isEnabledAnim(this)) {
                overridePendingTransition(R.anim.activity_slide_right_show_reverse, R.anim.activity_slide_left_out_reverse);
            } else {
                overridePendingTransition(0, 0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mDisposable != null)
            mDisposable.dispose();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("position", scroll.getScrollY());
        outState.putString("url", url);
        outState.putString("location", locationStr);
        outState.putString("phone", phoneStr);
        outState.putString("email", emailStr);
        outState.putString("detail", detailStr);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        scroll.smoothScrollTo(0, savedInstanceState.getInt("position", 0));
        url = savedInstanceState.getString("url");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_full, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_user_full_openInBrowser:
                IntentHelper.openInBrowser(mContext, url);
                return true;

            case R.id.menu_user_full_shareLink:
                IntentHelper.shareText(mContext, name + "\n\n" + url);
                return true;
        }

        return false;
    }

    private void getUserInfo() {

        getUserInformationFromURL = new GetUserInformationFromURL();
        mDisposable = new CompositeDisposable();
        mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(true));

        if(NetworkInformation.hasConnection()) {

            final Observable<String> mObservable = getUserInformationFromURL.getObservableUserInformation(url);
            mDisposable.add(mObservable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableObserver<String>() {

                        @Override
                        public void onNext(String s) {

                            if (s.startsWith("adr:"))
                                locationStr = s.substring(s.indexOf(":") + 1);

                            if (s.startsWith("tel:"))
                                phoneStr = s.substring(s.indexOf(":") + 1);

                            if (s.startsWith("email:"))
                                emailStr = s.substring(s.indexOf(":") + 1);

                            if (s.startsWith("userDetail:"))
                                detailStr = s.substring(s.indexOf(":") + 1);
                        }

                        @Override
                        public void onComplete() {
                            new Thread(() -> {
                                try {
                                    if (db.getOpenHelper().getWritableDatabase().isOpen()) {
                                        // Удаляем запись, если она есть
                                        if (db.userFullDao().isExists(id)) {
                                            db.userFullDao().delete(id);
                                        }

                                        // Добавляем новую запись
                                        final UserFullData userFullData = new UserFullData();
                                        userFullData.setId(id);
                                        userFullData.setPhone(phoneStr);
                                        userFullData.setEmail(emailStr);
                                        userFullData.setLocation(locationStr);
                                        userFullData.setDetail(detailStr);

                                        db.userFullDao().insert(userFullData);
                                    }
                                } catch (SQLiteException e) {
                                    runOnUiThread(() -> Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show());
                                }
                            }).start();

                            setInformation();
                            mSwipeRefreshLayout.setRefreshing(false);
                        }

                        @Override
                        public void onError(Throwable e) {
                            mSwipeRefreshLayout.setRefreshing(false);
                            if(e.getMessage().equals("Not found")){
                                ErrorMessage.show(mainLayout, 1,
                                        getResources().getString(R.string.error_message_file_not_found),
                                        mContext);
                            } else
                                ErrorMessage.show(mainLayout, -1, e.getMessage(), mContext);
                        }
                    }));
        } else {
            new Thread(() -> {
                try {
                    if (db.getOpenHelper().getReadableDatabase().isOpen() && db.userFullDao().isExists(id)) {

                        final UserFullData userFullData = db.userFullDao().getUserFull(id);
                        phoneStr = userFullData.getPhone();
                        emailStr = userFullData.getEmail();
                        locationStr = userFullData.getLocation();
                        detailStr = userFullData.getDetail();

                        runOnUiThread(() -> {
                            setInformation();
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
                                                getUserInfo();
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

    private void setInformation () {
        Spanned spanText;

        if (locationStr != null) {
            location.setVisibility(View.VISIBLE);
            locationText.setText(locationStr);
        }
        if (phoneStr != null) {
            phone.setVisibility(View.VISIBLE);
            spanText = new StringFormatter().getPhoneNumber(new SpannableStringBuilder(phoneStr));
            phoneText.setText(spanText);
            phoneText.setMovementMethod(LinkMovementMethod.getInstance());
        }
        if (emailStr != null) {
            email.setVisibility(View.VISIBLE);
            spanText = new StringFormatter().getEmail(new SpannableStringBuilder(emailStr));
            emailText.setText(spanText);
            emailText.setMovementMethod(LinkMovementMethod.getInstance());
        }
        if (detailStr != null) {
            detail.setVisibility(View.VISIBLE);
            detailText.setText(detailStr);
        }
    }
}
