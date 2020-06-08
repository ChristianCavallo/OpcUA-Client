package com.ccdev.opcua_client.ui.connection;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.ccdev.opcua_client.MainActivity;
import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.core.Core;
import com.ccdev.opcua_client.ui.adapters.EndpointAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.EndpointDescription;

import java.util.ArrayList;
import java.util.Arrays;

import static org.opcfoundation.ua.utils.EndpointUtil.selectByProtocol;

public class ConnectionFragment extends Fragment {

    public static ConnectionFragment newInstance() {
        return new ConnectionFragment();
    }

    Handler mainHandler;

    EditText serverAddress;
    Button discoveryButton;
    ListView endpointsList;

    ProgressDialog dialog;
    String url;
    EndpointDescription selectedEndpoint;
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
                if (Core.getInstance().getClient() == null) {
                    Toast.makeText(getContext(), "The client is still initializating, try in a few seconds.", Toast.LENGTH_LONG).show();
                    return;
                }
                StartDiscovery();
                MainActivity.hideSoftKeyboard(getActivity());
            }
        });

        this.endpointsList = root.findViewById(R.id.endpoints_list);
        this.endpointsList.setClickable(true);
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
                        selectedEndpoint = endpoints.get(position);
                        CreateSession();
                    }
                });

                builder.setNegativeButton("No", null);

                builder.show();
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void StartDiscovery() {

        url = this.serverAddress.getText().toString().toLowerCase();
        url = url.toLowerCase().trim();

        if (!url.toLowerCase().startsWith("opc.tcp://")) {
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

    private void DiscoveryEndpoints() {
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

    private void UpdateEndpointsList(ArrayList<EndpointDescription> endpoints) {
        EndpointAdapter adapter = new EndpointAdapter(getContext(), endpoints);
        this.endpointsList.setAdapter(adapter);
    }

    private void CreateSession() {
        dialog = ProgressDialog.show(getContext(), "",
                "Creating the session...", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Core.getInstance().createSession(url, selectedEndpoint);
                } catch (ServiceResultException ex) {
                    final ServiceResultException e = ex;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(getContext(), "Error: " + e.getStatusCode().getDescription() + ". Code: " + e.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        ShowAuthenticationDialog();
                    }
                });

            }
        }).start();
    }

    private void ShowAuthenticationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_credentials, null);

        RadioButton authCertificateRadio = (RadioButton) dialogView.findViewById(R.id.authCertificateRadioButton);
        final RadioButton authUserPasswordRadio = (RadioButton) dialogView.findViewById(R.id.authUserPassRadioButton);
        final TextView authUsernameText = (TextView) dialogView.findViewById(R.id.authUsernameTextView);
        final TextView authPasswordText = (TextView) dialogView.findViewById(R.id.authPasswordTextView);

        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton("Activate Session", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (authUserPasswordRadio.isChecked() && !authUsernameText.getText().toString().isEmpty()) {
                            ActivateSession(authUsernameText.getText().toString().trim(), authPasswordText.getText().toString().trim());
                        } else {
                            ActivateSession("", "");
                        }
                    }
                })
                .setNegativeButton("Abort", null);
        builder.show();
    }

    private void ActivateSession(final String username, final String password) {
        dialog = ProgressDialog.show(getContext(), "",
                "Activating the session...", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Core.getInstance().activateSession(username, password);
                } catch (ServiceResultException ex) {
                    final ServiceResultException e = ex;
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(getContext(), "Error: " + e.getStatusCode().getDescription() + ". Code: " + e.getStatusCode().getValue().toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        GoToHome();
                    }
                });

            }
        }).start();
    }

    private void GoToHome() {
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        navController.popBackStack();
        navController.navigate(R.id.navigation_home);
        BottomNavigationView m = (BottomNavigationView) getActivity().findViewById(R.id.nav_view);
        m.getMenu().findItem(R.id.navigation_connection).setEnabled(false);
    }

}
