package com.ccdev.opcua_client.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    private MutableLiveData<String>  prova;
    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
        prova = new MutableLiveData<>();

        prova.postValue("ti amo christian");
    }


    public LiveData<String> getProva() {
        return prova;
    }

    public LiveData<String> getText() {
        return mText;
    }
}