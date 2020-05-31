package com.ccdev.opcua_client.ui.connection;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ccdev.opcua_client.Core;
import com.ccdev.opcua_client.MainActivity;
import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.ui.adapters.EndpointAdapter;

import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.EndpointDescription;

import java.util.ArrayList;
import java.util.Arrays;

import static org.opcfoundation.ua.utils.EndpointUtil.selectByProtocol;
import static org.opcfoundation.ua.utils.EndpointUtil.sortBySecurityLevel;

public class ConnectionFragment extends Fragment {

    private ConnectionViewModel mViewModel;

    public static ConnectionFragment newInstance() {
        return new ConnectionFragment();
    }

    Handler mainHandler;

    EditText serverAddress;
    Button discoveryButton;

    ListView endpointsList;


    ProgressDialog dialog;
    String url;
    ArrayList<EndpointDescription> endpoints;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_connection, container, false);

        this.mainHandler = new Handler(getContext().getMainLooper());

        this.serverAddress = root.findViewById(R.id.text_server_address);
        this.discoveryButton = root.findViewById(R.id.discovery_button);
        this.discoveryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartDiscovery();
                MainActivity.hideSoftKeyboard(getActivity());
            }
        });

        this.endpointsList = root.findViewById(R.id.endpoints_list);

        this.endpointsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Do you want to connect to this endpoint?");
                builder.setMessage(endpoints.get(position).getEndpointUrl() + "\n"
                        + "SecurityMode: " + endpoints.get(position).getSecurityMode() + "\n"
                        + "SecurityLevel: " + endpoints.get(position).getSecurityLevel()
                );
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Connect(endpoints.get(position));
                    }
                });
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ConnectionViewModel.class);
    }

    public void StartDiscovery() {

        url = this.serverAddress.getText().toString().toLowerCase();
        url = url.toLowerCase().trim();

        if (!url.toLowerCase().startsWith("opc.tcp://")){
            url = "opc.tcp://" + url;
        }

        if (url.length() <= 10) {

            Toast.makeText(getContext(), "Invalid address format.", Toast.LENGTH_LONG).show();
            return;
        }

        this.serverAddress.setText(url);

        dialog = ProgressDialog.show(getContext(), "",
                "Discovering...", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                DiscoveryEndpoints();

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        UpdateEndpointsList(endpoints);
                    }
                });

            }
        }).start();

    }

    private void DiscoveryEndpoints(){
        try {

            endpoints = new ArrayList<>();

            EndpointDescription[] endpointDescriptions = Core.getInstance().getClient().discoverEndpoints(url);

            endpointDescriptions = selectByProtocol((endpointDescriptions), "opc.tcp");
            endpoints.addAll(Arrays.asList(endpointDescriptions));

        } catch (ServiceResultException e) {
            e.printStackTrace();

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Discover timeout.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void UpdateEndpointsList(ArrayList<EndpointDescription> endpoints){
        EndpointAdapter adapter = new EndpointAdapter(getContext(), endpoints);
        this.endpointsList.setAdapter(adapter);
    }


    private void Connect(EndpointDescription ed){

    }

}
