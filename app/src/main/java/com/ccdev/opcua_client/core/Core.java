package com.ccdev.opcua_client.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;
import com.ccdev.opcua_client.wrappers.ExtendedSubscription;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.ExtensionObject;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.CreateSubscriptionResponse;
import org.opcfoundation.ua.core.DataChangeNotification;
import org.opcfoundation.ua.core.DeleteMonitoredItemsRequest;
import org.opcfoundation.ua.core.DeleteMonitoredItemsResponse;
import org.opcfoundation.ua.core.DeleteSubscriptionsRequest;
import org.opcfoundation.ua.core.DeleteSubscriptionsResponse;
import org.opcfoundation.ua.core.EndpointDescription;
import org.opcfoundation.ua.core.MonitoredItemNotification;
import org.opcfoundation.ua.core.MonitoringMode;
import org.opcfoundation.ua.core.PublishResponse;
import org.opcfoundation.ua.core.SetMonitoringModeRequest;
import org.opcfoundation.ua.core.SetMonitoringModeResponse;
import org.opcfoundation.ua.core.SetPublishingModeRequest;
import org.opcfoundation.ua.core.SetPublishingModeResponse;
import org.opcfoundation.ua.encoding.DecodingException;
import org.opcfoundation.ua.transport.security.Cert;
import org.opcfoundation.ua.transport.security.CertificateValidator;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.PrivKey;
import org.opcfoundation.ua.utils.CertificateUtils;

