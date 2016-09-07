package com.mkrooted.bukiclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class OrderDetailsActivity extends AppCompatActivity {
    JSONObject orderData;
    TextView orderTitleView, orderDescriptionView, orderTypeView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        try {
            orderData = new JSONObject(getIntent().getStringExtra("orderData"));
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Сталася помилка при опрацюванні даних", Toast.LENGTH_SHORT).show();
            finish();
        }

        orderTitleView = (TextView) findViewById(R.id.order_title);
        orderDescriptionView = (TextView) findViewById(R.id.order_description);
        orderTypeView = (TextView) findViewById(R.id.order_details_type);

        handleJsonData();
    }

    private void handleJsonData() {
        //TODO Fill textviews with data
    }
}
