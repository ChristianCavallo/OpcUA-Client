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
import android.widget.ProgressBar;
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
import com.ccdev.opcua_client.ui.home.HomeFragment;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;
import com.lukelorusso.verticalseekbar.VerticalSeekBar;

import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.MonitoringMode;

import java.util.List;

public class CustomizedElementAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TankView = 0;
    public static final int PumpView = 1;
    public static final int SensorView = 2;
    public static final int ValveView = 3;

    List<CustomizedElement> elements;

    Context context;
    ProgressDialog dialog;

    HomeFragment parent;

    public CustomizedElementAdapter(List<CustomizedElement> elements, HomeFragment parent) {
        this.elements = elements;
        this.parent = parent;
    }

    @Override
    public int getItemViewType(int position) {
        if (elements.get(position) instanceof Tank) {
            return TankView;
        }

        if (elements.get(position) instanceof Valve) {
            return ValveView;
        }

        if (elements.get(position) instanceof Sensor) {
            return SensorView;
        }

        if (elements.get(position) instanceof Pump) {
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
        switch (viewType) {
            case TankView:
                contactView = inflater.inflate(R.layout.custom_listitem_tank, parent, false);
                return new TankViewHolder(contactView);

            case ValveView:
                contactView = inflater.inflate(R.layout.custom_listitem_valve, parent, false);
                return new ValveViewHolder(contactView);
            case PumpView:
                contactView = inflater.inflate(R.layout.custom_listitem_pump, parent, false);
                return new PumpViewHolder(contactView);
            case SensorView:
                contactView = inflater.inflate(R.layout.custom_listitem_sensor, parent, false);
                return new SensorViewHolder(contactView);
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

        switch (holder.getItemViewType()) {
            case TankView: {
                Tank t = (Tank) element;
                TankViewHolder h = (TankViewHolder) holder;
                monitorButton = h.monitorView;
                statusButton = h.statusView;
                removeButton = h.removeView;


                h.nameText.setText(element.getName());
                h.MonitoredItemView.setText(element.getMonitoredItem().getNodeName());
                h.NamespaceView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getNamespaceIndex() + "");
                h.NodeIndexView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getValue().toString());

                String value = t.getMonitoredItem().getNotifies().get(0).getValue().getValue().toString();
                try {
                    double input = new Double(value);
                    double percentage = ((input - t.getMinValue()) * 100) / (t.getMaxValue() - t.getMinValue());
                    percentage = Math.round(percentage * 100.0) / 100.0;
                    h.levelText.setText(percentage + "%");

                    h.levelBar.setProgress((int) percentage);

                } catch (Exception e) {
                    h.levelText.setText("Wrong data type...");
                    h.levelBar.setProgress(0);
                }

                break;
            }

            case ValveView: {

                Valve v = (Valve) element;
                ValveViewHolder h = (ValveViewHolder) holder;
                monitorButton = h.monitorView;
                statusButton = h.statusView;
                removeButton = h.removeView;


                h.nameText.setText(element.getName());

                h.MonitoredItemView.setText(element.getMonitoredItem().getNodeName());
                h.NamespaceView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getNamespaceIndex() + "");
                h.NodeIndexView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getValue().toString());
                String value = v.getMonitoredItem().getNotifies().get(0).getValue().getValue().toString();

                if (v.getOpenValue().toLowerCase().equals(value.toLowerCase())) {
                    h.StateImageView.setImageResource(R.drawable.ic_valve_open);
                    h.StateView.setText("Opened");

                } else {
                    h.StateImageView.setImageResource(R.drawable.ic_valve_closed);
                    h.StateView.setText("Closed");
                }

            }
            break;
            case PumpView: {
                Pump p = (Pump) element;
                PumpViewHolder h = (PumpViewHolder) holder;
                monitorButton = h.monitorView;
                statusButton = h.statusView;
                removeButton = h.removeView;

                h.nameText.setText(element.getName());

                h.MonitoredItemView.setText(element.getMonitoredItem().getNodeName());
                h.NamespaceView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getNamespaceIndex() + "");
                h.NodeIndexView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getValue().toString());
                String value = p.getMonitoredItem().getNotifies().get(0).getValue().getValue().toString();

                try {
                    double input = new Double(value);
                    double percentage = ((input - p.getMinRPM()) * 100) / (p.getMaxRPM() - p.getMinRPM());
                    percentage = Math.round(percentage * 100.0) / 100.0;

                    h.ValueRpmView.setText(value + " RPM");

                    h.ProgressBarView.setProgress((int) percentage);

                } catch (Exception e) {
                    h.ValueRpmView.setText("Wrong data type...");
                    h.ProgressBarView.setProgress(0);
                }

            }
            break;

            case SensorView: {

                Sensor s = (Sensor) element;
                SensorViewHolder h = (SensorViewHolder) holder;
                monitorButton = h.monitorView;
                statusButton = h.statusView;
                removeButton = h.removeView;

                h.nameText.setText(element.getName());

                h.MonitoredItemView.setText(element.getMonitoredItem().getNodeName());
                h.NamespaceView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getNamespaceIndex() + "");
                h.NodeIndexView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getValue().toString());
                String value = s.getMonitoredItem().getNotifies().get(0).getValue().getValue().toString();

                if (s.getMinValue() == -1 && s.getMaxValue() == -1) {

                    h.ValueView.setText(value);
                    h.ProgressBarView.setVisibility(View.GONE);
                    h.ValueBarView.setVisibility(View.GONE);
                    break;
                }
                try {
                    double input = new Double(value);
                    double percentage = ((input - s.getMinValue()) * 100) / (s.getMaxValue() - s.getMinValue());
                    percentage = Math.round(percentage * 100.0) / 100.0;

                    h.ValueView.setText(value);
                    h.ValueBarView.setText(percentage + "%");
                    h.ProgressBarView.setVisibility(View.VISIBLE);
                    h.ProgressBarView.setProgress((int) percentage);

                } catch (Exception e) {
                    h.ValueView.setText("Wrong data type...");
                    h.ProgressBarView.setProgress(0);
                }

            }
            break;

            default:

                break;
        }

        if (monitorButton == null || statusButton == null || removeButton == null) {
            return;
        }

        monitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        if (element.getMonitoredItem().getRequest().getItemsToCreate()[0].getMonitoringMode() == MonitoringMode.Sampling) {
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

        monitorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.ShowChart(element);
            }
        });
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
        return elements.size();
    }


    public class TankViewHolder extends RecyclerView.ViewHolder {
        ImageView monitorView;
        ImageView statusView;
        ImageView removeView;

        TextView MonitoredItemView;
        TextView NodeIndexView;
        TextView NamespaceView;

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

            MonitoredItemView = (TextView) itemView.findViewById(R.id.tankMonitoredItemTextView);
            NodeIndexView = (TextView) itemView.findViewById(R.id.tankNodeIndexTextView);
            NamespaceView = (TextView) itemView.findViewById(R.id.tankNamespaceTextView);
        }
    }

    public class PumpViewHolder extends RecyclerView.ViewHolder {

        ImageView monitorView;
        ImageView statusView;
        ImageView removeView;

        TextView nameText;
        TextView MonitoredItemView;
        TextView NodeIndexView;
        TextView NamespaceView;
        TextView ValueRpmView;
        ProgressBar ProgressBarView;

        public PumpViewHolder(@NonNull View itemView) {
            super(itemView);

            monitorView = (ImageView) itemView.findViewById(R.id.pumpMonitorImageView);
            statusView = (ImageView) itemView.findViewById(R.id.pumpStatusImageView);
            removeView = (ImageView) itemView.findViewById(R.id.pumpRemoveImageView);

            nameText = (TextView) itemView.findViewById(R.id.pumpNameTextView);
            MonitoredItemView = (TextView) itemView.findViewById(R.id.pumpMonitoredItemTextView);
            NodeIndexView = (TextView) itemView.findViewById(R.id.pumpNodeIndexTextView);
            NamespaceView = (TextView) itemView.findViewById(R.id.pumpNamespaceTextView);
            ValueRpmView = (TextView) itemView.findViewById(R.id.pumpValueRpmTextView);
            ProgressBarView = (ProgressBar) itemView.findViewById(R.id.pumpProgressBarView);
        }
    }

    public class ValveViewHolder extends RecyclerView.ViewHolder {

        ImageView monitorView;
        ImageView statusView;
        ImageView removeView;

        TextView nameText;
        TextView MonitoredItemView;
        TextView NodeIndexView;
        TextView NamespaceView;
        TextView StateView;

        ImageView StateImageView;

        public ValveViewHolder(@NonNull View itemView) {
            super(itemView);

            monitorView = (ImageView) itemView.findViewById(R.id.valveMonitorImageView);
            statusView = (ImageView) itemView.findViewById(R.id.valveStatusImageView);
            removeView = (ImageView) itemView.findViewById(R.id.valveRemoveImageView);

            nameText = (TextView) itemView.findViewById(R.id.valveNameTextView);
            MonitoredItemView = (TextView) itemView.findViewById(R.id.valveMonitoredItemTextView);
            NodeIndexView = (TextView) itemView.findViewById(R.id.valveNodeIndexTextView);
            NamespaceView = (TextView) itemView.findViewById(R.id.valveNamespaceTextView);
            StateView = (TextView) itemView.findViewById(R.id.valveStateTextView);

            StateImageView = (ImageView) itemView.findViewById(R.id.valveStateImageView);
        }
    }

    public class SensorViewHolder extends RecyclerView.ViewHolder {

        ImageView monitorView;
        ImageView statusView;
        ImageView removeView;

        TextView nameText;
        TextView MonitoredItemView;
        TextView NodeIndexView;
        TextView NamespaceView;
        TextView ValueView;
        TextView ValueBarView;
        VerticalSeekBar ProgressBarView;

        public SensorViewHolder(@NonNull View itemView) {
            super(itemView);

            monitorView = (ImageView) itemView.findViewById(R.id.sensorMonitorImageView);
            statusView = (ImageView) itemView.findViewById(R.id.sensorStatusImageView);
            removeView = (ImageView) itemView.findViewById(R.id.sensorRemoveImageView);

            nameText = (TextView) itemView.findViewById(R.id.sensorNameTextView);
            MonitoredItemView = (TextView) itemView.findViewById(R.id.sensorMonitoredItemTextView);
            NodeIndexView = (TextView) itemView.findViewById(R.id.sensorNodeIndexTextView);
            NamespaceView = (TextView) itemView.findViewById(R.id.sensorNamespaceTextView);
            ValueView = (TextView) itemView.findViewById(R.id.sensorValueTextView);
            ValueBarView = (TextView) itemView.findViewById(R.id.sensorValueBarTextView);
            ProgressBarView = (VerticalSeekBar) itemView.findViewById(R.id.sensorProgressBarTextView);


        }
    }

}
