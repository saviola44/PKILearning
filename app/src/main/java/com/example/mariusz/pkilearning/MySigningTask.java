package com.example.mariusz.pkilearning;

import android.content.Context;
import android.os.AsyncTask;
import android.security.KeyChain;
import android.util.Base64;
import android.util.Log;

import java.security.PrivateKey;
import java.security.Signature;

/**
 * Created by mariusz on 12.02.17.
 */
public class MySigningTask extends AsyncTask<String, Void, String> {
    Context mContext;
    HandleSignedData handler;
    public interface HandleSignedData{
        void update(String signedText);
    }

    public MySigningTask(Context mContext, HandleSignedData handler) {
        this.mContext = mContext;
        this.handler = handler;
    }

    @Override
    protected String doInBackground(String... params) {
        return createSignedNote(params[0], params[1]);
    }

    @Override
    protected void onPostExecute(String s) {
        handler.update(s);
    }

    public String createSignedNote(String textToSign, String alias) {
        try {
            byte[] textData = textToSign.getBytes("UTF-8");
            PrivateKey privateKey = KeyChain.getPrivateKey(mContext, alias);
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            signature.update(textData);
            byte[] signed = signature.sign();
            return Base64.encodeToString(textData, Base64.NO_WRAP | Base64.NO_PADDING) + "]" +
                    Base64.encodeToString(signed, Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (Exception e) {
            Log.e(MySigningTask.class.getName(), "Błąd podpisywania danych.", e);
        }
        return null;
    }
}
