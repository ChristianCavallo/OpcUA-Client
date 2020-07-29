package com.ccdev.opcua_client.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.core.Core;
import com.ccdev.opcua_client.core.CoreInterface;
import com.ccdev.opcua_client.elements.CustomizedElement;
import com.ccdev.opcua_client.elements.Valve;
import com.ccdev.opcua_client.ui.adapters.CustomizedElementAdapter;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.opcfoundation.ua.core.MonitoredItemNotification;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements CoreInterface {
    TextView elementAlertView;
    RecyclerView elementsListView;
    CustomizedElementAdapter elementAdapter;
    Handler mainHandler;

    LinearLayout chartLayout;
    LineChart chart;
    Button closeChartButton;
    TextView chartTitleText;

    LineDataSet dataSet;
    LineData data;
    CustomizedElement customElement;
    int chartIndex = 0;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        mainHandler = new Handler(Looper.getMainLooper());

        elementAlertView = root.findViewById(R.id.elementsAlertTextView);
        chartTitleText = root.findViewById(R.id.chartTitleTextView);
        elementsListView = root.findViewById(R.id.elementsRecyclerView);
        elementAdapter = new CustomizedElementAdapter(Core.getInstance().getCustomElements(), this);
        elementsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        elementsListView.setAdapter(elementAdapter);

        Core.getInstance().registerListener(this);

        if (Core.getInstance().getCustomElements().isEmpty()) {
            elementAlertView.setVisibility(View.VISIBLE);
        } else {
            elementAlertView.setVisibility(View.GONE);
        }


        this.chartLayout = root.findViewById(R.id.chartLinearLayout);
        this.chartLayout.setVisibility(View.GONE);
        this.chart = root.findViewById(R.id.elementLinechart);

        chart.setTouchEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);

        chart.setVisibleXRange(5, 10);


        this.closeChartButton = root.findViewById(R.id.chartCloseButton);

        this.closeChartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customElement = null;
                chartLayout.setVisibility(View.GONE);
                elementsListView.setVisibility(View.VISIBLE);
            }
        });


        return root;
    }


    public void ShowChart(CustomizedElement element) {
        elementsListView.setVisibility(View.GONE);
        chartLayout.setVisibility(View.VISIBLE);
        this.customElement = element;
        this.chartTitleText.setText(element.getName());
        //Initialize data
        chartIndex = 0;
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < element.getMonitoredItem().getNotifications().size(); i++) {

            String val = element.getMonitoredItem().getNotifications().get(i).getValue().getValue().toString();
            Float fval = null;

            if (element instanceof Valve) {
                Valve valve = (Valve) element;
                if (val.toLowerCase().equals(valve.getOpenValue().toLowerCase())) {
                    fval = 1.0f;
                } else {
                    fval = 0f;
                }
            } else {
                try {
                    fval = new Float(val);
                } catch (Exception e) {
                    continue;
                }
            }

            entries.add(new Entry(chartIndex, fval));
            chartIndex++;
        }

        dataSet = new LineDataSet(entries, "SAMPLES");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCircleColor(Color.RED);
        dataSet.setValueTextSize(14);
        dataSet.setColor(Color.BLUE);
        dataSet.setFormSize(12);


        data = new LineData(dataSet);
        chart.setData(data);
    }


    long tStart = System.currentTimeMillis();

    @Override
    public void onUpdateReceived() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                elementAdapter.notifyDataSetChanged();

                if (Core.getInstance().getCustomElements().isEmpty()) {
                    elementAlertView.setVisibility(View.VISIBLE);
                }

                if (customElement != null) {
                    long tEnd = System.currentTimeMillis();
                    long tDelta = tEnd - tStart;
                    double elapsedSeconds = tDelta / 1000.0;

                    tStart = System.currentTimeMillis();

                    if (elapsedSeconds == 0) {
                        return;
                    }

                    if (customElement.getMonitoredItem().getNotifications().isEmpty()) {
                        return;
                    }

                    MonitoredItemNotification m = customElement.getMonitoredItem().getNotifications().get(0);
                    String val = m.getValue().getValue().toString();
                    Float fval = null;

                    if (customElement instanceof Valve) {
                        Valve valve = (Valve) customElement;
                        if (val.toLowerCase().equals(valve.getOpenValue().toLowerCase())) {
                            fval = 1.0f;
                        } else {
                            fval = 0f;
                        }
                    } else {
                        try {
                            fval = new Float(val);
                        } catch (Exception e) {
                            return;
                        }
                    }

                    if (dataSet.getValues().size() > ExtendedMonitoredItem.notifiesListSize) {

                        dataSet.removeFirst();
                    }


                    dataSet.addEntry(new Entry(chartIndex, fval));

                    chartIndex++;

                    data.notifyDataChanged();
                    dataSet.notifyDataSetChanged();
                    chart.notifyDataSetChanged();

                    chart.moveViewToX(chartIndex - 10);
                }
            }
        });
    }
}
