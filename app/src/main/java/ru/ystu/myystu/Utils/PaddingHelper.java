package ru.ystu.myystu.Utils;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import ru.ystu.myystu.R;

public class PaddingHelper {

    public static void setPaddingStatusBarAndToolBar (Context mContext, View view, boolean isBottomPadding) {
        final TypedValue tv = new TypedValue();
        final int[] actionBarHeight = {(int) Converter.convertDpToPixel(56, mContext)};
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            if (mContext.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true))
                actionBarHeight[0] = TypedValue.complexToDimensionPixelSize(tv.data, mContext.getResources().getDisplayMetrics());
            if (isBottomPadding) {
                v.setPadding(0, insets.getSystemWindowInsetTop() + actionBarHeight[0],0, insets.getSystemWindowInsetBottom());
            } else {
                v.setPadding(0, insets.getSystemWindowInsetTop() + actionBarHeight[0],0, 0);
            }
            return insets;
        });
    }

    public static void setOffsetRefreshLayout (Context mContext, SwipeRefreshLayout swipeRefreshLayout) {
        final int[] actionBarHeight = {(int) Converter.convertDpToPixel(56, mContext)};
        swipeRefreshLayout.setOnApplyWindowInsetsListener((v, insets) -> {
            ((SwipeRefreshLayout) v).setProgressViewOffset(true, 0,
                    insets.getSystemWindowInsetTop()
                            + actionBarHeight[0]
                            + (int) Converter.convertDpToPixel(14, mContext));

            return insets;
        });
    }

    public static void setMarginsSnackbar (Context mContext, Snackbar snackbar) {

        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbar.getView().getLayoutParams();
        params.setMargins(params.leftMargin + (int) Converter.convertDpToPixel(16, mContext),
                params.topMargin,
                params.rightMargin + (int) Converter.convertDpToPixel(16, mContext),
                params.bottomMargin + (int) Converter.convertDpToPixel(70, mContext));
        snackbar.getView().setLayoutParams(params);

    }
}
