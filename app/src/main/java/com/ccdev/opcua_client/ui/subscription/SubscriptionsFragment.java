package com.ccdev.opcua_client.ui.subscription;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.core.Core;
import com.ccdev.opcua_client.core.CoreInterface;
import com.ccdev.opcua_client.ui.adapters.SubscriptionAdapter;

public class SubscriptionsFragment extends Fragment implements CoreInterface {

    RecyclerView subscriptionsListView;
    SubscriptionAdapter subscriptionAdapter;
    TextView subscriptionAlertText;

    Handler mainHandler;

    public SubscriptionsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.mainHandler = new Handler(getContext().getMainLooper());
        View root = inflater.inflate(R.layout.fragment_subscriptions, container, false);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());

        this.subscriptionsListView = (RecyclerView) root.findViewById(R.id.subscriptionsListView);
        this.subscriptionAdapter = new SubscriptionAdapter(Core.getInstance().getSubscriptions());
        this.subscriptionsListView.setAdapter(this.subscriptionAdapter);

        this.subscriptionsListView.setLayoutManager(llm);

        this.subscriptionAlertText = (TextView) root.findViewById(R.id.subscriptionAlertTextView);

        if (Core.getInstance().getSubscriptions().isEmpty()) {
            this.subscriptionAlertText.setVisibility(View.VISIBLE);
        } else {
            this.subscriptionAlertText.setVisibility(View.GONE);
        }

        Core.getInstance().registerListener(this);
        return root;
    }

    @Override
    public void onUpdateReceived() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                subscriptionAdapter.notifyDataSetChanged();
                if (Core.getInstance().getSubscriptions().isEmpty()) {
                    subscriptionAlertText.setVisibility(View.VISIBLE);
                }
            }
        });
    }

}