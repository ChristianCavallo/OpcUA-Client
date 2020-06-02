package com.ccdev.opcua_client.ui.browser;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ccdev.opcua_client.Core;
import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.ui.adapters.NodeAdapter;

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

public class BrowserFragment extends Fragment {

    ListView nodesList;
    Handler mainHandler;
    ReferenceDescription[] references;

    ArrayList<ReferenceDescription> navReferences;

    ImageView backButton;
    TextView navPathtView;
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
        ReferenceDescription r = new ReferenceDescription();
        ExpandedNodeId eni = new ExpandedNodeId(Identifiers.RootFolder);
        r.setNodeId(eni);
        Browse(r);
        return root;
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
}