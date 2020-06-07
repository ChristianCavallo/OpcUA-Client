package com.ccdev.opcua_client.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.core.Core;
import com.ccdev.opcua_client.core.CoreInterface;
import com.ccdev.opcua_client.ui.adapters.CustomizedElementAdapter;

public class HomeFragment extends Fragment implements CoreInterface {
    TextView elementAlertView;
    RecyclerView elementsListView;
    CustomizedElementAdapter elementAdapter;
    Handler mainHandler;

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        mainHandler = new Handler(Looper.getMainLooper());

        elementAlertView = (TextView) root.findViewById(R.id.elementsAlertTextView);

        elementsListView = (RecyclerView) root.findViewById(R.id.elementsRecyclerView);
        elementAdapter = new CustomizedElementAdapter(Core.getInstance().getCustomElements());
        elementsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        elementsListView.setAdapter(elementAdapter);

        Core.getInstance().registerListener(this);

        if(Core.getInstance().getCustomElements().isEmpty()){
            elementAlertView.setVisibility(View.VISIBLE);
        } else {
            elementAlertView.setVisibility(View.GONE);
        }
        return root;
    }

    @Override
    public void onUpdateReceived() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                elementAdapter.notifyDataSetChanged();
                if(Core.getInstance().getCustomElements().isEmpty()){
                    elementAlertView.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
