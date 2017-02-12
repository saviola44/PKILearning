package com.example.mariusz.pkilearning;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.mariusz.pkilearning.Util.FileUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements MySigningTask.HandleSignedData, MyVerifySignTask.HandleVerifiedData{
    public static String TAG = MainActivity.class.getCanonicalName();
    private static final int FILE_SELECT_CODE = 0;


    private String CERTIFICATE_PATH = null;
    private static final String CERTIFICATE_NAME = "MyCertificate";
    private static final int INSTALL_CERT_CODE = 1001;

    byte[] certData;

    private String messageToSign = "tekst na ktorym zloze podpis";

    @BindView(R.id.chosenFilePathTV)
    TextView chosenFilePathTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        chosenFilePathTV.setEnabled(false);
    }

    @OnClick(R.id.selectCertBtn)
    public void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        try {
            startActivityForResult(
            Intent.createChooser(intent, "Select your PKI certificate"),
            FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Log.d(MainActivity.class.getName(), "Error occuring selecting certificate " + ex.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();

                    //below is unused, I only display path to this certificate
                    CERTIFICATE_PATH = FileUtil.getPath(this, uri);
                    chosenFilePathTV.setEnabled(true);
                        chosenFilePathTV.setText(CERTIFICATE_PATH);
                        Log.d(TAG, CERTIFICATE_PATH);

                    //below is important, I read data from selected file
                    try {
                        readFile(uri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case INSTALL_CERT_CODE:
                if(requestCode == INSTALL_CERT_CODE) {
                    if(resultCode == RESULT_OK) {
                        Log.d(TAG, "instalacja certyfikatu zakończona pomyślnie");
                    } else {
                       Log.d(TAG, "Użytkownik anulował instalację certyfikatu.");
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @OnClick(R.id.installCertBtn)
    public void doInstallCertificate(View view) {
        Intent installCert = KeyChain.createInstallIntent();
        installCert.putExtra(KeyChain.EXTRA_NAME, CERTIFICATE_NAME);
        installCert.putExtra(KeyChain.EXTRA_PKCS12, certData);
        startActivityForResult(installCert, INSTALL_CERT_CODE);
    }

    /**
     * if we want read certificate from assets folder
     */
    private void readCertFromAssets(){
        try {
            BufferedInputStream bis = new BufferedInputStream(getAssets().open(
                    "MyKeystore.pfx"));
            certData = new byte[bis.available()];
            bis.read(certData);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * if we want read certificate from selected file chossen from file chooser dialog
     * @param uri
     * @throws FileNotFoundException
     */
    private void readFile(Uri uri) throws FileNotFoundException {
        InputStream in= getContentResolver().openInputStream(uri);
        try {
            certData = getBytes(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
    String alias;
    @OnClick(R.id.signTextbtn)
    public void doSignNoteData(View view) {
        KeyChain.choosePrivateKeyAlias(this, new KeyChainAliasCallback() {
            @Override
            public void alias(String alias) {
                MainActivity.this.alias = alias;
                new MySigningTask(getApplicationContext(), MainActivity.this).execute(messageToSign, alias);
            }
        }, null, null, null, -1, null);
    }

    @Override
    public void update(String signedText) {
        messageToSign = signedText;
        Log.d("jestem", "signedtext = " + signedText);

        /* takie cos wypluwa
        dGVrc3QgbmEga3RvcnltIHpsb3plIHBvZHBpcw]GT3+jTyCRUF2mmFh6ZSKfXep3Q025onHcx13juCRNP9Zq632jIPHIPW1fOsp0UiGuHNWXi3GEeIohJa7c1kp7a+rCShDzWelVNpBQUMfUz0DzOiy6NnvXbjbgndHnnX1zZfP1kghBb3yrPm2QyPU/5pZIT0Z0aRoqqUMLNb+IVVSEuAmITS9D9SiX2LTT7ajGxlA2TRyLFLBIT/qipUEWYOPHOVwqU6uXBmmNIGDE4Hxr/JS1v6Ea8+xrBl2ipjm5FH7URX7tA2FaFlpVKD247uG6lDROrUULVEjEVaFFyY42a52hfL4YNGDpcPqdk2pIDaxDBDFuHy8bLhmH+Fwng
         */
        new MyVerifySignTask(this, this).execute(signedText, alias);
    }


    @Override
    public void verified(boolean signedText) {
        Log.d("jestem", "verified text = " + signedText);
    }
}
