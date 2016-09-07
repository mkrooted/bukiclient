package com.mkrooted.bukiclient;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OrdersListActivity extends AppCompatActivity implements ListView.OnItemClickListener {
    Integer ORDERS_TYPE;
    ListView orderList;
    String[] orderTitles;
    ArrayAdapter<String> orderListAdapter;
    boolean dataAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_list);

        ORDERS_TYPE = getIntent().getIntExtra("OrderType", OrderType.ACTIVE);
        orderList = (ListView) findViewById(R.id.ordersListView);
        dataAvailable = false;
        new GetOrdersTask().execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, OrderDetailsActivity.class);
        intent.putExtra("orderData", prepareIntentData(id));
        startActivity(intent);
    }

    class GetOrdersTask extends AsyncTask<Void, Void, Void>{
        Integer result;
        private JSONObject jsonResponse;

        @Override
        protected Void doInBackground(Void... params) {
            ConnectivityManager connManager;
            NetworkInfo networkInfo;

            connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            networkInfo = connManager.getActiveNetworkInfo();

            if (networkInfo==null || !networkInfo.isConnected()){
                result = LoginResult.NO_CONNECTION;

                File cacheFile = new File(getCacheDir(),"order_titles_"+ORDERS_TYPE+".json");
                try {
                    FileInputStream fis = new FileInputStream(cacheFile);

                    byte[] buffer = new byte[2000];
                    fis.read(buffer);
                    jsonResponse = new JSONObject(new String(buffer));
                    dataAvailable = true;
                } catch (FileNotFoundException | JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    cacheFile.delete();
                }

                return null;
            }

            InputStream is = null;

            try {
                URL url = new URL("api.buki.com.ua/"); //TODO Change url
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                connection.setDoOutput(true);
                connection.setDoInput(true);
                OutputStreamWriter oswriter = new OutputStreamWriter(connection.getOutputStream());

                JSONObject data = prepareRequestData(); //TODO Implement this function
                assert data != null;
                oswriter.write(data.toString());
                oswriter.flush();
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

                jsonResponse = new JSONObject(rawResponse);
                if(jsonResponse.has("error")){
                    result = LoginResult.INVALID_LOGIN;
                    is.close();
                    return null;
                }

                dataAvailable = true;

                orderTitles = handleJsonResponse(jsonResponse); //TODO Implement this function

                File cache = File.createTempFile("order_titles_"+ORDERS_TYPE, "json", getCacheDir());
                FileOutputStream fos = new FileOutputStream(cache);
                fos.write(jsonResponse.toString(4).getBytes());
                fos.flush();
                fos.close();
            }
            catch (IOException | JSONException e){
                e.printStackTrace();
            }
            finally {
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
            if(result == LoginResult.INVALID_LOGIN) {
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            } else if(result == LoginResult.NO_CONNECTION) {
                Toast.makeText(getApplicationContext(), "Відсутнє підключення до мережі", Toast.LENGTH_SHORT).show();
            }
            else orderListAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.listview, orderTitles);
        }
    }

    private String[] handleJsonResponse(JSONObject jsonResponse) {
        if(!dataAvailable){
            Toast.makeText(this, "Джерела даних недоступні", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private JSONObject prepareRequestData() {
        return null;
    }

    private String prepareIntentData(long id) {
        return null;
    }
}
