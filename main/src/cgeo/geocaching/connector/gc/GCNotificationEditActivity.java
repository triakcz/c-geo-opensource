package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.connector.gc.CacheTypeGridViewAdapter.CacheTypeSelectionHolder;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.ui.recyclerview.RecyclerViewProvider;
import cgeo.geocaching.utils.Log;

import org.androidannotations.annotations.EActivity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

@EActivity
public class GCNotificationEditActivity extends AbstractActionBarActivity {

    private final ArrayList<CacheTypeSelectionHolder> cacheTypeSelection = new ArrayList<>();
    private final CacheTypeGridViewAdapter cacheTypeGridViewAdapter = new CacheTypeGridViewAdapter(cacheTypeSelection);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.gcnotification_edit_activity);

        final RecyclerView view = RecyclerViewProvider.provideRecyclerView(this, R.id.cache_type_selector, true, true);
        view.setAdapter(cacheTypeGridViewAdapter);
        view.setLayoutManager(new GridLayoutManager(this, 7));
        boolean be_selected = false;
        for (CacheType ct : CacheType.values()) {
            Log.d(ct.toString());
            final CacheTypeSelectionHolder cth = new CacheTypeGridViewAdapter.CacheTypeSelectionHolder(ct);
            cth.setSelected(be_selected);
            be_selected = !be_selected;
            cacheTypeSelection.add(cth);
        }
        cacheTypeGridViewAdapter.notifyItemRangeInserted(0, cacheTypeSelection.size());
        cacheTypeGridViewAdapter.notifyDataSetChanged();
        Log.d("Dataset: " + cacheTypeSelection);
    }

    public static void startActivityNewGCNotification(final Context context) {
        GCNotificationEditActivity_.intent(context).start();
    }
}
