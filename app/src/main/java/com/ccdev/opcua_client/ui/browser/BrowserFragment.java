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
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ccdev.opcua_client.Core;
import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.ui.adapters.NodeAdapter;
import com.ccdev.opcua_client.wrappers.ExtendedSubscription;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedByte;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseRequest;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.CreateMonitoredItemsRequest;
import org.opcfoundation.ua.core.CreateSubscriptionRequest;
import org.opcfoundation.ua.core.CreateSubscriptionResponse;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.MonitoredItemCreateRequest;
import org.opcfoundation.ua.core.MonitoringParameters;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.RequestHeader;
import org.opcfoundation.ua.core.WriteRequest;
import org.opcfoundation.ua.core.WriteValue;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class BrowserFragment extends Fragment {

    ListView nodesList;
    Handler mainHandler;
    ReferenceDescription[] references;

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

        } else if(item.getTitle().equals("Write")) {

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
    ProgressDialog dialog;

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

        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        CreateSubscriptionRequest req = new CreateSubscriptionRequest();

                        if(samplingText.getText().toString().isEmpty()){
                            samplingText.setText("0");
                        }
                        if(queueText.getText().toString().isEmpty()){
                            queueText.setText("1");
                        }

     
                    }
                })
                .setNegativeButton("Abort", null);

        builder.show();

        CreateMonitoredItemsRequest req = new CreateMonitoredItemsRequest();
        MonitoredItemCreateRequest m = new MonitoredItemCreateRequest();
        MonitoringParameters mp = new MonitoringParameters();

    }

    //==============================================================================================


}