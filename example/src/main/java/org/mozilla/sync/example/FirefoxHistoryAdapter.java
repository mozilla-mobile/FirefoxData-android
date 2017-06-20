package org.mozilla.sync.example;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.mozilla.sync.sync.HistoryRecord;

import java.util.Collections;
import java.util.List;

// todo: licenses.
class FirefoxHistoryAdapter extends RecyclerView.Adapter<FirefoxHistoryAdapter.HistoryViewHolder> {

    private List<HistoryRecord> historyRecords = Collections.emptyList();

    void setHistoryRecords(final List<HistoryRecord> historyRecords) {
        this.historyRecords = historyRecords;
        notifyDataSetChanged();
    }

    @Override
    public HistoryViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        return new HistoryViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(final HistoryViewHolder holder, final int position) {
        final HistoryRecord record = historyRecords.get(position);
        holder.titleView.setText(record.getTitle());
        holder.uriView.setText(record.getURI());
    }

    @Override
    public int getItemCount() { return historyRecords.size(); }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView uriView;

        HistoryViewHolder(final View itemView) {
            super(itemView);
            titleView = (TextView) itemView.findViewById(R.id.title);
            uriView = (TextView) itemView.findViewById(R.id.uri);
        }
    }
}
