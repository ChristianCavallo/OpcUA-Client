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
import androidx.recyclerview.widget.RecyclerView;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.core.Core;
import com.ccdev.opcua_client.elements.CustomizedElement;
import com.ccdev.opcua_client.elements.Pump;
import com.ccdev.opcua_client.elements.Sensor;
import com.ccdev.opcua_client.elements.Tank;
import com.ccdev.opcua_client.elements.Valve;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;
import com.lukelorusso.verticalseekbar.VerticalSeekBar;

import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.MonitoringMode;
import org.w3c.dom.Text;

import java.util.List;

public class CustomizedElementAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TankView = 0;
    public static final int PumpView = 1;
    public static final int SensorView = 2;
    public static final int ValveView = 3;

    List<CustomizedElement> elements;

    Context context;
    ProgressDialog dialog;

    public CustomizedElementAdapter(List<CustomizedElement> elements) {
        this.elements = elements;
    }

    @Override
    public int getItemViewType(int position) {
        if(elements.get(position) instanceof Tank){
            return TankView;
        }

        if(elements.get(position) instanceof Valve){
            return ValveView;
        }

        if(elements.get(position) instanceof Sensor){
            return SensorView;
        }

        if(elements.get(position) instanceof Pump){
            return PumpView;
        }

        return SensorView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView;
        switch(viewType){
            case TankView:
                contactView = inflater.inflate(R.layout.custom_listitem_tank, parent, false);
                return new TankViewHolder(contactView);

            case ValveView:

            case PumpView:

            case SensorView:

            default:

                break;
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final CustomizedElement element = elements.get(position);

        ImageView monitorButton = null;
        ImageView statusButton = null;
        ImageView removeButton = null;

        switch(holder.getItemViewType()){
            case TankView:
            {
                Tank t = (Tank) element;
                TankViewHolder h = (TankViewHolder) holder;
                monitorButton = h.monitorView;
                statusButton = h.statusView;
                removeButton = h.removeView;

                h.nameText.setText(element.getName());
                String value = t.getMonitoredItem().getNotifies().get(0).getValue().getValue().toString();
                try{
                    double input = new Double(value);
                    double percentage = ((input - t.getMinValue()) * 100) / (t.getMaxValue() - t.getMinValue());
                    percentage = Math.round(percentage * 100.0) / 100.0;
                    h.levelText.setText(percentage + "%");

                    h.levelBar.setProgress((int) percentage);

                }catch (Exception e){
                    h.levelText.setText("Wrong data type...");
                    h.levelBar.setProgress(0);
                }

                break;
            }

            case ValveView:

            case PumpView:

            case SensorView:

            default:

                break;
        }

        if(monitorButton == null || statusButton == null || removeButton == null){
            return;
        }

        monitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        if(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getMonitoringMode() == MonitoringMode.Sampling){
            statusButton.setImageResource(R.drawable.play_24dp);
        } else {
            statusButton.setImageResource(R.drawable.pause_24dp);
        }

        statusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = ProgressDialog.show(context, "",
                        "Switching state...", true);
                switchMonitoringMode(element.getMonitoredItem());
            }
        });

        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Do you really want to remove this custom monitored item?")
                        .setTitle("Deleting request?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeMonitoredItem(element.getMonitoredItem());
                    }
                });

                builder.setNegativeButton("No", null);
                builder.show();
            }
        });
    }


    private void switchMonitoringMode(final ExtendedMonitoredItem monitoredItem){
        if(Looper.myLooper() == Looper.getMainLooper()){
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
                    if(result){
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

    private void removeMonitoredItem(final ExtendedMonitoredItem m){
        if(Looper.myLooper() == Looper.getMainLooper()){
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

                    if(result){
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
        return elements.size();
    }


    public class TankViewHolder extends RecyclerView.ViewHolder {
        ImageView monitorView;
        ImageView statusView;
        ImageView removeView;

        TextView nameText;
        TextView levelText;

        VerticalSeekBar levelBar;

        public TankViewHolder(@NonNull View itemView) {
            super(itemView);
            monitorView = (ImageView) itemView.findViewById(R.id.tankMonitorImageView);
            statusView = (ImageView) itemView.findViewById(R.id.tankStatusImageView);
            removeView = (ImageView) itemView.findViewById(R.id.tankRemoveImageView);

            nameText = (TextView) itemView.findViewById(R.id.tankNameTextView);
            levelText = (TextView) itemView.findViewById(R.id.tankLevelTextView);
            levelBar = (VerticalSeekBar) itemView.findViewById(R.id.tankLevelBar);
        }
    }

    public class PumpViewHolder extends RecyclerView.ViewHolder {

        public PumpViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class ValveViewHolder extends RecyclerView.ViewHolder {

        public ValveViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class SensorViewHolder extends RecyclerView.ViewHolder {

        public SensorViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

}
