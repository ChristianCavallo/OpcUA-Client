package com.ccdev.opcua_client.ui.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.ccdev.opcua_client.wrappers.ExtendedSubscription;

import org.opcfoundation.ua.common.ServiceResultException;

import java.util.List;

public class SubscriptionAdapter extends RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder> {
    ProgressDialog dialog;

    List<ExtendedSubscription> subscriptionList;
    Context context;


    public SubscriptionAdapter(List<ExtendedSubscription> subscriptionList) {
        this.subscriptionList = subscriptionList;
    }

    @NonNull
    @Override
    public SubscriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.listitem_subscription, parent, false);

        // Return a new holder instance
        SubscriptionViewHolder viewHolder = new SubscriptionViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final SubscriptionViewHolder holder, int position) {
        final ExtendedSubscription subscription = subscriptionList.get(position);

        if(!subscription.getRequest().getPublishingEnabled()){
            holder.statusButton.setImageResource(R.drawable.play_24dp);
        } else {
            holder.statusButton.setImageResource(R.drawable.pause_24dp);
        }

        holder.statusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(context, "",
                        "Switching state...", true);
                switchPublishingEnabled(subscription);
            }
        });

        holder.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Do you really want to remove this subscription?")
                        .setTitle("Deleting request?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeSubscription(subscription);
                    }
                });
                builder.setNegativeButton("No", null);
                builder.show();
            }
        });

        holder.subscriptionName.setText(subscription.getName());
        holder.subscriptionId.setText("id: " + subscription.getResponse().getSubscriptionId().intValue());

        if(holder.itemAdapter == null){
            LinearLayoutManager llm = new LinearLayoutManager(context);
            holder.itemAdapter = new MonitoredItemAdapter(subscription.getMonitoredItems());
            holder.monitoredItemsList.setAdapter(holder.itemAdapter);
            holder.monitoredItemsList.setLayoutManager(llm);
        } else {
            holder.itemAdapter.notifyAdapterDatasetChanged();
        }
    }

    private void switchPublishingEnabled(final ExtendedSubscription subscription){
        if(Looper.myLooper() == Looper.getMainLooper()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    switchPublishingEnabled(subscription);
                }
            }).start();
            return;
        }


        try {

            final boolean result = Core.getInstance().switchPublishingSubscription(subscription);
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    if(result){
                        Toast.makeText(context, "Status switched for the subscription.", Toast.LENGTH_LONG).show();
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

    private void removeSubscription(final ExtendedSubscription subscription){
        if(Looper.myLooper() == Looper.getMainLooper()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    removeSubscription(subscription);
                }
            }).start();
            return;
        }

        try {
            final boolean result = Core.getInstance().removeSubscription(subscription);

            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if(result){
                        Toast.makeText(context, "Subscription removed.", Toast.LENGTH_LONG).show();
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

    @Override
    public int getItemCount() {
        return subscriptionList.size();
    }

    public class SubscriptionViewHolder extends RecyclerView.ViewHolder {

        public ImageView statusButton;
        public ImageView removeButton;

        public TextView subscriptionName;
        public TextView subscriptionId;
        public RecyclerView monitoredItemsList;
        MonitoredItemAdapter itemAdapter;

        public SubscriptionViewHolder(View convertView) {
            super(convertView);

            statusButton = (ImageView) convertView.findViewById(R.id.subItemStatusView);
            removeButton = (ImageView) convertView.findViewById(R.id.subItemRemoveView);
            subscriptionName = (TextView) convertView.findViewById(R.id.subItemNameText);
            subscriptionId = (TextView) convertView.findViewById(R.id.subItemIdText);
            monitoredItemsList = (RecyclerView) convertView.findViewById(R.id.monitoredItemsList);
        }
    }

}
