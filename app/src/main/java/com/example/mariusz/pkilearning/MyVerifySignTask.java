package com.example.mariusz.pkilearning;

/**
 * Created by mariusz on 12.02.17.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.security.KeyChain;
import android.util.Base64;
import android.util.Log;

import java.security.PublicKey;
import java.security.Signature;

import java.security.cert.X509Certificate;

/**
 * Created by mariusz on 12.02.17.
 */
public class MyVerifySignTask extends AsyncTask<String, Void, Boolean> {
    Context mContext;
    HandleVerifiedData handler;
    public interface HandleVerifiedData{
        void verified(boolean signedText);
    }

    public MyVerifySignTask(Context mContext, HandleVerifiedData handler) {
        this.mContext = mContext;
        this.handler = handler;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        return verifySignature(params[0], params[1]);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        handler.verified(aBoolean);
    }

    private Boolean verifySignature(String dataAndSignature, String alias) {
        try {
            String[] parts = dataAndSignature.split("]");
            byte[] decodedText = Base64.decode(parts[0], Base64.DEFAULT);
            byte[] signed = Base64.decode(parts[1], Base64.DEFAULT);
            String decodedTextString = Base64.encodeToString(decodedText, Base64.NO_WRAP);
            String signedString = Base64.encodeToString(signed, Base64.NO_WRAP);
            Log.d(this.getClass().getName(), "decodedTextString = " + decodedTextString);
            Log.d(this.getClass().getName(), "signedString = " + signedString);

            X509Certificate[] chain = KeyChain.getCertificateChain(mContext, alias);
            PublicKey publicKey = chain[0].getPublicKey();
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(publicKey);
            signature.update(decodedText);

            return signature.verify(signed);
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "Błąd weryfikacji podpisu.", e);
        }
        return false;
    }
}
