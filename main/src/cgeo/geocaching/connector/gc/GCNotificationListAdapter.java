package cgeo.geocaching.connector.gc;

import butterknife.BindView;

import cgeo.geocaching.R;
import cgeo.geocaching.models.GCNotification;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

class GCNotificationListAdapter extends RecyclerView.Adapter<GCNotificationListAdapter.ViewHolder> {

    @NonNull
    private final List<GCNotification> notifications;

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.notification_selected)
        CheckBox selected;
        @BindView(R.id.notification_activated)
        ImageView activated;
        @BindView(R.id.label)
        TextView label;
        @BindView(R.id.notification_coordinates)
        TextView notificationCoordinates;
        @BindView(R.id.loading)
        ProgressBar progressBar;

        ViewHolder(final View view) {
            super(view);
        }
    }

    GCNotificationListAdapter(@NonNull final List<GCNotification> notifications) {
        this.notifications = notifications;
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gcnotification_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final GCNotification notification = notifications.get(position);

        holder.selected.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, final boolean b) {
                notification.setChecked(b);
            }
        });
        /*
        holder.cachelist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Activity activity = (Activity) v.getContext();
                CacheListActivity.startActivityPocket(activity, notification);
            }
        });

        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Activity activity = (Activity) v.getContext();
                CacheListActivity.startActivityPocketDownload(activity, pocketQuery);
            }
        });

        holder.download.setVisibility(notification.isDownloadable() ? View.VISIBLE : View.GONE);
        */
        holder.name.setText(notification.getName());
        holder.activated.setVisibility(notification.isEnabled() ? View.VISIBLE : View.GONE);
        holder.label.setText(notification.getCacheType() + " " + notification.getLogTypes().toString());
        if (notification.getCoords() != null) {
            holder.notificationCoordinates.setText(notification.getCoords().toString() + " [" + notification.getDistance() + "km]");
            holder.notificationCoordinates.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.GONE);
        } else {
            holder.notificationCoordinates.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.VISIBLE);
        }
        holder.selected.setChecked(notification.isChecked());
    }

    public ArrayList<GCNotification> getSelectedNotifications() {
        ArrayList<GCNotification> selectedNotifications = new ArrayList<GCNotification>();
        for (GCNotification i : notifications) {
            if (i.isChecked()) {
                selectedNotifications.add(i);
            }
        }
        return selectedNotifications;
    }

}
