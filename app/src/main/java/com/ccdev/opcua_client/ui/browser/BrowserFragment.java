package com.ccdev.opcua_client.ui.browser;

import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ccdev.opcua_client.Core;
import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.ui.adapters.NodeAdapter;
import com.ccdev.opcua_client.wrappers.ExtendedSubscription;

import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.BrowseDescription;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowseRequest;
import org.opcfoundation.ua.core.BrowseResponse;
import org.opcfoundation.ua.core.BrowseResultMask;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ReferenceDescription;

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

        EditText displayNameText = (EditText) dialogView.findViewById(R.id.subscDisplayNameText);
        EditText publishIntervalText = (EditText) dialogView.findViewById(R.id.subscPublishIntervalText);
        EditText keepAliveCountText = (EditText) dialogView.findViewById(R.id.subscKeepAliveCountText);
        EditText lifetimeCountText = (EditText) dialogView.findViewById(R.id.subscLifetimeCountText);
        EditText maxNotificationsPerPublishText = (EditText) dialogView.findViewById(R.id.subscMaxNotPerPublishText);
        EditText priorityText = (EditText) dialogView.findViewById(R.id.subscPriorityText);
        Switch publishEnabledSwitch = (Switch) dialogView.findViewById(R.id.subscPublishEnabledSwitch);

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
    }

    public void ShowCreateMonitoredItemDialog(){

    }



    //==============================================================================================


}