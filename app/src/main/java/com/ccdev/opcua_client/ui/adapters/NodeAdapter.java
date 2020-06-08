package com.ccdev.opcua_client.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ccdev.opcua_client.R;

import org.opcfoundation.ua.core.ReferenceDescription;


public class NodeAdapter extends ArrayAdapter<ReferenceDescription> {


    public NodeAdapter(@NonNull Context context, @NonNull ReferenceDescription[] objects) {
        super(context, 0, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        ReferenceDescription ref = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_node, parent, false);
        }
        // Lookup view for data population
        TextView nameNodeView = (TextView) convertView.findViewById(R.id.nameNodeTextView);
        TextView namespaceView = (TextView) convertView.findViewById(R.id.namespaceTextView);
        TextView nodeIndexView = (TextView) convertView.findViewById(R.id.nodeIndexTextView);
        TextView classNodeView = (TextView) convertView.findViewById(R.id.classNodeTextView);
        ImageView iconNodeView = (ImageView) convertView.findViewById(R.id.iconNodeImageView);

        // Populate the data into the template view using the data object
        nameNodeView.setText(ref.getDisplayName().getText());
        namespaceView.setText("Namespace: " + ref.getNodeId().getNamespaceIndex());
        nodeIndexView.setText("NamespaceIndex: " + ref.getNodeId().getValue().toString());
        classNodeView.setText("Class: " + ref.getNodeClass().name());

        switch (ref.getNodeClass().getValue()) {
            case 0:
                break;
            case 1:
                iconNodeView.setImageResource(R.drawable.ic_folder_24dp);
                break;
            case 2:
                iconNodeView.setImageResource(R.drawable.ic_description_24dp);
                break;
            case 4:
                break;
            case 8:
                break;
            case 16:
                break;
            case 32:
                break;
            case 64:
                break;
            case 128:
                break;
            default:
                break;
        }
        /*
                *     Unspecified(0),
    Object(1),
    Variable(2),
    Method(4),
    ObjectType(8),
    VariableType(16),
    ReferenceType(32),
    DataType(64),
    View(128);
	*/
        // Return the completed view to render on screen
        return convertView;
    }
}
