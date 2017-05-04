package cgeo.geocaching.connector.gc;

import butterknife.BindView;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.Log;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.ArrayList;

public class CacheTypeGridViewAdapter extends RecyclerView.Adapter<CacheTypeGridViewAdapter.ViewHolder> {

    public static class CacheTypeSelectionHolder {
        private final CacheType ct;
        private boolean selected;

        public CacheTypeSelectionHolder(CacheType ct) {
            this.ct = ct;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public void toggleSelection() {
            this.selected = !this.selected;
        }

        @Override
        public String toString() {
            return selected ? "[" + ct + "]" : "{" + ct + "}";
        }
    }

    @NonNull
    private final ArrayList<CacheTypeSelectionHolder> cacheTypeSelection;

    CacheTypeGridViewAdapter(final ArrayList<CacheTypeSelectionHolder> cacheTypeSelection) {
        this.cacheTypeSelection = cacheTypeSelection;
    }

    protected final class ViewHolder extends AbstractRecyclerViewHolder {
        @BindView(R.id.icon)
        ImageButton icon;

        ViewHolder(final View view) {
            super(view);
        }
    }

    @Override
    public CacheTypeGridViewAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cachetype_selection_grid_button, parent, false);
        return new CacheTypeGridViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final CacheTypeGridViewAdapter.ViewHolder holder, final int position) {
        final CacheTypeSelectionHolder cth = cacheTypeSelection.get(position);
        holder.icon.setImageResource(cth.ct.markerId);

        if (cth.isSelected()) {
            holder.icon.setBackgroundColor(holder.icon.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.icon.setBackgroundColor(holder.icon.getResources().getColor(android.R.color.darker_gray));
        }

        holder.icon.setOnClickListener(

                new ImageButton.OnClickListener() {
                    @Override
                    public void onClick(final View view) {
                        Log.d("Clicked on: " + cth.ct);
                        cth.toggleSelection();
                        notifyItemChanged(position);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return cacheTypeSelection.size();
    }

}