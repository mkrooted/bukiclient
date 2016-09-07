package com.mkrooted.bukiclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.IntegerRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.net.MalformedURLException;
import java.net.URL;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{
    public static final String TAG = "BukiClient1";
    EditText emailInput, passwordInput;
    Button loginBtn;
    ConnectivityManager connManager;
    NetworkInfo networkInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.login_btn:
                new LoginTask().execute();
                loginBtn.setText("Вхід...");
                break;
        }
    }

    private void init(){
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo==null || !networkInfo.isConnected()){
            showNetworkAlert("Увага");
        }

        emailInput = (EditText) findViewById(R.id.email_input);
        passwordInput = (EditText) findViewById(R.id.password_input);
        loginBtn = (Button) findViewById(R.id.login_btn);
        loginBtn.setOnClickListener(this);
    }

    private void showNetworkAlert(String errorMsg){
        Toast.makeText(this, errorMsg+" - відсутнє підключення до мережі", Toast.LENGTH_SHORT).show();
    }
    public void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private class LoginTask extends AsyncTask<String, Void, String> {
        Integer result = null;

        @Override
        protected String doInBackground(String... params) {
            result = login();
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            loginBtn.setText("Увійти");

            if (result == LoginResult.SUCCESS) {
                showToast("Вхід успішний");
                finish();
            } else if(result == LoginResult.INVALID_INPUT) {
                showToast("Уведіть email та пароль");
            } else if(result == LoginResult.HTTP_ERROR) {
                showToast("Помилка - спробуйте пізніше");
            } else if(result == LoginResult.INVALID_LOGIN) {
                showToast("Неправильне ім'я користувача чи пароль");
            } else if(result == LoginResult.OTHER_DICH) {
                showToast("Якась інша дичина");
            } else if(result == LoginResult.NO_CONNECTION) {
                showToast("Відсутнє підключення до мережі");
            }
        }
    }

    private Integer login(){
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo==null || !networkInfo.isConnected()){
            return LoginResult.NO_CONNECTION;
        }


        if (emailInput.getText().toString().length()<=0 || passwordInput.getText().toString().length()<=0){
            return LoginResult.INVALID_INPUT;
        }

        InputStream is = null;
        try{
            URL url = new URL("http://api.buki.com.ua/login");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            CookieManager cookieManager = new CookieManager();
            String COOKIE_HEADER = "Set-Cookie";

            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            JSONObject data = new JSONObject();
            data.put("email", emailInput.getText().toString());
            data.put("password", passwordInput.getText().toString());


            connection.setDoOutput(true);
            connection.setDoInput(true);
            OutputStreamWriter oswriter = new OutputStreamWriter(connection.getOutputStream());
            oswriter.write(data.toString());
            oswriter.flush();
            connection.connect();
            int responseCode = connection.getResponseCode();
            if(responseCode != HttpURLConnection.HTTP_OK){
                Log.e("BukiClient", "RESPONSE CODE = "+responseCode);
                return LoginResult.HTTP_ERROR;
            }

            is = connection.getInputStream();
            Reader reader = new InputStreamReader(is, "UTF-8");
            char[] buffer = new char[500];
            reader.read(buffer);
            String rawResponse = new String(buffer);
            Log.d("BukiClient", "RawResponse: \n"+rawResponse);

            JSONObject response = new JSONObject(rawResponse);
            if ( response.has("error") ){
                Log.d("BukiClient", "Can't login");
                is.close();
                return LoginResult.INVALID_LOGIN;
            }
            response = response.getJSONObject("response");

            String rawCookies = connection.getHeaderField(COOKIE_HEADER);
            Log.d(TAG, "rawCookies: "+rawCookies);

            SharedPreferences preferences = getSharedPreferences(TAG, 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("cookie", rawCookies);
            editor.putString("name", response.get("users_sname")+" "+response.get("users_name")+" "+response.get("users_fname"));
            editor.commit();
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
        return LoginResult.SUCCESS;
    }
}