import java.io.File;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class Core {

    private static Core _instance;

    private Core() {

    }

    public static Core getInstance() {
        if (_instance == null)
        {
            _instance = new Core();
        }
        return _instance;
    }

    Context context;
    org.opcfoundation.ua.application.Application opcApplication;
    Client client;
    Publisher publisher = null;
    Thread publisherThread;

    public Client getClient() {
        return client;
    }

    public void InitializeClient(Context t){
        if(t != null){
            this.context = t;
        }

        if(Looper.myLooper() == Looper.getMainLooper()){

            new Thread(new Runnable() {
                @Override
                public void run() {
                    InitializeClient(null);
                }
            }).start();
            return;
        }

        File certFile = new File(this.context.getFilesDir(), "OPCCert.der");
        File privKeyFile = new File(this.context.getFilesDir(), "OPCCert.pem");
        SharedPreferences sharedPref = this.context.getSharedPreferences("OpcUA_Preferences", MODE_PRIVATE);;

        String PrivateKey = sharedPref.getString("private_key", "");
        KeyPair keys = null;
        if(certFile.exists() && privKeyFile.exists() && !PrivateKey.isEmpty()){

            try {
                Cert myCertificate = null;
                myCertificate = Cert.load(certFile);
                PrivKey myPrivateKey = PrivKey.load(privKeyFile, PrivateKey);
                keys = new KeyPair(myCertificate, myPrivateKey);
                Log.i("CLIENT", "Certificate loaded!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            PrivateKey = generateString(256);
            try {
                keys = CertificateUtils.createApplicationInstanceCertificate("OpcUA_Client", "ccdev",
                                "com.ccdev.opcua_client", 3650);
                keys.getCertificate().save(certFile);
                keys.getPrivateKey().save(privKeyFile, PrivateKey);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("private_key", PrivateKey);
                editor.commit();
                Log.i("CLIENT", "Certificate created!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        opcApplication = new org.opcfoundation.ua.application.Application();
        opcApplication.addApplicationInstanceCertificate(keys);
        opcApplication.getOpctcpSettings().setCertificateValidator(CertificateValidator.ALLOW_ALL);
        opcApplication.getHttpsSettings().setCertificateValidator(CertificateValidator.ALLOW_ALL);
        opcApplication.getHttpsSettings().setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        opcApplication.setApplicationUri("com.ccdev.opcua_client");
        client = new Client(opcApplication);
    }

    public void ShutDown(){
        try {
             if(sessionChannel != null){
                 sessionChannel.close();
            }
            if(publisher != null){
                publisher.setEnabled(false);
                publisher = null;
            }
             client.getApplication().close();
             _instance = null;
            Log.i("CLIENT", "Everything closed!");
        } catch (ServiceResultException e) {
            e.printStackTrace();
        }
    }

    private static String generateString(int n)
    {
        String AlphaNumericString = "0123456789" + "abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {
            int index
                    = (int)(AlphaNumericString.length()
                    * Math.random());
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }

    //SESSION CREATION AND ACTIVATION ===============================================================

    SessionChannel sessionChannel;
    String serverUrl;
    EndpointDescription endpointDescription;

    public void createSession(String url, EndpointDescription e) throws ServiceResultException {
        if(sessionChannel != null){
            sessionChannel.close();
        }
        sessionChannel = client.createSessionChannel(url, e);

        this.endpointDescription = e;
        this.serverUrl = url;
    }

    public void activateSession(String username, String password) throws ServiceResultException {
        if(!username.isEmpty() && !password.isEmpty()){
           sessionChannel.activate(username, password);
        } else {
            sessionChannel.activate();
        }

    }

    public SessionChannel getSessionChannel() {
        return sessionChannel;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public EndpointDescription getEndpointDescription() {
        return endpointDescription;
    }

    //==============================================================================================



    // OBSERVER PATTERN ============================================================================

    ArrayList<CoreInterface> listeners = new ArrayList();

    public void notifyUpdate() {
        if (listeners.size() > 0) {
            for (CoreInterface next : listeners) {
                if (next != null) {
                    next.onUpdateReceived();
                } else {
                    unregisterListener(next);
                }
            }
        }
    }

    public void registerListener(CoreInterface mListener) {
        if (!listeners.contains(mListener)) {
            listeners.add(mListener);
        }
    }

    public void unregisterListener(CoreInterface listener) {
        listeners.remove(listener);
    }

    //==============================================================================================


    // SUBSCRIPTIONS ===============================================================================

    ArrayList<ExtendedSubscription> subscriptions = new ArrayList<>();

    public void createSubscription(ExtendedSubscription ex) throws ServiceResultException {
        CreateSubscriptionResponse res = sessionChannel.CreateSubscription(ex.getRequest());
        ex.setResponse(res);
        subscriptions.add(ex);

        startPublisher();
    }

    public ArrayList<ExtendedSubscription> getSubscriptions() {
        return subscriptions;
    }

    public boolean switchPublishingSubscription(ExtendedSubscription ex) throws ServiceResultException {
        boolean status = !ex.getRequest().getPublishingEnabled();
        SetPublishingModeRequest request = new SetPublishingModeRequest();
        request.setPublishingEnabled(status);
        request.setSubscriptionIds(new UnsignedInteger[]{ex.getResponse().getSubscriptionId()});

        SetPublishingModeResponse response = sessionChannel.SetPublishingMode(request);
        if(response.getResults()[0].isGood()){
            ex.getRequest().setPublishingEnabled(status);
            return true;
        }
        return false;
    }

    public boolean removeSubscription(ExtendedSubscription ex) throws ServiceResultException {

        DeleteSubscriptionsRequest req = new DeleteSubscriptionsRequest();
        req.setSubscriptionIds(new UnsignedInteger[]{ex.getResponse().getSubscriptionId()});

        DeleteSubscriptionsResponse res = sessionChannel.DeleteSubscriptions(req);
        if(res.getResults()[0].isGood()){
            subscriptions.remove(ex);
            if(subscriptions.isEmpty()){
                stopPublisher();
            }
            return true;
        }
        return false;
    }

    public boolean switchMonitoringMode(ExtendedMonitoredItem ex) throws ServiceResultException {
        SetMonitoringModeRequest req = new SetMonitoringModeRequest();
        MonitoringMode mode = MonitoringMode.Reporting;

        if(ex.getRequest().getItemsToCreate()[0].getMonitoringMode() == MonitoringMode.Reporting){
           mode = MonitoringMode.Sampling;
        }

        req.setMonitoringMode(mode);

        ExtendedSubscription subscription = null;
        for(int i = 0; i < subscriptions.size(); i++){

            for(int j = 0; j < subscriptions.get(i).getMonitoredItems().size(); j++){
                if(subscriptions.get(i).getMonitoredItems().get(j).getId() == ex.getId()){
                    subscription = subscriptions.get(i);
                    break;
                }
            }

            if(subscription != null){
                break;
            }
        }

        if(subscription == null){
            return false;
        }

        req.setSubscriptionId(subscription.getResponse().getSubscriptionId());
        req.setMonitoredItemIds(new UnsignedInteger[]{ex.getMonitoredItem().getMonitoredItemId()});

        SetMonitoringModeResponse res = sessionChannel.SetMonitoringMode(req);
        if(res.getResults()[0].isGood()){
            ex.getRequest().getItemsToCreate()[0].setMonitoringMode(mode);
            return true;
        }
        return false;
    }

    public boolean removeMonitoredItem(ExtendedMonitoredItem ex) throws ServiceResultException {
        DeleteMonitoredItemsRequest req = new DeleteMonitoredItemsRequest();
        ExtendedSubscription subscription = null;
        for(int i = 0; i < subscriptions.size(); i++){

            for(int j = 0; j < subscriptions.get(i).getMonitoredItems().size(); j++){
                if(subscriptions.get(i).getMonitoredItems().get(j).getId() == ex.getId()){
                    subscription = subscriptions.get(i);
                    break;
                }
            }

            if(subscription != null){
                break;
            }
        }

        if(subscription == null){
            return false;
        }

        req.setSubscriptionId(subscription.getResponse().getSubscriptionId());
        req.setMonitoredItemIds(new UnsignedInteger[]{ex.getMonitoredItem().getMonitoredItemId()});

        DeleteMonitoredItemsResponse res = sessionChannel.DeleteMonitoredItems(req);
        if(res.getResults()[0].isGood()){
            subscription.getMonitoredItems().remove(ex);
            return true;
        }
        return false;
    }


    // =============================================================================================

    // PUBLISH =====================================================================================

    public void updateSubscription(PublishResponse res){

        int index = -1;
        for(int i = 0; i < subscriptions.size(); i++){
            if(subscriptions.get(i).getResponse().getSubscriptionId() == res.getSubscriptionId()){
                index = i;
                break;
            }
        }

        if(index < 0){
            return;
        }

        subscriptions.get(index).setLastAck(res.getNotificationMessage().getSequenceNumber().intValue());

        for (ExtensionObject obj: res.getNotificationMessage().getNotificationData()) {
            try {
                Object decoded = obj.decode(client.getEncoderContext());
                if(decoded instanceof DataChangeNotification){
                    DataChangeNotification update = (DataChangeNotification) decoded;

                    for(MonitoredItemNotification m: update.getMonitoredItems()){
                        updateMonitoredItem(index, m);
                    }
                }
            } catch (DecodingException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateMonitoredItem(int subid, MonitoredItemNotification notification){
        for (ExtendedMonitoredItem m : subscriptions.get(subid).getMonitoredItems()) {
            if(m.getId() == notification.getClientHandle().intValue()){
                m.addRead(notification);
                break;
            }
        }
    }

    public void startPublisher(){
        if(publisher == null){
            publisher = new Publisher();
        }

        publisherThread = new Thread(publisher);
        publisherThread.start();
    }

    public void stopPublisher(){
        publisher.setEnabled(false);
        publisher = null;
    }

    // =============================================================================================



}
