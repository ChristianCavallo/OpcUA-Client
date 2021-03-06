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
import android.widget.FrameLayout;
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
import com.ccdev.opcua_client.ui.home.HomeFragment;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;
import com.github.anastr.speedviewlib.SpeedView;
import com.lukelorusso.verticalseekbar.VerticalSeekBar;

import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.MonitoringMode;

import java.util.List;

public class CustomizedElementAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TankView = 0;
    public static final int PumpView = 1;
    public static final int SensorView = 2;
    public static final int ValveView = 3;

    public static final int TemperatureSensorView = 4;

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
            Sensor s = (Sensor) elements.get(position);
            if (s.getCategory() == Sensor.Category.TEMPERATURE) {
                return TemperatureSensorView;
            }
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
                contactView = inflater.inflate(R.layout.custom_listitem_generic, parent, false);
                return new GenericViewHolder(contactView);
            case SensorView:
                contactView = inflater.inflate(R.layout.custom_listitem_generic, parent, false);
                return new GenericViewHolder(contactView);

            case TemperatureSensorView:
                contactView = inflater.inflate(R.layout.custom_listitem_sensor_temperature, parent, false);
                return new TemperatureViewHolder(contactView);

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

                h.rangeText.setText("[" + t.getMinValue() + ", " + t.getMaxValue() + "]");

                h.speedometer.setMinSpeed(new Float(t.getMinValue()));
                h.speedometer.setMaxSpeed(new Float(t.getMaxValue()));

                h.speedometer.setUnit(t.getUnit());

                switch (t.getVisualization()) {
                    case PROGRESS_BAR:
                        h.progressBarLayout.setVisibility(View.VISIBLE);
                        h.speedometer.setVisibility(View.GONE);
                        break;
                    case INDICATOR:
                        h.progressBarLayout.setVisibility(View.GONE);
                        h.speedometer.setVisibility(View.VISIBLE);
                        break;
                    case BOTH:
                        h.progressBarLayout.setVisibility(View.VISIBLE);
                        h.speedometer.setVisibility(View.VISIBLE);
                        break;
                }

                try {
                    String s = t.getMonitoredItem().getNotifications().get(0).getValue().getValue().toString();
                    double value = new Double(s);
                    value = Math.round(value * 100) / 100;
                    h.valueText.setText(value + " " + t.getUnit());
                    h.speedometer.speedTo(new Float(value));
                    double percentage = ((value - t.getMinValue()) * 100) / (t.getMaxValue() - t.getMinValue());
                    percentage = Math.round(percentage * 100.0) / 100.0;
                    h.levelBar.setProgress((int) percentage);
                } catch (Exception e) {
                    h.valueText.setText("Wrong data type...");
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
                String value = v.getMonitoredItem().getNotifications().get(0).getValue().getValue().toString();

                if (v.getOpenValue().toLowerCase().equals(value.toLowerCase())) {
                    h.StateImageView.setImageResource(R.drawable.ic_valve_open);
                    h.StateView.setText("Opened");

                } else {
                    h.StateImageView.setImageResource(R.drawable.ic_valve_closed);
                    h.StateView.setText("Closed");
                }

                h.valueText.setText(value);

            }
            break;
            case PumpView: {
                Pump p = (Pump) element;
                GenericViewHolder h = (GenericViewHolder) holder;
                monitorButton = h.monitorView;
                statusButton = h.statusView;
                removeButton = h.removeView;

                h.nameText.setText(element.getName());

                h.MonitoredItemView.setText(element.getMonitoredItem().getNodeName());
                h.NamespaceView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getNamespaceIndex() + "");
                h.NodeIndexView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getValue().toString());

                h.rangeText.setText("[" + p.getMinValue() + ", " + p.getMaxValue() + "]");
                h.imageView.setImageResource(R.drawable.ic_pump);

                h.speedometer.setMinSpeed(new Float(p.getMinValue()));
                h.speedometer.setMaxSpeed(new Float(p.getMaxValue()));

                h.speedometer.setUnit(p.getUnit());


                switch (p.getVisualization()) {
                    case PROGRESS_BAR:
                        h.progressBarLayout.setVisibility(View.VISIBLE);
                        h.speedometer.setVisibility(View.GONE);
                        break;
                    case INDICATOR:
                        h.progressBarLayout.setVisibility(View.GONE);
                        h.speedometer.setVisibility(View.VISIBLE);
                        break;
                    case BOTH:
                        h.progressBarLayout.setVisibility(View.VISIBLE);
                        h.speedometer.setVisibility(View.VISIBLE);
                        break;
                }

                try {

                    String s = p.getMonitoredItem().getNotifications().get(0).getValue().getValue().toString();
                    double value = new Double(s);
                    value = Math.round(value * 100) / 100;
                    h.valueText.setText(value + " " + p.getUnit());
                    h.speedometer.speedTo(new Float(value));
                    double percentage = ((value - p.getMinValue()) * 100) / (p.getMaxValue() - p.getMinValue());
                    percentage = Math.round(percentage * 100.0) / 100.0;
                    h.levelBar.setProgress((int) percentage);

                } catch (Exception e) {
                     h.valueText.setText("Wrong data type...");
                }

            }
            break;

            case SensorView: {

                Sensor s = (Sensor) element;
                GenericViewHolder h = (GenericViewHolder) holder;
                monitorButton = h.monitorView;
                statusButton = h.statusView;
                removeButton = h.removeView;

                h.nameText.setText(element.getName());

                h.MonitoredItemView.setText(element.getMonitoredItem().getNodeName());
                h.NamespaceView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getNamespaceIndex() + "");
                h.NodeIndexView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getValue().toString());

                h.rangeText.setText("[" + s.getMinValue() + ", " + s.getMaxValue() + "]");

                h.speedometer.setMinSpeed(new Float(s.getMinValue()));
                h.speedometer.setMaxSpeed(new Float(s.getMaxValue()));

                h.speedometer.setUnit(s.getUnit());

                switch (s.getVisualization()) {
                    case PROGRESS_BAR:
                        h.progressBarLayout.setVisibility(View.VISIBLE);
                        h.speedometer.setVisibility(View.GONE);
                        break;
                    case INDICATOR:
                        h.progressBarLayout.setVisibility(View.GONE);
                        h.speedometer.setVisibility(View.VISIBLE);
                        break;
                    case BOTH:
                        h.progressBarLayout.setVisibility(View.VISIBLE);
                        h.speedometer.setVisibility(View.VISIBLE);
                        break;
                }

                try {

                    String str = s.getMonitoredItem().getNotifications().get(0).getValue().getValue().toString();
                    double value = new Double(str);
                    value = Math.round(value * 100) / 100;
                    h.valueText.setText(value + " " + s.getUnit());
                    h.speedometer.speedTo(new Float(value));
                    double percentage = ((value - s.getMinValue()) * 100) / (s.getMaxValue() - s.getMinValue());
                    percentage = Math.round(percentage * 100.0) / 100.0;
                    h.levelBar.setProgress((int) percentage);


                } catch (Exception e) {
                  h.valueText.setText("Wrong data type...");
                }

                switch (s.getCategory()){
                    case GENERIC:
                        h.imageView.setImageResource(R.drawable.ic_sensor);
                        break;
                    case HUMIDITY:
                        h.imageView.setImageResource(R.drawable.ic_humidity_sensor);
                        break;
                    case PRESSURE:
                        h.imageView.setImageResource(R.drawable.ic_pressure);
                        break;
                }

            }
            break;

            case TemperatureSensorView: {
                Sensor s = (Sensor) element;
                TemperatureViewHolder h = (TemperatureViewHolder) holder;
                monitorButton = h.monitorView;
                statusButton = h.statusView;
                removeButton = h.removeView;

                h.nameText.setText(element.getName());

                h.MonitoredItemView.setText(element.getMonitoredItem().getNodeName());
                h.NamespaceView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getNamespaceIndex() + "");
                h.NodeIndexView.setText(element.getMonitoredItem().getRequest().getItemsToCreate()[0].getItemToMonitor().getNodeId().getValue().toString());

                h.rangeText.setText("[" + s.getMinValue() + ", " + s.getMaxValue() + "]");

                h.speedometer.setMinSpeed(new Float(s.getMinValue()));
                h.speedometer.setMaxSpeed(new Float(s.getMaxValue()));

                h.speedometer.setUnit(s.getUnit());

                switch (s.getVisualization()) {
                    case PROGRESS_BAR:
                        h.progressBarLayout.setVisibility(View.VISIBLE);
                        h.speedometer.setVisibility(View.GONE);
                        break;
                    case INDICATOR:
                        h.progressBarLayout.setVisibility(View.GONE);
                        h.speedometer.setVisibility(View.VISIBLE);
                        break;
                    case BOTH:
                        h.progressBarLayout.setVisibility(View.VISIBLE);
                        h.speedometer.setVisibility(View.VISIBLE);
                        break;
                }

                try {

                    String str = s.getMonitoredItem().getNotifications().get(0).getValue().getValue().toString();
                    double value = new Double(str);
                    value = Math.round(value * 100) / 100;
                    h.valueText.setText(value + " " + s.getUnit());
                    h.speedometer.speedTo(new Float(value));
                    double percentage = ((value - s.getMinValue()) * 100) / (s.getMaxValue() - s.getMinValue());
                    percentage = Math.round(percentage * 100.0) / 100.0;
                    h.LevelBar.setProgress((int) percentage);


                } catch (Exception e) {
                    h.valueText.setText("Wrong data type...");
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
        TextView valueText;
        TextView rangeText;

        VerticalSeekBar levelBar;
        FrameLayout progressBarLayout;
        SpeedView speedometer;

        public TankViewHolder(@NonNull View itemView) {
            super(itemView);
            monitorView = itemView.findViewById(R.id.tankMonitorImageView);
            statusView = itemView.findViewById(R.id.tankStatusImageView);
            removeView = itemView.findViewById(R.id.tankRemoveImageView);

            nameText = itemView.findViewById(R.id.tankNameTextView);
            valueText = itemView.findViewById(R.id.tankValueTextView);
            rangeText = itemView.findViewById(R.id.tankRangeTextView);

            levelBar = itemView.findViewById(R.id.tankLevelBar);

            MonitoredItemView = itemView.findViewById(R.id.tankMonitoredItemTextView);
            NodeIndexView = itemView.findViewById(R.id.tankNodeIndexTextView);
            NamespaceView = itemView.findViewById(R.id.tankNamespaceTextView);

            progressBarLayout = itemView.findViewById(R.id.tankProgressBarLayout);
            speedometer = itemView.findViewById(R.id.tankSpeedView);
        }
    }

    public class GenericViewHolder extends RecyclerView.ViewHolder {

        ImageView monitorView;
        ImageView statusView;
        ImageView removeView;
        ImageView imageView;

        TextView MonitoredItemView;
        TextView NodeIndexView;
        TextView NamespaceView;

        TextView nameText;
        TextView valueText;
        TextView rangeText;

        VerticalSeekBar levelBar;
        FrameLayout progressBarLayout;
        SpeedView speedometer;


        public GenericViewHolder(@NonNull View itemView) {
            super(itemView);

            monitorView = itemView.findViewById(R.id.genericMonitorImageView);
            statusView = itemView.findViewById(R.id.genericStatusImageView);
            removeView = itemView.findViewById(R.id.genericRemoveImageView);

            MonitoredItemView = itemView.findViewById(R.id.genericMonitoredItemTextView);
            NodeIndexView = itemView.findViewById(R.id.genericNodeIndexTextView);
            NamespaceView = itemView.findViewById(R.id.genericNamespaceTextView);

            nameText = itemView.findViewById(R.id.genericNameTextView);
            rangeText = itemView.findViewById(R.id.genericRangeTextView);
            valueText = itemView.findViewById(R.id.genericValueTextView);
            imageView = itemView.findViewById(R.id.genericImageView);

            levelBar = itemView.findViewById(R.id.genericLevelBar);
            progressBarLayout = itemView.findViewById(R.id.genericProgressBarLayout);
            speedometer = itemView.findViewById(R.id.genericSpeedView);

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
        TextView valueText;

        ImageView StateImageView;

        public ValveViewHolder(@NonNull View itemView) {
            super(itemView);

            monitorView = itemView.findViewById(R.id.valveMonitorImageView);
            statusView = itemView.findViewById(R.id.valveStatusImageView);
            removeView = itemView.findViewById(R.id.valveRemoveImageView);

            nameText = itemView.findViewById(R.id.valveNameTextView);
            MonitoredItemView = itemView.findViewById(R.id.valveMonitoredItemTextView);
            NodeIndexView = itemView.findViewById(R.id.valveNodeIndexTextView);
            NamespaceView = itemView.findViewById(R.id.valveNamespaceTextView);
            StateView = itemView.findViewById(R.id.valveStateTextView);

            StateImageView = itemView.findViewById(R.id.valveStateImageView);

            valueText = itemView.findViewById(R.id.valveValueTextView);
        }
    }

    public class TemperatureViewHolder extends RecyclerView.ViewHolder {

        ImageView monitorView;
        ImageView statusView;
        ImageView removeView;
        ImageView imageView;

        TextView nameText;

        TextView MonitoredItemView;
        TextView NodeIndexView;
        TextView NamespaceView;

        TextView valueText;
        TextView rangeText;

        //TemperatureLevelBar:
        VerticalSeekBar LevelBar;
        SpeedView speedometer;
        FrameLayout progressBarLayout;

        public TemperatureViewHolder(@NonNull View itemView) {
            super(itemView);

            monitorView = itemView.findViewById(R.id.temperatureMonitorImageView);
            statusView = itemView.findViewById(R.id.temperatureStatusImageView);
            removeView = itemView.findViewById(R.id.temperatureRemoveImageView);

            nameText = itemView.findViewById(R.id.temperatureNameTextView);
            MonitoredItemView = itemView.findViewById(R.id.temperatureMonitoredItemTextView);
            NodeIndexView = itemView.findViewById(R.id.temperatureNodeIndexTextView);
            NamespaceView = itemView.findViewById(R.id.temperatureNamespaceTextView);
            valueText = itemView.findViewById(R.id.temperatureValueTextView);
            rangeText = itemView.findViewById(R.id.temperatureRangeTextView);
            imageView = itemView.findViewById(R.id.temperatureimageView);
            LevelBar = itemView.findViewById(R.id.temperatureLevelBar);

            speedometer = itemView.findViewById(R.id.temperatureSpeedView);
            progressBarLayout = itemView.findViewById(R.id.temperatureProgressBarLayout);
        }
    }

}
