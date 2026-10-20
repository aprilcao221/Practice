package com.april.airtableform;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Builds the app's views in code, so there are no layout files to keep in sync. */
final class Ui {

    static final int ACCENT = Color.parseColor("#2D7FF9");

    private final Context context;

    Ui(Context context) {
        this.context = context;
    }

    int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    LinearLayout column() {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(12), dp(16), dp(24));
        return layout;
    }

    TextView heading(String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, dp(16), 0, dp(4));
        return view;
    }

    TextView hint(String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setTextColor(Color.GRAY);
        view.setPadding(0, 0, 0, dp(4));
        return view;
    }

    EditText editText(String hint) {
        EditText view = new EditText(context);
        view.setHint(hint);
        return view;
    }

    Button button(String text) {
        Button view = new Button(context);
        view.setText(text);
        view.setAllCaps(false);
        return view;
    }

    Button primaryButton(String text) {
        Button view = button(text);
        view.setTextColor(Color.WHITE);
        view.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        params.topMargin = dp(20);
        view.setLayoutParams(params);
        return view;
    }
}
