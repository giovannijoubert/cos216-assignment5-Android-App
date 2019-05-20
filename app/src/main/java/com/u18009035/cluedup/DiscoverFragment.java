package com.u18009035.cluedup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Movie;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static com.u18009035.cluedup.Discover.api;
import static com.u18009035.cluedup.Login.convertStreamToString;
import static com.u18009035.cluedup.Login.wheatleyPass;

public class DiscoverFragment extends Fragment {

    private JSONArray MoviesShown;
    protected View dView;

    public DiscoverFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        //Restart shake sensor listener
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        //Stop shake sensor listener (switching to another activity/fragment)
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
    }

    //Shake variables
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Shake mShakeDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Shake Init
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new Shake();
        mShakeDetector.setOnShakeListener(new Shake.OnShakeListener() {

            @Override
            public void onShake(int count) {
                ViewPager viewPager = getView().findViewById(R.id.posterHolder);
                fadeOutViewPager(viewPager);
                DiscoverGetContent("*");
            }
        });

        if (getArguments() != null) {
            DiscoverGetContent(getArguments().getString("title", "*"));
        } else {
            DiscoverGetContent("*");
        }
    }

    //Helper function to fade out the view pager (for replacement animation)
    private void fadeOutViewPager(final ViewPager VP) {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(1000);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                VP.setVisibility(View.GONE);
            }
            public void onAnimationRepeat(Animation animation) {
            }
            public void onAnimationStart(Animation animation) {
            }
        });
        VP.startAnimation(fadeOut);
    }

    //Helper function to fade in the view pager (for replacement animation)
    private void fadeInViewPager(final ViewPager VP) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(1000);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                VP.setVisibility(View.VISIBLE);
            }
            public void onAnimationRepeat(Animation animation) {
            }
            public void onAnimationStart(Animation animation) {
            }
        });
        VP.startAnimation(fadeIn);
    }


    Runnable runningThread;
    //PHP API call to DISCOVER
    public void DiscoverGetContent(final String title) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runningThread = this;
                    URL url = new URL("http://@wheatley.cs.up.ac.za/u18009035/api.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    //Authenticate wheatley
                    String basicAuth = "Basic " + wheatleyPass;
                    conn.setRequestProperty("Authorization", basicAuth);

                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    //Create JSON request
                    JSONObject jsonRequest = new JSONObject();
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("key", api);
                    jsonParam.put("type", "info");
                    jsonParam.put("title", title);
                    jsonParam.put("return", "*");
                    jsonRequest.put("request", jsonParam);

                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(jsonRequest.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG", conn.getResponseMessage());


                    JSONObject jsonRes = new JSONObject(convertStreamToString(conn.getInputStream()));

                    String status = jsonRes.getString("status");

                    if (status == "unsuccessful") {
                        getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getActivity().getApplicationContext(), "Something went wrong", Toast.LENGTH_LONG).show();
                            }
                        });
                        return;
                    }

                    ArrayList<String> list = new ArrayList<String>();
                    final JSONArray movieArray = new JSONArray(jsonRes.getString("data"));
                    MoviesShown = movieArray;
                    if (movieArray != null) {
                        int len = movieArray.length();
                        for (int i = 0; i < len; i++) {
                            list.add(movieArray.get(i).toString());
                        }
                    }

                    if(getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            //Handle weird search queries
                            if (movieArray.length() == 0 && getArguments() != null) {
                                Toast.makeText(getActivity().getApplicationContext(), "No results, try a different search",
                                        Toast.LENGTH_LONG).show();
                                DiscoverGetContent("*");
                            }

                            ViewPager viewPager = getView().findViewById(R.id.posterHolder);
                            ViewPagerAdapter adapter = new ViewPagerAdapter(getActivity(), movieArray);
                            viewPager.setAdapter(adapter);
                            fadeInViewPager(viewPager);
                        }
                    });

                    conn.disconnect();
                    runningThread = null;
                } catch (Exception e) {
                    e.printStackTrace();
                    runningThread = null;
                }
            }
        });

        if (runningThread == null) //ensure that only one API call is made at a time
        thread.start();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment


        View view = inflater.inflate(R.layout.fragment_discover, container, false);
        this.dView = view;

        final FloatingActionButton filter = dView.findViewById(R.id.btnFilter);
        filter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LayoutInflater factory = LayoutInflater.from(getContext());
                final View filterView = factory.inflate(R.layout.filter_popup, null);
                final AlertDialog FilterDialog = new AlertDialog.Builder(getContext()).create();

                final Spinner spinner = filterView.findViewById(R.id.spinGenre);
                final SeekBar seekRating = filterView.findViewById(R.id.seekRating);

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                        R.array.genres, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);

                Button filter = filterView.findViewById(R.id.btnFilter);
                filter.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        int spinner_pos = spinner.getSelectedItemPosition();
                        String[] genre_values = getResources().getStringArray(R.array.genre_values);
                        final Integer gNo = Integer.valueOf(genre_values[spinner_pos]);
                        RunFilter(seekRating.getProgress(), gNo);
                        FilterDialog.hide();
                    }
                });

                FilterDialog.getWindow().setDimAmount(0.7f);
                FilterDialog.setView(filterView);
                FilterDialog.show();

            }
        });

        return dView;
    }

    private void RunFilter(Integer Rating, Integer Gno){
        if(MoviesShown != null){

            for(int i = 0; i < MoviesShown.length(); i++){
                try {
                    JSONObject Movie = new JSONObject(MoviesShown.get(i).toString());

                    if(Movie.getString("imdbRating").equals("N/A")){
                        MoviesShown.remove(i);
                        i--;
                    } else
                    if(Movie.getDouble("imdbRating") < Rating-1){
                        MoviesShown.remove(i);
                        i--;
                    } else {
                        JSONArray Genres = Movie.getJSONArray("genres");
                        boolean flag = false;
                        for (int j = 0; j < Genres.length(); j++) {
                            if (Genres.get(j).equals(Gno)){
                                flag = true;
                            }
                        }
                        if(!flag) {
                            MoviesShown.remove(i);
                            i--;
                        }
                    }

                } catch(JSONException e){}
            }

            if(MoviesShown.length() == 0){
                Toast.makeText(getActivity().getApplicationContext(), "None of the currently displayed movies match", Toast.LENGTH_LONG).show();
            } else {
                ViewPager viewPager = getView().findViewById(R.id.posterHolder);
                ViewPagerAdapter adapter = new ViewPagerAdapter(getActivity(), MoviesShown);
                viewPager.setAdapter(adapter);
                fadeInViewPager(viewPager);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        dView = null;
    }

}
