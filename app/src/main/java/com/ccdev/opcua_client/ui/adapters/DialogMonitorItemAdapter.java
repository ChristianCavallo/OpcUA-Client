package com.ccdev.opcua_client.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

import org.opcfoundation.ua.core.MonitoredItemNotification;

import java.util.List;

public class DialogMonitorItemAdapter extends RecyclerView.Adapter<DialogMonitorItemAdapter.DialogMonitorItemViewHolder> {

    List<MonitoredItemNotification> notificationList;

    public DialogMonitorItemAdapter(List<MonitoredItemNotification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public DialogMonitorItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.listitem_dialog_monitor, parent, false);

        // Return a new holder instance
        DialogMonitorItemViewHolder viewHolder = new DialogMonitorItemViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull DialogMonitorItemViewHolder holder, int position) {
        MonitoredItemNotification m = notificationList.get(position);

        holder.valueText.setText(m.getValue().getValue().toString());
        String timestamp = "";
        if(m.getValue().getServerTimestamp() != null){
            timestamp += "Server -> " + m.getValue().getServerTimestamp().toString() + "\n";
        }

        if(m.getValue().getSourceTimestamp() != null){
            timestamp += "Source -> " + m.getValue().getSourceTimestamp();
        }

        holder.timestampText.setText(timestamp.trim());
        m.getValue().getValue().toString();
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public class DialogMonitorItemViewHolder extends RecyclerView.ViewHolder{

        TextView timestampText;
        TextView valueText;

        public DialogMonitorItemViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampText = (TextView) itemView.findViewById(R.id.item_dmTimestampTextView);
            valueText = (TextView) itemView.findViewById(R.id.item_dmValueTextView);
        }
    }
}
