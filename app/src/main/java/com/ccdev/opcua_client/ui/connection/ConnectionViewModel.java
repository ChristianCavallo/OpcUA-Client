package com.ccdev.opcua_client.ui.connection;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConnectionViewModel extends ViewModel {

    private MutableLiveData<String> myText;


    public ConnectionViewModel() {
        this.myText = new MutableLiveData<>();

        this.myText.setValue("Ciao");


    }

    public LiveData<String> getMyText() {
        return myText;
    }
}
