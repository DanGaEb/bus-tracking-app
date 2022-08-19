package com.dge.bustimings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    ListView list;// = findViewById(R.id.buses);
    ArrayAdapter<String> adapter;
    ArrayList<String> names = new ArrayList<String>();
    ArrayList<String> identification = new ArrayList<String>();
    String url, latitude, longitude;
    RequestQueue queue;
    String postcode;
    StringRequest request;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText locationBox = findViewById(R.id.location);
                postcode = "https://api.postcodes.io/postcodes/" + locationBox.getText().toString();

                StringRequest postcodeReq = new StringRequest(Request.Method.GET, postcode, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JSONObject respo = null;
                        try {
                            respo = new JSONObject(response);
                            JSONObject arr = respo.getJSONObject("result");
                            longitude = arr.getString("longitude");
                            latitude = arr.getString("latitude");
                            url = "https://api.tfl.gov.uk/StopPoint?stopTypes=NaptanPublicBusCoachTram&lat=" + latitude + "&lon=" + longitude + "&radius=500";

                            request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    JSONObject respo = null;
                                    try {
                                        respo = new JSONObject(response);
                                        JSONArray arr = respo.getJSONArray("stopPoints");
                                        names.clear();
                                        identification.clear();
                                        for (int i = 0; i < arr.length(); i++) {
                                            String id;
                                            StringBuilder busStop = new StringBuilder();
                                            try {
                                                busStop.append("(" + arr.getJSONObject(i).getString("stopLetter") + ") ");
                                            } catch (Exception e) {
                                                busStop.append("(?) ");
                                            }
                                            busStop.append(arr.getJSONObject(i).getString("commonName"));

                                            try {
                                                id = arr.getJSONObject(i).getString("naptanId");
                                            } catch (JSONException e) {
                                                id = "-1";
                                            }

                                            JSONArray arr2 = arr.getJSONObject(i).getJSONArray("lines");
                                            for (int j = 0; j < arr2.length(); j++) {
                                                busStop.append(" " + arr2.getJSONObject(j).getString("name") + " ");
                                            }

                                            try {
                                                busStop.append("\nTowards: " + arr.getJSONObject(i).getJSONArray("additionalProperties").getJSONObject(1).getString("value"));
                                            } catch (JSONException e) {
                                                busStop.append("\nDestination Unavailable");
                                            }

                                            names.add(busStop.toString());
                                            identification.add(id);
                                        }
                                        list = findViewById(R.id.buses);
                                        adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, names);
                                        list.setAdapter(adapter);
                                        list.requestLayout();

                                        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                                Intent intent = new Intent(MainActivity.this, TimingActivity.class);

                                                intent.putExtra("id", identification.get(i));

                                                startActivity(intent);
                                            }
                                        });
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                }
                            }
                            );

                            queue.add(request);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });


                queue = Volley.newRequestQueue(MainActivity.this);
                queue.add(postcodeReq);



            }
        });

        findViewById(R.id.locate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }

                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    FusedLocationProviderClient mFusedLocationLocationClient;
                    mFusedLocationLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
                    mFusedLocationLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            Location location = task.getResult();
                            if (location == null) {
                                Toast.makeText(MainActivity.this, "Location not working", Toast.LENGTH_LONG).show();
                            } else {
                                StringRequest latReq = new StringRequest(Request.Method.GET, "https://api.postcodes.io/postcodes?lon=" + location.getLongitude() + "&lat=" + location.getLatitude(), new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            JSONObject latResponse = new JSONObject(response);
                                                if (latResponse.getString("result").equals("null")) {
                                                    Toast.makeText(MainActivity.this, "Location error", Toast.LENGTH_LONG).show();
                                                } else {
                                                    ((TextView) findViewById(R.id.location)).setText(latResponse.getJSONArray("result").getJSONObject(0).getString("postcode"));
                                                }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Toast.makeText(MainActivity.this, "Location not working", Toast.LENGTH_LONG).show();
                                    }
                                });

                                RequestQueue queue2 = Volley.newRequestQueue(MainActivity.this);
                                queue2.add(latReq);
                            }
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Please allow location permission", Toast.LENGTH_LONG).show();
                }


            }
        });



    }
}