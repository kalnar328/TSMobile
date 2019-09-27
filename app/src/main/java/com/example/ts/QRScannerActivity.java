package com.example.ts;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

public class QRScannerActivity extends AppCompatActivity {

    String scannedData;
    Button scanBtn;
    String destination;
    String start;
    int count=0;

    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        sharedpreferences = getSharedPreferences("Pref", Context.MODE_PRIVATE);

//        destination = "test";
//        start = "xxx";
        destination = sharedpreferences.getString("From", "");
        start = sharedpreferences.getString("To", "");

        final Activity activity =this;

        scanBtn = (Button)findViewById(R.id.scan_btn);

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(activity);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setPrompt("Scan");
                integrator.setBeepEnabled(false);
                integrator.setCameraId(0);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
        if(result!=null) {
            scannedData = result.getContents();
            if (scannedData != null) {
                // Handle scanned data

                new SendRequest().execute();


            }else {
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public class SendRequest extends AsyncTask<String, Void, String> {


        protected void onPreExecute(){}

        protected String doInBackground(String... arg0) {

            try{

                //Script URL Here
                URL url = new URL("https://script.google.com/macros/s/AKfycbyZNThIgnoJOP2VlytDyCeXO4SjVmKlXdblQMN72rPooIBKUMg/exec");

                JSONObject postDataParams = new JSONObject();

                //Passing scanned code as parameter
                postDataParams.put("sdata",scannedData);

                Log.e("***************",scannedData);
                Log.e("params",postDataParams.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));

                writer.flush();
                writer.close();
                os.close();

                int responseCode=conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {

                    BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuffer sb = new StringBuffer("");
                    String line="";

                    while((line = in.readLine()) != null) {

                        sb.append(line);
                        break;
                    }

                    in.close();
                    return sb.toString();

                }
                else {
                    return new String("false : "+responseCode);
                }
            }
            catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), result,
                    Toast.LENGTH_LONG).show();

            //////////////////////////////////////////////////////////////////////////////

            Log.e("-------------------",scannedData);

            if(scannedData.equals(destination)) {
                Intent intent = new Intent(QRScannerActivity.this, ValidActivity.class);
                QRScannerActivity.this.startActivity(intent);

                Bundle bundle = new Bundle();
                bundle.putString("qrValue", scannedData);
                intent.putExtras(bundle);

                count--;
            }else if(scannedData.equals(start)){
                Intent intent = new Intent(QRScannerActivity.this, ValidActivity.class);
                QRScannerActivity.this.startActivity(intent);

                Bundle bundle = new Bundle();
                bundle.putString("qrValue", scannedData);
                intent.putExtras(bundle);

                count++;
            }else
                {
                Intent intent = new Intent(QRScannerActivity.this, InvalidActivity.class);
                QRScannerActivity.this.startActivity(intent);
            }
        }
    }

    public String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            //result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }
}
