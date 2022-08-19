package com.dge.bustimings;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class TimingActivity extends AppCompatActivity {

    RequestQueue queue;
    String url;
    StringRequest request;
    ListView list;
    ArrayAdapter<String> adapter;
    ArrayList<String> buses;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timing);

        TextView name = findViewById(R.id.stopName);
        TextView direction = findViewById(R.id.textView2);

        url = "https://api.tfl.gov.uk/StopPoint/" + getIntent().getStringExtra("id") + "/arrivals";

        request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONArray respo = null;
                HashMap<String, ArrayList<Integer>> busInfo = new HashMap<String, ArrayList<Integer>>();

                try {
                    respo = new JSONArray(response);

                    for (int i=0; i<respo.length(); i++) {
                        JSONObject bus = respo.getJSONObject(i);
                        String busNumber = bus.getString("lineName");
                        if (i == 1) {
                            name.setText(bus.getString("stationName"));
                            direction.setText("Towards: " + bus.getString("towards"));
                        }
                        Integer timeToBus = Integer.parseInt(bus.getString("timeToStation")) / 60;
                        if (!busInfo.containsKey(busNumber)) {
                            busInfo.put(busNumber, new ArrayList<>());
                        }
                        busInfo.get(busNumber).add(timeToBus);
                    }

                } catch (JSONException e) {
                    name.setText("ERROR");
                    direction.setText("Please check connection");
                    e.printStackTrace();
                }

                buses = new ArrayList<String>();

                for (String busNumber : busInfo.keySet()) {
                    ArrayList<String> writtenTimes = new ArrayList<>();

                    Collections.sort(busInfo.get(busNumber));

                    for (Integer timeToBus : busInfo.get(busNumber)) {
                        String contents;
                        if (timeToBus == 0) {
                            contents = "due";
                        } else if (timeToBus == 1) {
                            contents = "1 min";
                        } else {
                            contents = Integer.toString(timeToBus) + " mins";
                        }

                        writtenTimes.add(contents);
                    }

                    buses.add(busNumber + " - " + String.join(", ", writtenTimes));
                }

                list = findViewById(R.id.busList);
                adapter = new ArrayAdapter<String>(TimingActivity.this, android.R.layout.simple_list_item_1, buses);
                list.setAdapter(adapter);
                list.requestLayout();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                name.setText("ERROR");
                direction.setText("Please check connection");
            }
        });

        queue = Volley.newRequestQueue(TimingActivity.this);
        queue.add(request);

    }
}