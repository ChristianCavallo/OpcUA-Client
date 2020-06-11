package com.ccdev.opcua_client.ui.browser;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ccdev.opcua_client.MainActivity;
import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.core.Core;
import com.ccdev.opcua_client.elements.CustomizedElement;
import com.ccdev.opcua_client.elements.Pump;
import com.ccdev.opcua_client.elements.Sensor;
import com.ccdev.opcua_client.elements.Tank;
import com.ccdev.opcua_client.elements.Valve;
import com.ccdev.opcua_client.ui.adapters.NodeAdapter;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;
import com.ccdev.opcua_client.wrappers.ExtendedSubscription;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.ExtensionObject;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedByte;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.UnsignedShort;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.CreateMonitoredItemsRequest;
import org.opcfoundation.ua.core.CreateMonitoredItemsResponse;
import org.opcfoundation.ua.core.CreateSubscriptionRequest;
import org.opcfoundation.ua.core.DataChangeFilter;
import org.opcfoundation.ua.core.DataChangeTrigger;
import org.opcfoundation.ua.core.DeadbandType;
import org.opcfoundation.ua.core.EUInformation;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MonitoredItemCreateRequest;
import org.opcfoundation.ua.core.MonitoringMode;
import org.opcfoundation.ua.core.MonitoringParameters;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.Range;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.core.WriteRequest;
import org.opcfoundation.ua.core.WriteResponse;
import org.opcfoundation.ua.core.WriteValue;

import java.util.ArrayList;

public class BrowserFragment extends Fragment {

    ListView nodesList;
    Handler mainHandler;
    ReferenceDescription[] references;
    ProgressDialog dialog;

    ArrayList<ReferenceDescription> navReferences;

    ImageView backButton;
    TextView navPathtView;

