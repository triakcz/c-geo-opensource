package cgeo.geocaching.activity;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeo;
import cgeo.geocaching.compatibility.Compatibility;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import gnu.android.app.appmanualclient.AppManualReaderClient;

public final class ActivityMixin {

    public final static void goHome(final Activity fromActivity) {
        final Intent intent = new Intent(fromActivity, cgeo.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        fromActivity.startActivity(intent);
        fromActivity.finish();
    }

    public static void goManual(final Context context, final String helpTopic) {
        if (StringUtils.isBlank(helpTopic)) {
            return;
        }
        try {
            AppManualReaderClient.openManual(
                    "c-geo",
                    helpTopic,
                    context,
                    "http://manual.cgeo.org/");
        } catch (Exception e) {
            // nothing
        }
    }

    public static void setTitle(final Activity activity, final String text) {
        if (StringUtils.isBlank(text)) {
            return;
        }

        final TextView title = (TextView) activity.findViewById(R.id.actionbar_title);
        if (title != null) {
            title.setText(text);
        }
    }

    public static void showProgress(final Activity activity, final boolean show) {
        if (activity == null) {
            return;
        }

        final ProgressBar progress = (ProgressBar) activity.findViewById(R.id.actionbar_progress);
        if (show) {
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }
    }

    public static void setTheme(final Activity activity) {
        if (Settings.isLightSkin()) {
            activity.setTheme(R.style.light);
        } else {
            activity.setTheme(R.style.dark);
        }
    }

    public static void showToast(final Context context, final String text) {
        if (StringUtils.isNotBlank(text)) {
            Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);

            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
            toast.show();
        }
    }

    public static void showShortToast(final Context context, final String text) {
        if (StringUtils.isNotBlank(text)) {
            Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);

            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
            toast.show();
        }
    }

    public static void helpDialog(final Activity activity, final String title, final String message, final Drawable icon) {
        if (StringUtils.isBlank(message)) {
            return;
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity).setTitle(title).setMessage(message).setCancelable(true);
        dialog.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        if (icon != null) {
            dialog.setIcon(icon);
        }

        AlertDialog alert = dialog.create();
        alert.show();
    }

    public static void helpDialog(Activity activity, String title, String message) {
        helpDialog(activity, title, message, null);
    }

    public static void keepScreenOn(final Activity abstractActivity, boolean keepScreenOn) {
        if (keepScreenOn) {
            abstractActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public static void invalidateOptionsMenu(Activity activity) {
        Compatibility.invalidateOptionsMenu(activity);
    }
}
