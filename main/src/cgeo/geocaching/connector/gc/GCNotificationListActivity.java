package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.models.GCNotification;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.Log;

import io.reactivex.functions.Consumer;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class GCNotificationListActivity extends AbstractActionBarActivity {
    private final Progress progress = new Progress();

    @NonNull
    private final List<GCNotification> notifications = new ArrayList<>();

    @NonNull
    final GCNotificationListAdapter adapter = new GCNotificationListAdapter(notifications);

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.manage_notifications_options, menu);
        return true;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        Log.d("onCreate GCNotificationListActivity");
        super.onCreate(savedInstanceState, R.layout.gcnotification_activity);

        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.gcnotification_list, true, true);
        view.setAdapter(adapter);

        final ProgressDialog waitDialog = ProgressDialog.show(this, getString(R.string.search_notifications_title), getString(R.string.search_notifications_loading), true, true);
        waitDialog.setCancelable(true);
        loadInBackground(adapter, waitDialog);
    }

    private void loadInBackground(final GCNotificationListAdapter adapter, final ProgressDialog waitDialog) {
        Log.d("Subscribing to searchGCNotificationListObservable");
        AndroidRxUtils.bindActivity(this, GCParser.searchGCNotificationListObservable).subscribe(new Consumer<List<GCNotification>>() {
            @Override
            public void accept(final List<GCNotification> notificationList) {
                waitDialog.dismiss();
                notifications.addAll(notificationList);
                adapter.notifyItemRangeInserted(0, notificationList.size());
                final Handler h = new Handler() {
                    @Override
                    public void handleMessage(final Message msg) {
                        adapter.notifyDataSetChanged();
                    }
                };

                for (final GCNotification notification : notificationList) {
                    AndroidRxUtils.networkScheduler.scheduleDirect(new Runnable() {
                        @Override
                        public void run() {
                            GCParser.getGCNotificationDetails(notification, h);
                        }
                    });
                }
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(final Throwable e) {
                ActivityMixin.showToast(GCNotificationListActivity.this, getString(R.string.err_read_notification_list));
                finish();
            }
        });
    }

    private class BatchUpdateHandler extends DisposableHandler {
        @Override
        public void handleRegularMessage(final Message msg) {
            progress.incrementProgressBy(1);
        }
    }

    void setNotificationsActiveState(ArrayList<GCNotification> notifications, final boolean state) {
        final BatchUpdateHandler batchUpdateHandler = new BatchUpdateHandler();
        progress.show(this, null, "Refreshing notification status", ProgressDialog.STYLE_HORIZONTAL, batchUpdateHandler.disposeMessage());
        progress.setMaxProgressAndReset(notifications.size());
        for (final GCNotification notification : notifications) {
            AndroidRxUtils.networkScheduler.scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    GCParser.toggleGCNotificationState(notification, batchUpdateHandler);
                }
            });
        }
    }

    void deleteNotifications(ArrayList<GCNotification> notifications) {
        final BatchUpdateHandler batchUpdateHandler = new BatchUpdateHandler();
        progress.show(this, null, "Deleting notifications", ProgressDialog.STYLE_HORIZONTAL, batchUpdateHandler.disposeMessage());
        progress.setMaxProgressAndReset(notifications.size());
        for (final GCNotification notification : notifications) {
            AndroidRxUtils.networkScheduler.scheduleDirect(new Runnable() {
                @Override
                public void run() {
                    GCParser.deleteGCNotification(notification, batchUpdateHandler);
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_activate:
                setNotificationsActiveState(adapter.getSelectedNotifications(), true);
                invalidateOptionsMenuCompatible();
                return true;

            case R.id.menu_deactivate:
                setNotificationsActiveState(adapter.getSelectedNotifications(), false);
                invalidateOptionsMenuCompatible();
                return true;

            case R.id.menu_delete:
                deleteNotifications(adapter.getSelectedNotifications());
                invalidateOptionsMenuCompatible();
                return true;
            case R.id.menu_add:
                invalidateOptionsMenuCompatible();
                GCNotificationEditActivity.startActivityNewGCNotification(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
