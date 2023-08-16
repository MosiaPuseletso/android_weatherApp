package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private RecyclerView weatherRV;
    private TextInputEditText cityEdit;
    private ImageView backIV, iconIV, searchIV;
    private ArrayList<WeatherRVModel> weatherRVModelArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;
    private String cityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_main);

        homeRL = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        weatherRV = findViewById(R.id.idRvWeather);
        cityEdit = findViewById(R.id.idEditCity);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idIVSearch);

        weatherRVModelArrayList = new ArrayList<>();
        weatherRVAdapter =  new WeatherRVAdapter(this, weatherRVModelArrayList);
        weatherRV.setAdapter(weatherRVAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(location != null){
            cityName = getCityName(location.getLongitude(), location.getLatitude());
            getWeatherInfo(cityName);
        }

        searchIV.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String city = cityEdit.getText().toString();
                if(city.isEmpty()){
                    Toast.makeText(MainActivity.this, "Please enter city Name", Toast.LENGTH_SHORT).show();
                }else{
                    cityNameTV.setText(cityName);
                    getWeatherInfo(city);
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Please provide the permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void getWeatherInfo(String cityName){
        String url = "http://api.weatherapi.com/v1/forecast.json?key=db7d300411ce42d0a2f203909231408&q="+ cityName + "&days=1&aqi=yes&alerts=yes";
        cityNameTV.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModelArrayList.clear();

                try{
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature + "°c");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("http:".concat(conditionIcon)).into(iconIV);
                    conditionTV.setText(condition);

                    if(isDay == 1){
                        Picasso.get().load("data:image/jpeg;base64,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").into(backIV);
                    }else{
                        Picasso.get().load("data:image/jpeg;base64,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").into(backIV);
                    }

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecast = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecast.getJSONArray("hour");

                    for(int i = 0; i < hourArray.length(); i++){
                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");
                        weatherRVModelArrayList.add(new WeatherRVModel(time, temper, img, wind));
                    }
                    weatherRVAdapter.notifyDataSetChanged();
                }catch(JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Please enter valid city name", Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private String getCityName(double longitude, double latitude){
        String cityName = "Not found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());

        try{
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);

            for(Address adr: addresses){
                if(adr != null){
                    String city = adr.getLocality();
                    if(city != null && !city.equals("")){
                        cityName = city;
                    }else{
                        Log.d("TAG", "CITY NOT FOUND");
                        Toast.makeText(this, "User City Not Found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }

        return cityName;
    }
}