    int selectedNodeIndex = -1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_browser, container, false);
        navPathtView = root.findViewById(R.id.navPathtTexView);
        navReferences = new ArrayList<>();
        this.mainHandler = new Handler(getContext().getMainLooper());
        backButton = root.findViewById(R.id.backBrowserButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (navReferences.size() > 1) {
                    Browse(navReferences.get(navReferences.size() - 2));
                }
            }
        });
        nodesList = root.findViewById(R.id.nodesList);
        nodesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (references[position].getNodeClass().getValue() != 1) {
                    //return;
                }
                Browse(references[position]);
            }
        });

        registerForContextMenu(nodesList);


        ReferenceDescription r = new ReferenceDescription();
        ExpandedNodeId eni = new ExpandedNodeId(Identifiers.RootFolder);
        r.setNodeId(eni);
        Browse(r);
        return root;
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        int index = info.position;
        if (references[index].getNodeClass().getValue() != 2) {
            return;
        }
        menu.add(0, 1, 0, "Read");
        menu.add(0, 2, 1, "Write");
        menu.add(0, 3, 2, "Subscribe");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        super.onContextItemSelected(item);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        this.selectedNodeIndex = info.position;
        if (item.getTitle().equals("Read")) {
            ShowReadDialog();
        } else if (item.getTitle().equals("Write")) {
            ShowWriteDialog();
        } else if (item.getTitle().equals("Subscribe")) {
            ShowSubscriptionChooseDialog();
        }
        return true;
    }

    private void Browse(final ReferenceDescription r) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Browse(r);
                }
            }).start();
            return;
        }

        try {

            references = Core.getInstance().Browse(r);

            //NAV PATH UPDATE
            boolean found = false;
            for (int i = 0; i < navReferences.size(); i++) {
                ExpandedNodeId e = navReferences.get(i).getNodeId();
                if (r.getNodeId().getNamespaceIndex() == e.getNamespaceIndex() &&
                        r.getNodeId().getValue().toString().equals(e.getValue().toString())) {
                    found = true;
                    break;
                }
            }

            if (found) {
                navReferences.remove(navReferences.size() - 1);
            } else {
                navReferences.add(r);
            }

            ArrayList<ReferenceDescription> rl = new ArrayList<>();
            for (int i = 0; i < references.length; i++) {
                found = false;
                for (int j = 0; j < rl.size(); j++) {
                    if (references[i].getNodeId().getNamespaceIndex() == rl.get(j).getNodeId().getNamespaceIndex() &&
                            references[i].getNodeId().getValue().toString().equals(rl.get(j).getNodeId().getValue().toString())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    rl.add(references[i]);
                }
            }

            references = new ReferenceDescription[rl.size()];
            references = rl.toArray(references);

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    UpdateList();
                    UpdatePath();
                }
            });

        } catch (final ServiceResultException e) {

            e.printStackTrace();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Error: " + e.getStatusCode().getDescription() + ". Code: " + e.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                }
            });

        }

    }

    private void UpdateList() {
        NodeAdapter na = new NodeAdapter(getContext(), references);
        this.nodesList.setAdapter(na);
    }

    private void UpdatePath() {

        StringBuilder sb = new StringBuilder();
        sb.append("Root/");
        for (int i = 1; i < navReferences.size(); i++) {
            sb.append(navReferences.get(i).getDisplayName().getText() + "/");
        }

        navPathtView.setText(sb.toString());

    }

    private Range searchEuRange(ReferenceDescription r) {
        try {
            ReferenceDescription[] browseResult = Core.getInstance().Browse(r);

            for (ReferenceDescription rd : browseResult) {
                if (rd.getDisplayName().getText().equals("EURange") && rd.getNodeClass() == NodeClass.Variable) {

                    DataValue v = Core.getInstance().Read(rd, new Double(0), TimestampsToReturn.Neither);

                    Range range = v.getValue().asClass(Range.class, null);
                    return range;
                }
            }
        } catch (ServiceResultException e) {
            e.printStackTrace();
        }
        return null;
    }

    private EUInformation searchMisurementUnit(ReferenceDescription r) {
        try {
            ReferenceDescription[] browseResult = Core.getInstance().Browse(r);

            for (ReferenceDescription rd : browseResult) {
                if (rd.getDisplayName().getText().equals("EngineeringUnits") && rd.getNodeClass() == NodeClass.Variable) {

                    DataValue v = Core.getInstance().Read(rd, new Double(0), TimestampsToReturn.Neither);

                    EUInformation info = v.getValue().asClass(EUInformation.class, null);
                    return info;
                }
            }
        } catch (ServiceResultException e) {
            e.printStackTrace();
        }
        return null;
    }


    //SUBSCRIPTION =================================================================================
    ExtendedSubscription selectedSubscription;
    ExtendedMonitoredItem createdMonitoredItem;

    private void ShowSubscriptionChooseDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_subscription_choose, null);

        ListView subscriptionsList = dialogView.findViewById(R.id.subscChooseListView);
        TextView titleView = dialogView.findViewById(R.id.subscChooseTitleTextView);

        builder.setView(dialogView)
                // Add action buttons
                .setNeutralButton("Create New", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        ShowCreateSubscriptionDialog();
                    }
                })
                .setNegativeButton("Abort", null);

        final AlertDialog ad = builder.show();

        if (Core.getInstance().getSubscriptions().size() == 0) {
            titleView.setText("No subscriptions. Create a new one.");
            subscriptionsList.setVisibility(View.GONE);
        } else {

            ArrayList<String> subs = new ArrayList<>();
            for (int i = 0; i < Core.getInstance().getSubscriptions().size(); i++) {
                subs.add(Core.getInstance().getSubscriptions().get(i).getName());
            }
            subscriptionsList.setAdapter(new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, subs));

            subscriptionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedSubscription = Core.getInstance().getSubscriptions().get(position);
                    ad.cancel();
                    ShowCreateMonitoredItemDialog();
                }
            });
        }
    }

    public void ShowCreateSubscriptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_subscription, null);

        final EditText displayNameText = dialogView.findViewById(R.id.subscDisplayNameText);
        final EditText publishIntervalText = dialogView.findViewById(R.id.subscPublishIntervalText);
        final EditText keepAliveCountText = dialogView.findViewById(R.id.subscKeepAliveCountText);
        final EditText lifetimeCountText = dialogView.findViewById(R.id.subscLifetimeCountText);
        final EditText maxNotificationsPerPublishText = dialogView.findViewById(R.id.subscMaxNotPerPublishText);
        final EditText priorityText = dialogView.findViewById(R.id.subscPriorityText);
        final Switch publishEnabledSwitch = dialogView.findViewById(R.id.subscPublishEnabledSwitch);


        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int id) {
                        CreateSubscriptionRequest req = new CreateSubscriptionRequest();

                        if (publishIntervalText.getText().toString().isEmpty()) {
                            publishIntervalText.setText("1000");
                        }
                        if (keepAliveCountText.getText().toString().isEmpty()) {
                            keepAliveCountText.setText("10");
                        }
                        if (lifetimeCountText.getText().toString().isEmpty()) {
                            lifetimeCountText.setText("1000");
                        }
                        if (maxNotificationsPerPublishText.getText().toString().isEmpty()) {
                            maxNotificationsPerPublishText.setText("0");
                        }
                        if (priorityText.getText().toString().isEmpty()) {
                            priorityText.setText("255");
                        }

                        if (displayNameText.getText().toString().trim().isEmpty()) {
                            displayNameText.setText("MySubscription" + (Core.getInstance().getSubscriptions().size() + 1));
                        }

                        try {
                            req.setRequestedPublishingInterval(Double.parseDouble(publishIntervalText.getText().toString()));
                            req.setRequestedMaxKeepAliveCount(UnsignedInteger.parseUnsignedInteger(keepAliveCountText.getText().toString()));
                            req.setRequestedLifetimeCount(UnsignedInteger.parseUnsignedInteger(lifetimeCountText.getText().toString()));
                            req.setMaxNotificationsPerPublish(UnsignedInteger.parseUnsignedInteger(maxNotificationsPerPublishText.getText().toString()));
                            req.setPriority(UnsignedByte.parseUnsignedByte(priorityText.getText().toString()));
                            req.setPublishingEnabled(publishEnabledSwitch.isChecked());
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        ExtendedSubscription es = new ExtendedSubscription(displayNameText.getText().toString().trim(), req);

                        dialog = ProgressDialog.show(getContext(), "", "Creating subscription...", true);

                        CreateSubscription(es);
                    }
                })
                .setNegativeButton("Abort", null);

        builder.show();
    }

    public void CreateSubscription(final ExtendedSubscription es) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CreateSubscription(es);
                }
            }).start();
            return;
        }

        try {
            Core.getInstance().createSubscription(es);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Subscription created.", Toast.LENGTH_LONG).show();
                    selectedSubscription = es;
                    ShowCreateMonitoredItemDialog();
                }
            });
        } catch (final ServiceResultException ex) {
            ex.printStackTrace();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + ex.getStatusCode().getDescription() + ". Code: " + ex.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void ShowCreateMonitoredItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_monitoreditem, null);

        final EditText samplingText = dialogView.findViewById(R.id.monSamplingText);
        final EditText queueText = dialogView.findViewById(R.id.monQueueText);
        final EditText deadbandText = dialogView.findViewById(R.id.monDeadbandText);
        final Spinner triggerSpinner = dialogView.findViewById(R.id.monTriggerSpinner);
        final Spinner typeSpinner = dialogView.findViewById(R.id.monTypeSpinner);
        final RadioButton discardOldestRadio = dialogView.findViewById(R.id.monDiscardOldestRadioButton);
        final Spinner timestampSpinner = dialogView.findViewById(R.id.monTimestampSpinner);

        String[] timestamps = new String[]{"Source", "Server", "Both", "Neither"};
        ArrayAdapter<String> timestampsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, timestamps);
        timestampSpinner.setAdapter(timestampsAdapter);

        String[] triggers = new String[]{"Status", "Status + Value", "Status + Value + Timestamp"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, triggers);
        triggerSpinner.setAdapter(adapter);

        String[] types = new String[]{"None", "Percent", "Absolute"};
        ArrayAdapter<String> adapterTypes = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, types);
        typeSpinner.setAdapter(adapterTypes);

        triggerSpinner.setSelection(1);
        typeSpinner.setSelection(0);

        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int id) {
                        CreateMonitoredItemsRequest req = new CreateMonitoredItemsRequest();
                        MonitoredItemCreateRequest m = new MonitoredItemCreateRequest();
                        MonitoringParameters mp = new MonitoringParameters();

                        if (samplingText.getText().toString().isEmpty()) {
                            samplingText.setText("0");
                        }
                        if (deadbandText.getText().toString().isEmpty()) {
                            deadbandText.setText("0");
                        }
                        if (queueText.getText().toString().isEmpty()) {
                            queueText.setText("1");
                        }
                        if (Integer.parseInt(queueText.getText().toString()) > 100) {
                            queueText.setText("100");
                        }

                        DataChangeFilter filter = new DataChangeFilter();
                        switch (triggerSpinner.getSelectedItemPosition()) {
                            case 0:
                                filter.setTrigger(DataChangeTrigger.Status);
                                break;
                            case 1:
                                filter.setTrigger(DataChangeTrigger.StatusValue);
                                break;
                            case 2:
                                filter.setTrigger(DataChangeTrigger.StatusValueTimestamp);
                                break;
                        }

                        switch (typeSpinner.getSelectedItemPosition()) {
                            case 0:
                                filter.setDeadbandType(new UnsignedInteger(DeadbandType.None.getValue()));
                                break;
                            case 1:
                                filter.setDeadbandType(new UnsignedInteger(DeadbandType.Percent.getValue()));
                                break;
                            case 2:
                                filter.setDeadbandType(new UnsignedInteger(DeadbandType.Absolute.getValue()));
                                break;
                        }

                        filter.setDeadbandValue(Double.parseDouble(deadbandText.getText().toString().trim()));

                        ExtensionObject fil = new ExtensionObject(filter);
                        mp.setFilter(fil);

                        mp.setDiscardOldest(discardOldestRadio.isChecked());
                        mp.setQueueSize(UnsignedInteger.parseUnsignedInteger(queueText.getText().toString().trim()));
                        mp.setSamplingInterval(Double.parseDouble(samplingText.getText().toString().trim()));
                        int mId = (int) (Math.random() * 1000000);
                        mp.setClientHandle(new UnsignedInteger(mId));

                        switch (timestampSpinner.getSelectedItemPosition()) {
                            case 0:
                                req.setTimestampsToReturn(TimestampsToReturn.Source);
                                break;
                            case 1:
                                req.setTimestampsToReturn(TimestampsToReturn.Server);
                                break;
                            case 2:
                                req.setTimestampsToReturn(TimestampsToReturn.Both);
                                break;
                            case 3:
                                req.setTimestampsToReturn(TimestampsToReturn.Neither);
                                break;
                        }

                        ExpandedNodeId en = references[selectedNodeIndex].getNodeId();
                        NodeId n = NodeId.get(en.getIdType(), en.getNamespaceIndex(), en.getValue());
                        ReadValueId rvi = new ReadValueId();
                        rvi.setNodeId(n);
                        rvi.setAttributeId(Attributes.Value);

                        m.setItemToMonitor(rvi);
                        m.setMonitoringMode(MonitoringMode.Reporting);
                        m.setRequestedParameters(mp);

                        req.setItemsToCreate(new MonitoredItemCreateRequest[]{m});
                        req.setSubscriptionId(selectedSubscription.getResponse().getSubscriptionId());

                        dialog = ProgressDialog.show(getContext(), "", "Creating monitored item...", true);
                        CreateMonitoredItem(req);
                    }
                })
                .setNegativeButton("Abort", null);

        builder.show();
    }

    private void CreateMonitoredItem(final CreateMonitoredItemsRequest req) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CreateMonitoredItem(req);
                }
            }).start();
            return;
        }

        try {
            CreateMonitoredItemsResponse res = Core.getInstance().getSessionChannel().CreateMonitoredItems(req);

            ExtendedMonitoredItem emi = new ExtendedMonitoredItem(references[selectedNodeIndex].getDisplayName().getText(), req.getItemsToCreate()[0].getRequestedParameters().getClientHandle().intValue(),
                    req, res.getResults()[0]);

            createdMonitoredItem = emi;
            selectedSubscription.getMonitoredItems().add(emi);

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Monitored Item created correctly.", Toast.LENGTH_LONG).show();
                    ShowChooseCustomizedElementDialog();
                }
            });

        } catch (final ServiceResultException ex) {
            ex.printStackTrace();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + ex.getStatusCode().getDescription() + ". Code: " + ex.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void ShowChooseCustomizedElementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Do you want to link this monitored item to a customized element?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ShowCreateCustomizedElementDialog();
            }
        });

        builder.setNegativeButton("No", null);
        builder.show();
    }

    private void ShowCreateCustomizedElementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_custom_element, null);

        builder.setView(dialogView);

        builder.setNegativeButton("Abort", null);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();

        final EditText elementNameText = dialogView.findViewById(R.id.elementNameTextView);
        final Spinner elementSpinner = dialogView.findViewById(R.id.elementsSpinner);

        final ImageView elementImage = dialog.findViewById(R.id.elementImageView);
        final TextView elementHeaderView = dialogView.findViewById(R.id.elementHeaderTextView);

        final TextView minValueView = dialogView.findViewById(R.id.elementMinValueTextView);
        final EditText minText = dialogView.findViewById(R.id.elementMinValueEditText);

        final TextView maxValueView = dialogView.findViewById(R.id.elementMaxValueTextView);
        final EditText maxText = dialogView.findViewById(R.id.elementMaxValueEditText);

        final TextView nodeInfoText = dialog.findViewById((R.id.elementNodeInfoTextView));
        nodeInfoText.setText("");

        final EditText unitText = dialogView.findViewById(R.id.elementUnitEditText);

        final CheckBox indicatorCheckBox = dialogView.findViewById(R.id.elementUseIndicatorCheckBox);
        final CheckBox progressbarCheckBox = dialogView.findViewById(R.id.elementUseProgressBarCheckBox);

        final LinearLayout sensorsLayout = dialogView.findViewById(R.id.sensorTypeLayout);
        sensorsLayout.setVisibility(View.GONE);

        final Spinner sensorsSpinner = dialogView.findViewById(R.id.sensorsSpinner);


        new Thread(new Runnable() {
            @Override
            public void run() {
                Range r = searchEuRange(references[selectedNodeIndex]);
                if (r != null) {
                    nodeInfoText.setText("This node has an EURange: [" + r.getLow() + ", " + r.getHigh() + "]");
                    minText.setText(r.getLow() + "");
                    maxText.setText(r.getHigh() + "");
                } else {
                    nodeInfoText.setText("No EURange found for this node.");
                    minText.setText("");
                    maxText.setText("");
                }

                EUInformation i = searchMisurementUnit(references[selectedNodeIndex]);
                if (i != null) {
                    nodeInfoText.setText("The misurement unit is " + i.getDisplayName().getText());
                    unitText.setText(i.getDisplayName().getText());
                } else {
                    nodeInfoText.setText("No EngineeringUnits found for this node.");
                    unitText.setText("");
                }

            }
        }).start();

        String[] elements = new String[]{"Tank", "Pump", "Valve", "Sensor", "Temperature Sensor", "Pressure Sensor", "Humidity Sensor"};
        ArrayAdapter<String> elementsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, elements);
        elementSpinner.setAdapter(elementsAdapter);

        elementSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sensorsLayout.setVisibility(View.GONE);
                switch (position) {
                    case 0:
                        elementImage.setImageResource(R.drawable.ic_tank);
                        elementHeaderView.setText("Set a range of values for the tank's level");
                        minText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        minText.setHint("Ex: 0");
                        minValueView.setText("Min value:");
                        maxText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        maxText.setHint("Ex: 100");
                        maxValueView.setText("Max value:");
                        break;
                    case 1:
                        elementImage.setImageResource(R.drawable.ic_pump);
                        elementHeaderView.setText("Set a range of values for the pump speed (RPM)");
                        minText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        minText.setHint("Ex: 0");
                        minValueView.setText("Min value:");
                        maxText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        maxValueView.setText("Max value:");
                        maxText.setHint("Ex: 10000");
                        break;
                    case 2:
                        elementImage.setImageResource(R.drawable.ic_valve_open);
                        elementHeaderView.setText("Set two values to map the valve's state");
                        minText.setInputType(InputType.TYPE_CLASS_TEXT);
                        minValueView.setText("Open value:");
                        minText.setHint("Ex: 1 or True");
                        maxText.setInputType(InputType.TYPE_CLASS_TEXT);
                        maxValueView.setText("Closed value:");
                        maxText.setHint("Ex: 0 or False");
                        break;
                    case 3:
                        sensorsLayout.setVisibility(View.VISIBLE);
                        elementImage.setImageResource(R.drawable.ic_sensor);
                        elementHeaderView.setText("Set a range of values");
                        minText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        minValueView.setText("Min value:");
                        minText.setHint("Ex: 0");
                        maxText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                        maxValueView.setText("Max value:");
                        maxText.setHint("Ex: 60,32");
                        break;
                    default:
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        String[] sensors = new String[]{"Generic", "Temperature", "Pressure", "Humidity"};
        ArrayAdapter<String> sensorsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, sensors);
        sensorsSpinner.setAdapter(sensorsAdapter);

        sensorsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        elementImage.setImageResource(R.drawable.ic_sensor);
                        break;

                    case 1:

                    case 2:
                    case 3:
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (elementNameText.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "You didn't put any name.", Toast.LENGTH_LONG).show();
                    elementNameText.requestFocus();
                    return;
                }

                if (unitText.getText().toString().isEmpty() && elementSpinner.getSelectedItemPosition() != 2) {
                    Toast.makeText(getContext(), "You didn't put any name.", Toast.LENGTH_LONG).show();
                    elementNameText.requestFocus();
                    return;
                }

                CustomizedElement e = null;

                String name = elementNameText.getText().toString().trim();

                CustomizedElement.VisualizationType vt;
                if (indicatorCheckBox.isChecked()) {
                    vt = CustomizedElement.VisualizationType.INDICATOR;
                } else {
                    vt = CustomizedElement.VisualizationType.PROGRESS_BAR;
                }
                if (progressbarCheckBox.isChecked() && indicatorCheckBox.isChecked()) {
                    vt = CustomizedElement.VisualizationType.BOTH;
                }

                try {
                    switch (elementSpinner.getSelectedItemPosition()) {
                        case 0: {
                            e = new Tank(createdMonitoredItem, name);
                            Tank t = (Tank) e;
                            t.setMinValue(new Double(minText.getText().toString().trim()));
                            t.setMaxValue(new Double(maxText.getText().toString().trim()));
                            t.setUnit(unitText.getText().toString().trim());
                            t.setVisualization(vt);
                        }
                        break;
                        case 1: {
                            e = new Pump(createdMonitoredItem, name);
                            Pump p = (Pump) e;
                            p.setMinValue(new Integer(minText.getText().toString().trim()));
                            p.setMaxValue(new Integer(maxText.getText().toString().trim()));
                            p.setUnit(unitText.getText().toString().trim());
                            p.setVisualization(vt);
                        }
                        break;
                        case 2: {
                            if (minText.getText().toString().isEmpty() || maxText.getText().toString().isEmpty()) {
                                throw new Exception("Invalid range values for this valve.");
                            }
                            e = new Valve(createdMonitoredItem, name);
                            Valve va = (Valve) e;
                            va.setOpenValue(minText.getText().toString());
                            va.setClosedValue(maxText.getText().toString());
                            va.setVisualization(vt);
                            va.setUnit(unitText.getText().toString().trim());
                        }
                        break;
                        case 3: {
                            Sensor.Category cat;
                            switch (sensorsSpinner.getSelectedItemPosition()) {
                                case 0:
                                    cat = Sensor.Category.GENERIC;
                                    break;
                                case 1:
                                    cat = Sensor.Category.TEMPERATURE;
                                    break;
                                case 2:
                                    cat = Sensor.Category.PRESSURE;
                                    break;
                                case 3:
                                    cat = Sensor.Category.HUMIDITY;
                                    break;
                                default:
                                    cat = Sensor.Category.GENERIC;
                                    break;
                            }
                            e = new Sensor(createdMonitoredItem, name, cat);
                            Sensor s = (Sensor) e;
                            s.setMinValue(new Double(minText.getText().toString().trim()));
                            s.setMaxValue(new Double(maxText.getText().toString().trim()));
                            e.setVisualization(vt);
                            e.setUnit(unitText.getText().toString().trim());
                        }
                        break;
                    }

                } catch (Exception exsa) {
                    Toast.makeText(getContext(), "Fix your range values.", Toast.LENGTH_LONG).show();
                    return;
                }

                if (e == null) {
                    return;
                }

                Core.getInstance().addCustomizedElement(e);
                Toast.makeText(getContext(), e.getClass().getSimpleName() + " created.", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });


    }


    //==============================================================================================

    TextView writeNodeStausView;

    //Read =========================================================================================

    TextView nodeValueView;
    TextView nodeTimestampView;

    String currentNodeDataType;
    TextView dataTypeView;

    private void ShowReadDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_read, null);

        //Text info node
        TextView readNameNodeView = dialogView.findViewById(R.id.readNameNodeTextView);
        TextView readNamespaceView = dialogView.findViewById(R.id.readNamespaceTextView);
        TextView readNodeIndexView = dialogView.findViewById(R.id.readNodeIndexTextView);

        //Text result read
        nodeValueView = dialogView.findViewById(R.id.readNodeValuetextView);
        nodeTimestampView = dialogView.findViewById(R.id.readNodeTimestamptextView);
        dataTypeView = dialogView.findViewById(R.id.readDataTypeTextView);

        //Button
        Button readButton = dialogView.findViewById(R.id.readButton);
        Button readCloseButton = dialogView.findViewById(R.id.readCloseButton);

        //Parameters
        final Spinner readTimestamp = dialogView.findViewById(R.id.readTimestampSpinner);
        String[] items = new String[]{"Source", "Server", "Both", "Neither"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, items);
        readTimestamp.setAdapter(adapter);
        final EditText readMaxAgeView = dialogView.findViewById(R.id.readMaxAgeTextView);


        ReferenceDescription r = references[selectedNodeIndex];
        readNameNodeView.setText(r.getDisplayName().getText());
        readNamespaceView.setText("Namespace: " + r.getNodeId().getNamespaceIndex() + "");
        readNodeIndexView.setText("NamespaceIndex: " + r.getNodeId().getValue().toString());
        nodeValueView.setText("Value: ");
        readMaxAgeView.setText("0");
        readTimestamp.setSelection(1);

        builder.setView(dialogView);
        final AlertDialog ad = builder.show();

        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimestampsToReturn timestamps = TimestampsToReturn.Neither;
                switch (readTimestamp.getSelectedItemPosition()) {
                    case 0:
                        timestamps = TimestampsToReturn.Source;
                        break;
                    case 1:
                        timestamps = TimestampsToReturn.Server;
                        break;
                    case 2:
                        timestamps = TimestampsToReturn.Both;
                        break;
                    case 3:
                        timestamps = TimestampsToReturn.Neither;
                        break;
                }

                dialog = ProgressDialog.show(getContext(), "",
                        "Reading node value...", true);

                ReadNode(Double.parseDouble(readMaxAgeView.getText().toString().trim()), timestamps);

            }
        });

        readCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.cancel();
            }
        });
    }

    private void ReadNode(final Double maxAge, final TimestampsToReturn timestamps) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ReadNode(maxAge, timestamps);
                }
            }).start();
            return;
        }

        final Range r = searchEuRange(references[selectedNodeIndex]);

        try {
            final DataValue v = Core.getInstance().Read(references[selectedNodeIndex], maxAge, timestamps);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null) {
                        dialog.dismiss();
                    }

                    try {
                        Toast.makeText(getContext(), "Read done.", Toast.LENGTH_LONG).show();

                        currentNodeDataType = v.getValue().getCompositeClass().getSimpleName();
                        dataTypeView.setText("DataType: " + currentNodeDataType);

                        String value = v.getValue().toString();
                        if (r != null) {
                            value += "\nEURange -> [" + r.getLow() + ", " + r.getHigh() + "]";
                        }

                        nodeValueView.setText("Value: " + value);

                        StringBuilder b = new StringBuilder();
                        if (v.getSourceTimestamp() != null) {
                            b.append("Timestamp Source: " + v.getSourceTimestamp().toString() + "\n");
                        }

                        if (v.getServerTimestamp() != null) {
                            b.append("Timestamp Server: " + v.getServerTimestamp().toString() + "\n");
                        }

                        nodeTimestampView.setText(b.toString().trim());

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Toast.makeText(getContext(), "Data read but it can't be printed.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (final ServiceResultException e) {
            e.printStackTrace();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + e.getStatusCode().getDescription() + ". Code: " + e.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                }
            });
        }

    }
    //==============================================================================================


    //Write ========================================================================================

    private void ShowWriteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_write, null);

        //Text info node
        TextView writeNameNodeView = dialogView.findViewById(R.id.writeNameNodeTextView);
        TextView writeNamespaceView = dialogView.findViewById(R.id.writeNamespaceTextView);
        TextView writeNodeIndexView = dialogView.findViewById(R.id.writeNodeIndexTextView);

        //Text result
        nodeValueView = dialogView.findViewById(R.id.writeResultNodeValuetextView);
        writeNodeStausView = dialogView.findViewById(R.id.writeResultNodeStatustextView);
        nodeTimestampView = dialogView.findViewById(R.id.writeResultNodeTimestamptextView);
        dataTypeView = dialogView.findViewById(R.id.writeDataTypeTextView);


        //Button
        Button writeButton = dialogView.findViewById(R.id.writeButton);
        Button writeCloseButton = dialogView.findViewById(R.id.writeCloseButton);

        //Parameters

        //Value che viene inserito dall'utente
        final EditText writeValueView = dialogView.findViewById(R.id.writeValueTextView);

        ReferenceDescription r = references[selectedNodeIndex];
        writeNameNodeView.setText(r.getDisplayName().getText());
        writeNamespaceView.setText("Namespace: " + r.getNodeId().getNamespaceIndex() + "");
        writeNodeIndexView.setText("NamespaceIndex: " + r.getNodeId().getValue().toString());
        nodeValueView.setText("Value: ");
        dataTypeView.setText("DataType: ");
        nodeTimestampView.setText("Timestamp: ");
        writeNodeStausView.setText("Status: ");


        builder.setView(dialogView);
        final AlertDialog ad = builder.show();

        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                MainActivity.hideSoftKeyboard(getActivity());

                WriteRequest reqwrite = new WriteRequest();
                WriteValue wv = new WriteValue();

                DataValue data = new DataValue();
                Variant var = null;
                String s = writeValueView.getText().toString().trim();
                try {
                    switch (currentNodeDataType.toLowerCase()) {
                        case "string":
                            var = new Variant(s);
                            break;

                        case "integer":
                            var = new Variant(new Integer(s));
                            break;
                        case "unsignedshort":
                            var = new Variant(new UnsignedShort(s));
                            break;
                        case "boolean":
                            var = new Variant(new Boolean(s));
                            break;

                        case "short":
                            var = new Variant(new Short(s));
                            break;

                        case "double":
                            var = new Variant(new Double(s));
                            break;

                        case "float":
                            var = new Variant(new Float(s));
                            break;

                        default:
                            break;
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                if (var == null) {
                    Toast.makeText(getContext(), "Unsupported type.", Toast.LENGTH_LONG).show();
                    return;
                }

                data.setValue(var);
                data.setStatusCode(StatusCode.GOOD);
                wv.setValue(data);

                ExpandedNodeId en = references[selectedNodeIndex].getNodeId();
                NodeId n = NodeId.get(en.getIdType(), en.getNamespaceIndex(), en.getValue());
                wv.setNodeId(n);

                wv.setAttributeId(Attributes.Value);

                reqwrite.setNodesToWrite(new WriteValue[]{wv});

                dialog = ProgressDialog.show(getContext(), "",
                        "Writing node value...", true);

                WriteNode(reqwrite);

            }
        });

        writeCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.cancel();
            }
        });

        dialog = ProgressDialog.show(getContext(), "",
                "Loading node...", true);
        ReadNode(new Double(0), TimestampsToReturn.Both);
    }

    private void WriteNode(final WriteRequest req) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    WriteNode(req);
                }
            }).start();
            return;
        }

        try {
            final WriteResponse res = Core.getInstance().getSessionChannel().Write(req);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();

                    writeNodeStausView.setText("Status: " + res.getResults()[0].getName());

                    dialog = ProgressDialog.show(getContext(), "",
                            "Reloading node...", true);
                    ReadNode(new Double(0), TimestampsToReturn.Both);

                }
            });

        } catch (final ServiceResultException e) {
            e.printStackTrace();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + e.getStatusCode().getDescription() + ". Code: " + e.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                }
            });

        }
    }

}