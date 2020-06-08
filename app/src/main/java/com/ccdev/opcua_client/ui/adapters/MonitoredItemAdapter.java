package com.ccdev.opcua_client.ui.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.core.Core;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.MonitoringMode;

import java.util.List;

public class MonitoredItemAdapter extends RecyclerView.Adapter<MonitoredItemAdapter.MonitoredItemViewHolder> {
    List<ExtendedMonitoredItem> monitoredItemList;

    ProgressDialog dialog;
    Context context;
    DialogMonitorItemAdapter dialogAdapter;


    public MonitoredItemAdapter(List<ExtendedMonitoredItem> monitoredItemList) {
        this.monitoredItemList = monitoredItemList;
    }

    @NonNull
    @Override
    public MonitoredItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.listitem_monitoreditem, parent, false);

        // Return a new holder instance
        MonitoredItemViewHolder viewHolder = new MonitoredItemViewHolder(contactView);


        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MonitoredItemViewHolder holder, int position) {
        final ExtendedMonitoredItem m = monitoredItemList.get(position);

        holder.itemName.setText(m.getNodeName());
        holder.itemMode.setText(m.getRequest().getItemsToCreate()[0].getMonitoringMode().name());
        holder.itemSampling.setText(m.getMonitoredItem().getRevisedSamplingInterval().intValue() + "");
        holder.itemQueueSize.setText(m.getMonitoredItem().getRevisedQueueSize().intValue() + "");

        if (m.getNotifies().isEmpty()) {
            holder.itemValue.setText("...");
        } else {
            holder.itemValue.setText(m.getNotifies().get(0).getValue().getValue().toString());
        }

        holder.itemRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Do you really want to remove this monitored item?")
                        .setTitle("Deleting request?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeMonitoredItem(m);
                    }
                });

                builder.setNegativeButton("No", null);
                builder.show();
            }
        });

        if (m.getRequest().getItemsToCreate()[0].getMonitoringMode() == MonitoringMode.Sampling) {
            holder.itemStatusButton.setImageResource(R.drawable.play_24dp);
        } else {
            holder.itemStatusButton.setImageResource(R.drawable.pause_24dp);
        }

        holder.itemStatusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(context, "",
                        "Switching monitoring mode...", true);
                switchMonitoringMode(m);
            }
        });

        holder.itemMonitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder;
                View dialogView;
                TextView nameText;
                RecyclerView monitorList;
                builder = new AlertDialog.Builder(context);

                dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_item_monitor, null);

                nameText = (TextView) dialogView.findViewById(R.id.dmNameTextView);
                monitorList = (RecyclerView) dialogView.findViewById(R.id.dmRecyclerView);

                builder.setView(dialogView);

                nameText.setText(m.getNodeName());
                dialogAdapter = new DialogMonitorItemAdapter(m.getNotifies());
                monitorList.setLayoutManager(new LinearLayoutManager(context));
                monitorList.setAdapter(dialogAdapter);

                builder.setNegativeButton("CLOSE", null);
                builder.show();
            }
        });

    }

    public void notifyAdapterDatasetChanged() {
        notifyDataSetChanged();

        if (dialogAdapter != null) {
            dialogAdapter.notifyDataSetChanged();
        }
    }

    private void switchMonitoringMode(final ExtendedMonitoredItem monitoredItem) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    switchMonitoringMode(monitoredItem);
                }
            }).start();
            return;
        }


        try {
            final boolean result = Core.getInstance().switchMonitoringMode(monitoredItem);

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    if (result) {
                        Toast.makeText(context, "Monitoring mode switched.", Toast.LENGTH_LONG).show();
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(context, "Something went wrong. Try again.", Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (final ServiceResultException e) {
            e.printStackTrace();
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Toast.makeText(context, "Error: " + e.getStatusCode().getDescription() + ". Code: " + e.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                }
            });
        }


    }

    private void removeMonitoredItem(final ExtendedMonitoredItem m) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    removeMonitoredItem(m);
                }
            }).start();
            return;
        }

        try {
            final boolean result = Core.getInstance().removeMonitoredItem(m);

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {

                    if (result) {
                        Toast.makeText(context, "Monitored item removed.", Toast.LENGTH_LONG).show();
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(context, "Something went wrong. Try again.", Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (final ServiceResultException e) {
            e.printStackTrace();
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {

                    Toast.makeText(context, "Error: " + e.getStatusCode().getDescription() + ". Code: " + e.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();

                }
            });
        }


    }

    @Override
    public int getItemCount() {
        return monitoredItemList.size();
    }

    public class MonitoredItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemName;
        TextView itemMode;
        TextView itemSampling;
        TextView itemQueueSize;
        TextView itemValue;
        ImageView itemRemoveButton;
        ImageView itemMonitorButton;
        ImageView itemStatusButton;

        public MonitoredItemViewHolder(View convertView) {
            super(convertView);

            itemName = (TextView) convertView.findViewById(R.id.monItemNameText);
            itemMode = (TextView) convertView.findViewById(R.id.monItemModeText);
            itemSampling = (TextView) convertView.findViewById(R.id.monItemSamplingText);
            itemQueueSize = (TextView) convertView.findViewById(R.id.monItemQueueSizeText);
            itemValue = (TextView) convertView.findViewById(R.id.monItemValueText);
            itemRemoveButton = (ImageView) convertView.findViewById(R.id.monItemRemoveView);
            itemMonitorButton = (ImageView) convertView.findViewById(R.id.monItemMonitorView);
            itemStatusButton = (ImageView) convertView.findViewById(R.id.monItemStateView);
        }
    }
}
