package com.mkrooted.bukiclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    public static final String TAG = "BukiClient1";
    TextView pibText, activeOrders, endedOrders, personalOrders, potentialOrders, lastUpdate;
    Button updateBtn;
    ImageView avatarView;
    String cookie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        checkLoggedIn();
    }

    private void init(){
        pibText = (TextView) findViewById(R.id.name_view);
        activeOrders = (TextView) findViewById(R.id.active_orders);
        endedOrders = (TextView) findViewById(R.id.ended_orders);
        personalOrders = (TextView) findViewById(R.id.personal_orders);
        potentialOrders = (TextView) findViewById(R.id.potential_orders);
        lastUpdate = (TextView) findViewById(R.id.last_update_date);
        updateBtn = (Button) findViewById(R.id.update_btn);
        avatarView = (ImageView) findViewById(R.id.avatar);
        cookie = null;
        updateBtn.setOnClickListener(this);

        SharedPreferences prefs = getSharedPreferences(TAG, 0);
        String lastUpdatedString = prefs.getString("lastUpdated", null);
        if (lastUpdatedString!=null) lastUpdate.setText(lastUpdatedString);
        pibText.setText(prefs.getString("name", "Дані відсутні"));
        activeOrders.setText(prefs.getString("activeOrders", "Дані відсутні"));
        endedOrders.setText(prefs.getString("endedOrders", "Дані відсутні"));
        personalOrders.setText(prefs.getString("personalOrders", "Дані відсутні"));
        potentialOrders.setText(prefs.getString("potentialOrders", "Дані відсутні"));
    }

    private void checkLoggedIn(){
        SharedPreferences preferences = getSharedPreferences(TAG, 0);
        cookie = preferences.getString("cookie", null);
        if(cookie == null){
            startActivity(new Intent(this, LoginActivity.class));
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.update_btn:
                updateBtn.setText("Оновлюється...");
                updateBtn.setEnabled(false);
                new DataUpdater().execute();
                break;
        }
    }

    class DataUpdater extends AsyncTask<Void, Void, Void>{
        Integer result;
        JSONObject response;
        Bitmap avatarBitmap;

        @Override
        protected Void doInBackground(Void... params) {
            ConnectivityManager connManager;
            NetworkInfo networkInfo;

            connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            networkInfo = connManager.getActiveNetworkInfo();

            if (networkInfo==null || !networkInfo.isConnected()){
                result = LoginResult.NO_CONNECTION;
                return null;
            }

            InputStream is = null;
            try{
                URL url = new URL("http://api.buki.com.ua");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                Log.d(TAG, "Cookie stored: "+HttpCookie.parse(cookie).get(0).toString());

                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Cookie", HttpCookie.parse(cookie).get(0).toString());

                connection.setDoInput(true);
                connection.connect();
                int responseCode = connection.getResponseCode();
                if(responseCode != HttpURLConnection.HTTP_OK){
                    Log.e("BukiClient", "RESPONSE CODE = "+responseCode);
                    result = LoginResult.HTTP_ERROR;
                    return null;
                }

                is = connection.getInputStream();
                Reader reader = new InputStreamReader(is, "UTF-8");
                char[] buffer = new char[500];
                reader.read(buffer);
                String rawResponse = new String(buffer);
                Log.d("BukiClient", "RawResponse: \n"+rawResponse);

                response = new JSONObject(rawResponse);
                if ( response.has("error") ){
                    Log.d("BukiClient", "Can't login");
                    result = LoginResult.INVALID_LOGIN;
                    return null;
                }

                result = LoginResult.SUCCESS;

                if (response.get("avatar") != null) {
                    url = new URL("http://buki.com.ua"+response.get("avatar").toString());
                    Log.d(TAG, url.toString());
                    connection = (HttpURLConnection) url.openConnection();

                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(10000);
                    connection.setRequestMethod("GET");

                    connection.setDoInput(true);
                    connection.connect();
                    responseCode = connection.getResponseCode();

                    if (responseCode == 200){
                        is = connection.getInputStream();
                        avatarBitmap = BitmapFactory.decodeStream(is);
                        Log.d(TAG, avatarBitmap.toString());
                    } else if(responseCode == 301){
                        String locationHeader = connection.getHeaderField("Location");
                        Log.d(TAG, "Redirect to "+locationHeader);

                        url = new URL(locationHeader);
                        connection = (HttpURLConnection) url.openConnection();

                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(10000);
                        connection.setRequestMethod("GET");

                        connection.setDoInput(true);
                        connection.connect();
                        responseCode = connection.getResponseCode();
                        if(responseCode == 200){
                            is = connection.getInputStream();
                            avatarBitmap = BitmapFactory.decodeStream(is);
                        } else {
                            Log.e(TAG, "Error downloading avatar after redirect: responsecode="+responseCode);
                        }
                    }
                    else {
                        Log.e(TAG, "Error downloading avatar: responsecode="+responseCode);
                        avatarBitmap = null;
                    }
                }
                else avatarBitmap = null;

                return null;
            }
            catch (IOException | JSONException e){
                e.printStackTrace();
            } finally {
                if(is != null){
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            SharedPreferences prefs = getSharedPreferences(TAG, 0);
            SharedPreferences.Editor editor = prefs.edit();
            Calendar now = Calendar.getInstance();
            String day = now.get(Calendar.DATE)+"";
            if (day.length()==1) day = "0"+day;
            String month = now.get(Calendar.MONTH)+"";
            if (month.length()==1) month = "0"+month;

            String currentDate = day+"."+month+"."+now.get(Calendar.YEAR) +
                    " "+now.get(Calendar.HOUR_OF_DAY)+":"+now.get(Calendar.MINUTE);
            editor.putString("lastUpdated", currentDate);
            lastUpdate.setText(currentDate);
            pibText.setText(prefs.getString("name", "Дані відсутні"));

            updateBtn.setText("Оновити");
            updateBtn.setEnabled(true);
            if (result == LoginResult.SUCCESS) {
                Toast.makeText(getApplicationContext(), "Успішно оновлено", Toast.LENGTH_SHORT).show();
                try {
                    activeOrders.setText(response.get("active").toString());
                    endedOrders.setText(response.get("ended").toString());
                    personalOrders.setText(response.get("personal").toString());
                    potentialOrders.setText(response.get("potential").toString());

                    editor.putString("activeOrders", response.get("active").toString());
                    editor.putString("endedOrders", response.get("ended").toString());
                    editor.putString("personalOrders", response.get("personal").toString());
                    editor.putString("potentialOrders", response.get("potential").toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (avatarBitmap != null) {
                    avatarView.setImageBitmap(avatarBitmap);

                }
            } else if(result == LoginResult.INVALID_LOGIN) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            } else if(result == LoginResult.NO_CONNECTION) {
                Toast.makeText(getApplicationContext(), "Відсутнє підключення до мережі", Toast.LENGTH_SHORT).show();
            }
            editor.apply();
        }
    }
}
