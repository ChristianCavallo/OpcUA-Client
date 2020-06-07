package com.ccdev.opcua_client.ui.browser;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ccdev.opcua_client.core.Core;
import com.ccdev.opcua_client.MainActivity;
import com.ccdev.opcua_client.R;
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
import org.opcfoundation.ua.builtintypes.UnsignedLong;
import org.opcfoundation.ua.builtintypes.UnsignedShort;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.builtintypes.UnsignedByte;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseRequest;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.CreateMonitoredItemsRequest;
import org.opcfoundation.ua.core.CreateMonitoredItemsResponse;
import org.opcfoundation.ua.core.CreateSubscriptionRequest;
import org.opcfoundation.ua.core.DataChangeFilter;
import org.opcfoundation.ua.core.DataChangeTrigger;
import org.opcfoundation.ua.core.DeadbandType;
import org.opcfoundation.ua.core.DataTypeAttributes;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MonitoredItemCreateRequest;
import org.opcfoundation.ua.core.MonitoringMode;
import org.opcfoundation.ua.core.MonitoringParameters;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReadRequest;
import org.opcfoundation.ua.core.ReadResponse;
import org.opcfoundation.ua.core.ReadValueId;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.core.WriteRequest;
import org.opcfoundation.ua.core.WriteResponse;
import org.opcfoundation.ua.core.WriteValue;
import org.w3c.dom.Text;

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
        navPathtView = (TextView) root.findViewById(R.id.navPathtTexView);
        navReferences = new ArrayList<>();
        this.mainHandler = new Handler(getContext().getMainLooper());
        backButton = (ImageView) root.findViewById(R.id.backBrowserButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if(navReferences.size()>1){
                        Browse(navReferences.get(navReferences.size()-2));
                    }
            }
        });
        nodesList = (ListView) root.findViewById(R.id.nodesList);
        nodesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(references[position].getNodeClass().getValue() != 1){
                    return;
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
        if(references[index].getNodeClass().getValue() != 2){
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
        if(item.getTitle().equals("Read")) {
            ShowReadDialog();
        } else if(item.getTitle().equals("Write")) {
            ShowWriteDialog();
        } else if(item.getTitle().equals("Subscribe")) {
            ShowSubscriptionChooseDialog();
        }
        return true;
    }

    private void Browse(final ReferenceDescription r) {

        if(Looper.myLooper() == Looper.getMainLooper()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Browse(r);
                }
            }).start();
            return;
        }
        BrowseRequest req = new BrowseRequest();

        ExpandedNodeId en = r.getNodeId();
        NodeId n = NodeId.get(en.getIdType(), en.getNamespaceIndex(), en.getValue());

        BrowseDescription browse = new BrowseDescription();
        browse.setNodeId(n);
        browse.setBrowseDirection(BrowseDirection.Forward);
        browse.setIncludeSubtypes(true);
        browse.setNodeClassMask(NodeClass.Object, NodeClass.Variable);
        browse.setResultMask(BrowseResultMask.All);

        req.setNodesToBrowse(new BrowseDescription[]{browse});

        BrowseResponse res;

        try {
            res = Core.getInstance().getSessionChannel().Browse(req);
        } catch (ServiceResultException ex) {
            ex.printStackTrace();
            final ServiceResultException e = ex;
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Error: " + e.getStatusCode().getDescription() + ". Code: " + e.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                }
            });

            return;
        }

        boolean found = false;
        for (int i=0; i<navReferences.size() ;i++){
            ExpandedNodeId e = navReferences.get(i).getNodeId();
            if(r.getNodeId().getNamespaceIndex() == e.getNamespaceIndex() &&
                r.getNodeId().getValue().toString().equals(e.getValue().toString())){
                found= true;
                break;
            }
        }

        if(found){
            navReferences.remove(navReferences.size()-1);
        }else{
            navReferences.add(r);
        }

        if(res.getResults().length>0){

            references = res.getResults()[0].getReferences();
            ArrayList<ReferenceDescription> rl = new ArrayList<>();
            for(int i = 0; i < references.length; i++){
                found = false;
                for(int j = 0; j < rl.size(); j++){
                    if(references[i].getNodeId().getNamespaceIndex() == rl.get(j).getNodeId().getNamespaceIndex() &&
                            references[i].getNodeId().getValue().toString().equals(rl.get(j).getNodeId().getValue().toString())){
                        found = true;
                        break;
                    }
                }
                if(!found){
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
        }
    }

    private void UpdateList(){

        NodeAdapter na = new NodeAdapter(getContext(), references);
        this.nodesList.setAdapter(na);

    }

    private void UpdatePath(){

        StringBuilder sb = new StringBuilder();
        sb.append("Root/");
        for(int i=1; i< navReferences.size(); i++){
            sb.append(navReferences.get(i).getDisplayName().getText()+ "/");
        }

        navPathtView.setText(sb.toString());

    }


    //SUBSCRIPTION =================================================================================
    ExtendedSubscription selectedSubscription;
    ExtendedMonitoredItem createdMonitoredItem;

    private void ShowSubscriptionChooseDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_subscription_choose, null);

        ListView subscriptionsList = (ListView) dialogView.findViewById(R.id.subscChooseListView);
        TextView titleView = (TextView) dialogView.findViewById(R.id.subscChooseTitleTextView);

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

        if(Core.getInstance().getSubscriptions().size() == 0){
            titleView.setText("No subscriptions. Create a new one.");
            subscriptionsList.setVisibility(View.GONE);
        } else {

            ArrayList<String> subs = new ArrayList<>();
            for(int i = 0; i < Core.getInstance().getSubscriptions().size(); i++){
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

    public void ShowCreateSubscriptionDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_subscription, null);

        final EditText displayNameText = (EditText) dialogView.findViewById(R.id.subscDisplayNameText);
        final EditText publishIntervalText = (EditText) dialogView.findViewById(R.id.subscPublishIntervalText);
        final EditText keepAliveCountText = (EditText) dialogView.findViewById(R.id.subscKeepAliveCountText);
        final EditText lifetimeCountText = (EditText) dialogView.findViewById(R.id.subscLifetimeCountText);
        final EditText maxNotificationsPerPublishText = (EditText) dialogView.findViewById(R.id.subscMaxNotPerPublishText);
        final EditText priorityText = (EditText) dialogView.findViewById(R.id.subscPriorityText);
        final Switch publishEnabledSwitch = (Switch) dialogView.findViewById(R.id.subscPublishEnabledSwitch);


        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        CreateSubscriptionRequest req = new CreateSubscriptionRequest();

                        if(publishIntervalText.getText().toString().isEmpty()){
                            publishIntervalText.setText("1000");
                        }
                        if(keepAliveCountText.getText().toString().isEmpty()){
                            keepAliveCountText.setText("10");
                        }
                        if(lifetimeCountText.getText().toString().isEmpty()){
                            lifetimeCountText.setText("1000");
                        }
                        if(maxNotificationsPerPublishText.getText().toString().isEmpty()){
                            maxNotificationsPerPublishText.setText("0");
                        }
                        if(priorityText.getText().toString().isEmpty()){
                            priorityText.setText("255");
                        }

                        if(displayNameText.getText().toString().trim().isEmpty()){
                            displayNameText.setText("MySubscription" + (Core.getInstance().getSubscriptions().size() + 1));
                        }

                        try{
                            req.setRequestedPublishingInterval(Double.parseDouble(publishIntervalText.getText().toString()));
                            req.setRequestedMaxKeepAliveCount(UnsignedInteger.parseUnsignedInteger(keepAliveCountText.getText().toString()));
                            req.setRequestedLifetimeCount(UnsignedInteger.parseUnsignedInteger(lifetimeCountText.getText().toString()));
                            req.setMaxNotificationsPerPublish(UnsignedInteger.parseUnsignedInteger(maxNotificationsPerPublishText.getText().toString()));
                            req.setPriority(UnsignedByte.parseUnsignedByte(priorityText.getText().toString()));
                            req.setPublishingEnabled(publishEnabledSwitch.isChecked());
                        }catch(Exception e){
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        ExtendedSubscription es = new ExtendedSubscription(displayNameText.getText().toString().trim(), req);


                        CreateSubscription(es);
                    }
                })
                .setNegativeButton("Abort", null);

        builder.show();
    }

    public void CreateSubscription(final ExtendedSubscription es){
        if(Looper.myLooper() == Looper.getMainLooper()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CreateSubscription(es);
                }
            }).start();
            return;
        }

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                dialog = ProgressDialog.show(getContext(), "","Creating subscription...", true);
            }
        });

        ServiceResultException exception = null;
        try {
            Core.getInstance().createSubscription(es);
        } catch (ServiceResultException ex) {
            ex.printStackTrace();
            exception = ex;
            return;
        }

        final ServiceResultException finalException = exception;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {

                dialog.dismiss();

                if(finalException != null){
                    Toast.makeText(getContext(), "Error: " + finalException.getStatusCode().getDescription() + ". Code: " + finalException.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Subscription created.", Toast.LENGTH_LONG).show();
                    selectedSubscription = es;
                    ShowCreateMonitoredItemDialog();
                }
            }
        });
    }

    public void ShowCreateMonitoredItemDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_monitoreditem, null);

        final EditText samplingText = (EditText) dialogView.findViewById(R.id.monSamplingText);
        final EditText queueText = (EditText) dialogView.findViewById(R.id.monQueueText);
        final EditText deadbandText = (EditText) dialogView.findViewById(R.id.monDeadbandText);
        final Spinner triggerSpinner = (Spinner) dialogView.findViewById(R.id.monTriggerSpinner);
        final Spinner typeSpinner = (Spinner) dialogView.findViewById(R.id.monTypeSpinner);
        final RadioButton discardOldestRadio = (RadioButton) dialogView.findViewById(R.id.monDiscardOldestRadioButton);
        final Spinner timestampSpinner = (Spinner) dialogView.findViewById(R.id.monTimestampSpinner);

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
                    public void onClick(DialogInterface dialog, int id) {
                        CreateMonitoredItemsRequest req = new CreateMonitoredItemsRequest();
                        MonitoredItemCreateRequest m = new MonitoredItemCreateRequest();
                        MonitoringParameters mp = new MonitoringParameters();

                        if(samplingText.getText().toString().isEmpty()){
                            samplingText.setText("0");
                        }
                        if(deadbandText.getText().toString().isEmpty()){
                            deadbandText.setText("0");
                        }
                        if(queueText.getText().toString().isEmpty()){
                            queueText.setText("1");
                        }
                        if(Integer.parseInt(queueText.getText().toString()) > 100){
                            queueText.setText("100");
                        }

                        DataChangeFilter filter = new DataChangeFilter();
                        switch(triggerSpinner.getSelectedItemPosition()){
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

                        switch(typeSpinner.getSelectedItemPosition()){
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

                        switch (timestampSpinner.getSelectedItemPosition()){
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


                        CreateMonitoredItem(req);
                    }
                })
                .setNegativeButton("Abort", null);

        builder.show();



    }

    private void CreateMonitoredItem(final CreateMonitoredItemsRequest req){
        if(Looper.myLooper() == Looper.getMainLooper()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    CreateMonitoredItem(req);
                }
            }).start();
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                dialog = ProgressDialog.show(getContext(), "","Creating monitored item...", true);
            }
        });
        CreateMonitoredItemsResponse res = null;
        ServiceResultException exception = null;
        try {
            res = Core.getInstance().getSessionChannel().CreateMonitoredItems(req);
        } catch (ServiceResultException ex) {
            ex.printStackTrace();
            exception = ex;
        }

        final ServiceResultException e = exception;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
                if(e != null){
                    Toast.makeText(getContext(), "Error: " + e.getStatusCode().getDescription() + ". Code: " + e.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Monitored Item created correctly.", Toast.LENGTH_LONG).show();
                    ShowChooseCustomizedElementDialog();
                }
            }
        });

        if(exception == null){
            ExtendedMonitoredItem emi = new ExtendedMonitoredItem(references[selectedNodeIndex].getDisplayName().getText(), req.getItemsToCreate()[0].getRequestedParameters().getClientHandle().intValue(),
                    req, res.getResults()[0]);

            createdMonitoredItem = emi;
            selectedSubscription.getMonitoredItems().add(emi);
        }
    }


    private void ShowChooseCustomizedElementDialog(){
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

    private void ShowCreateCustomizedElementDialog(){
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

        final EditText elementNameText = (EditText) dialogView.findViewById(R.id.elementNameTextView);
        final Spinner elementSpinner = (Spinner) dialogView.findViewById(R.id.elementsSpinner);

        final ImageView elementImage = (ImageView) dialog.findViewById(R.id.elementImageView);
        final TextView elementHeaderView = (TextView) dialogView.findViewById(R.id.elementHeaderTextView);

        final TextView minValueView = (TextView) dialogView.findViewById(R.id.elementMinValueTextView);
        final EditText minText = (EditText) dialogView.findViewById(R.id.elementMinValueEditText);

        final TextView maxValueView = (TextView) dialogView.findViewById(R.id.elementMaxValueTextView);
        final EditText maxText = (EditText) dialogView.findViewById(R.id.elementMaxValueEditText);

        String[] elements = new String[]{"Tank", "Pump", "Valve", "Sensor"};
        ArrayAdapter<String> elementsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, elements);
        elementSpinner.setAdapter(elementsAdapter);

        elementSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                minText.setText("");
                maxText.setText("");
                    switch(position){
                        case 0:
                            elementImage.setImageResource(R.drawable.ic_tank);
                            elementHeaderView.setText("Set a range of values for the tank's level");
                            minText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            minText.setHint("Ex: 0");
                            minValueView.setText("Min value:");
                            maxText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            maxText.setHint("Ex: 100");
                            maxValueView.setText("Max value:");
                            break;
                        case 1:
                            elementImage.setImageResource(R.drawable.ic_pump);
                            elementHeaderView.setText("Set a range of values for the pump speed (RPM)");
                            minText.setInputType(InputType.TYPE_CLASS_NUMBER);
                            minText.setHint("Ex: 0");
                            minValueView.setText("Min value:");
                            maxText.setInputType(InputType.TYPE_CLASS_NUMBER);
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
                            elementImage.setImageResource(R.drawable.ic_sensor);
                            elementHeaderView.setText("Optionally, set a range of values");
                            minText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            minValueView.setText("Min value:");
                            minText.setHint("Ex: 0");
                            maxText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
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

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //DATA VALIDATION
                if(elementNameText.getText().toString().isEmpty()){
                    Toast.makeText(getContext(), "You didn't put any name.", Toast.LENGTH_LONG).show();
                    elementNameText.requestFocus();
                    return;
                }

                CustomizedElement e = null;

                String name = elementNameText.getText().toString().trim();

                try{
                    switch (elementSpinner.getSelectedItemPosition()){
                        case 0: {
                            e = new Tank(createdMonitoredItem, name);
                            Tank t = (Tank) e;
                            t.setMinValue(new Double(minText.getText().toString().trim()));
                            t.setMaxValue(new Double(maxText.getText().toString().trim()));
                        }
                            break;
                        case 1: {
                            e = new Pump(createdMonitoredItem, name);
                            Pump p = (Pump) e;
                            p.setMinRPM(new Integer(minText.getText().toString().trim()));
                            p.setMaxRPM(new Integer(maxText.getText().toString().trim()));
                        }
                            break;
                        case 2:
                        {
                            if(minText.getText().toString().isEmpty() || maxText.getText().toString().isEmpty()){
                                throw new Exception("Invalid range values for this valve.");
                            }
                            e = new Valve(createdMonitoredItem, name);
                            Valve va = (Valve) e;
                            va.setOpenValue(minText.getText().toString());
                            va.setClosedValue(maxText.getText().toString());
                        }
                            break;
                        case 3:
                        {
                            e = new Sensor(createdMonitoredItem, name);
                            Sensor s = (Sensor) e;
                            if(!minText.getText().toString().isEmpty() && !maxText.getText().toString().isEmpty()) {
                                s.setMinValue(new Double(minText.getText().toString().trim()));
                                s.setMaxValue(new Double(maxText.getText().toString().trim()));
                            }
                        }
                            break;
                    }

                } catch(Exception exsa){
                    Toast.makeText(getContext(), "Fix your range values.", Toast.LENGTH_LONG).show();
                    return;
                }

                if(e == null){
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

    private void ShowReadDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_read, null);

        //Text info node
        TextView readNameNodeView = (TextView) dialogView.findViewById(R.id.readNameNodeTextView);
        TextView readNamespaceView = (TextView) dialogView.findViewById(R.id.readNamespaceTextView);
        TextView readNodeIndexView = (TextView) dialogView.findViewById(R.id.readNodeIndexTextView);

        //Text result read
        nodeValueView = (TextView) dialogView.findViewById(R.id.readNodeValuetextView);
        nodeTimestampView = (TextView) dialogView.findViewById(R.id.readNodeTimestamptextView);
        dataTypeView = (TextView) dialogView.findViewById(R.id.readDataTypeTextView);

        //Button
        Button readButton = (Button) dialogView.findViewById(R.id.readButton);
        Button readCloseButton = (Button) dialogView.findViewById(R.id.readCloseButton);

        //Parameters
        final Spinner readTimestamp = (Spinner) dialogView.findViewById(R.id.readTimestampSpinner);
        String[] items = new String[]{"Source", "Server", "Both", "Neither"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, items);
        readTimestamp.setAdapter(adapter);
        final EditText readMaxAgeView = (EditText) dialogView.findViewById(R.id.readMaxAgeTextView);


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
                switch (readTimestamp.getSelectedItemPosition()){
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

                ReadRequest req = new ReadRequest();
                req.setMaxAge(Double.parseDouble(readMaxAgeView.getText().toString().trim()));
                req.setTimestampsToReturn(timestamps);
                ReadValueId rv = new ReadValueId();
                rv.setAttributeId(Attributes.Value);
                ExpandedNodeId en = references[selectedNodeIndex].getNodeId();
                NodeId n = NodeId.get(en.getIdType(), en.getNamespaceIndex(), en.getValue());
                rv.setNodeId(n);
                req.setNodesToRead(new ReadValueId[]{rv});

                dialog = ProgressDialog.show(getContext(), "",
                        "Reading node value...", true);

                ReadNode(req);

            }
        });

        readCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.cancel();
            }
        });
    }

    private void ReadNode(final ReadRequest req){

        if(Looper.myLooper() == Looper.getMainLooper()){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ReadNode(req);
                }
            }).start();
            return;
        }

        try {
            final ReadResponse res = Core.getInstance().getSessionChannel().Read(req);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(dialog != null){
                        dialog.dismiss();
                    }

                    try{
                        Toast.makeText(getContext(), "Read done.", Toast.LENGTH_LONG).show();

                        currentNodeDataType = res.getResults()[0].getValue().getCompositeClass().getSimpleName();
                        dataTypeView.setText("DataType: " + currentNodeDataType);
                        nodeValueView.setText("Value: " + res.getResults()[0].getValue().toString());

                        switch (req.getTimestampsToReturn()){
                            case Source: //Source
                                nodeTimestampView.setText("Timestamp Source: " + res.getResults()[0].getSourceTimestamp().toString());
                                break;
                            case Server: //Server
                                nodeTimestampView.setText("Timestamp Server: " + res.getResults()[0].getServerTimestamp().toString());
                                break;
                            case Both: //Both
                                nodeTimestampView.setText("Timestamp Server: " + res.getResults()[0].getServerTimestamp().toString() + "\n" +
                                        "Timestamp Source: " + res.getResults()[0].getSourceTimestamp().toString() );
                                break;
                            case Neither: //Neither
                                nodeTimestampView.setText("Timestamp Neither");
                                break;
                        }


                    }catch(Exception ex){
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

            return;
        }

    }
    //==============================================================================================



    //Write ========================================================================================
    /*
    * TextView readNodeValueView;
    * TextView readNodeTimestampView;
    * Spinner readTimestamp;
    */

    private void ShowWriteDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_write, null);

        //Text info node
        TextView writeNameNodeView = (TextView) dialogView.findViewById(R.id.writeNameNodeTextView);
        TextView writeNamespaceView = (TextView) dialogView.findViewById(R.id.writeNamespaceTextView);
        TextView writeNodeIndexView = (TextView) dialogView.findViewById(R.id.writeNodeIndexTextView);

        //Text result
        nodeValueView = (TextView) dialogView.findViewById(R.id.writeResultNodeValuetextView);
        writeNodeStausView = (TextView) dialogView.findViewById(R.id.writeResultNodeStatustextView);
        nodeTimestampView = (TextView) dialogView.findViewById(R.id.writeResultNodeTimestamptextView);
        dataTypeView = (TextView) dialogView.findViewById(R.id.writeDataTypeTextView);




        //Button
        Button writeButton = (Button) dialogView.findViewById(R.id.writeButton);
        Button writeCloseButton = (Button) dialogView.findViewById(R.id.writeCloseButton);

        //Parameters

        //Value che viene inserito dall'utente
        final EditText writeValueView = (EditText) dialogView.findViewById(R.id.writeValueTextView);

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
                try{
                    switch(currentNodeDataType.toLowerCase()){
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
                } catch(Exception e){
                    Toast.makeText(getContext(), "Error on the new value: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                if(var == null){
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

        ReadRequest reqread = new ReadRequest();
        reqread.setMaxAge(new Double("0"));
        reqread.setTimestampsToReturn(TimestampsToReturn.Both);

        ReadValueId rv = new ReadValueId();
        rv.setAttributeId(Attributes.Value);
        ExpandedNodeId enr = references[selectedNodeIndex].getNodeId();
        NodeId nr = NodeId.get(enr.getIdType(), enr.getNamespaceIndex(), enr.getValue());
        rv.setNodeId(nr);
        reqread.setNodesToRead(new ReadValueId[]{rv});

        dialog = ProgressDialog.show(getContext(), "",
                "Loading node...", true);
        ReadNode(reqread);
    }

    private void WriteNode(final WriteRequest req){

        if(Looper.myLooper() == Looper.getMainLooper()){
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

                    ReadRequest reqread = new ReadRequest();
                    reqread.setMaxAge(new Double("0"));
                    reqread.setTimestampsToReturn(TimestampsToReturn.Both);

                    ReadValueId rv = new ReadValueId();
                    rv.setAttributeId(Attributes.Value);
                    ExpandedNodeId enr = references[selectedNodeIndex].getNodeId();
                    NodeId nr = NodeId.get(enr.getIdType(), enr.getNamespaceIndex(), enr.getValue());
                    rv.setNodeId(nr);
                    reqread.setNodesToRead(new ReadValueId[]{rv});

                    dialog = ProgressDialog.show(getContext(), "",
                            "Reloading node...", true);
                    ReadNode(reqread);

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