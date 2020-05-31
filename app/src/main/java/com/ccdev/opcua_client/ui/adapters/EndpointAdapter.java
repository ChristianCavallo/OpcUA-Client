package com.ccdev.opcua_client.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ccdev.opcua_client.R;

import org.opcfoundation.ua.core.EndpointDescription;
import org.opcfoundation.ua.transport.Endpoint;

import java.util.List;

public class EndpointAdapter extends ArrayAdapter<EndpointDescription> {

    public EndpointAdapter(@NonNull Context context, @NonNull List<EndpointDescription> objects) {
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        EndpointDescription endpointDescription = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.endpoint_listitem, parent, false);
        }
        // Lookup view for data population
        TextView uriView = (TextView) convertView.findViewById(R.id.uriTextView);
        TextView securityModeView = (TextView) convertView.findViewById(R.id.securityModeTextView);
        TextView securityPolicyView = (TextView) convertView.findViewById(R.id.securityPolicyTextView);
        TextView securityLevelView = (TextView) convertView.findViewById(R.id.securityLevelTextView);

        // Populate the data into the template view using the data object
        uriView.setText(endpointDescription.getEndpointUrl());
        securityModeView.setText(endpointDescription.getSecurityMode().name());
        securityPolicyView.setText(endpointDescription.getSecurityPolicyUri().split("#")[1]);
        securityLevelView.setText(endpointDescription.getSecurityLevel().toString());

        // Return the completed view to render on screen
        return convertView;
    }
}
