package com.ccdev.opcua_client;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import com.ccdev.opcua_client.wrappers.ExtendedSubscription;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.application.Session;
import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.CreateSubscriptionRequest;
import org.opcfoundation.ua.core.CreateSubscriptionResponse;
import org.opcfoundation.ua.core.EndpointDescription;
import org.opcfoundation.ua.core.UserIdentityToken;
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

    public void notifySubscriptionCreated() {
        if (listeners.size() > 0) {
            for (CoreInterface next : listeners) {
                if (next != null) {
                    next.onSubscriptionCreated();
                } else {
                    listeners.remove(next);
                }
            }
        }
    }

    public void registerListener(CoreInterface mListener) {
        if (!listeners.contains(mListener)) {
            listeners.add(mListener);
        }
    }

    public void UnregisterListener(CoreInterface listener) {
        listeners.remove(listener);
    }

    //==============================================================================================


    // SUBSCRIPTIONS ===============================================================================

    ArrayList<ExtendedSubscription> subscriptions = new ArrayList<>();

    public void createSubscription(ExtendedSubscription ex) throws ServiceResultException {
        CreateSubscriptionResponse res = sessionChannel.CreateSubscription(ex.getRequest());
        ex.setResponse(res);
        subscriptions.add(ex);
    }

    public ArrayList<ExtendedSubscription> getSubscriptions() {
        return subscriptions;
    }

    // =============================================================================================





}
