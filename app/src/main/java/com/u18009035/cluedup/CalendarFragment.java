package com.u18009035.cluedup;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.u18009035.cluedup.Discover.api;
import static com.u18009035.cluedup.Login.convertStreamToString;
import static com.u18009035.cluedup.Login.wheatleyPass;

public class CalendarFragment extends Fragment {

    public CalendarFragment() {
        // Required empty public constructor
    }

    protected View mView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    private AlertDialog LoaderDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        this.mView = view;

        CalendarView calendarView = mView.findViewById(R.id.calendar);
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {

            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {


                LayoutInflater factory = LayoutInflater.from(getActivity());
                final View movieInfoDialogView = factory.inflate(R.layout.loader_popup, null);
                LoaderDialog = new AlertDialog.Builder(getActivity()).create();
                LoaderDialog.getWindow().setDimAmount(0.7f);
                LoaderDialog.setView(movieInfoDialogView);
                LoaderDialog.show();

                CalendarGetDay(dayOfMonth, month, year);
            }
        });

        return view;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mView = null;
    }

    Runnable runningThread;

    public void CalendarGetDay(final Integer d, final Integer m, final Integer y) {
        Thread thread = new Thread(new Runnable() {@Override
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
                jsonParam.put("special_req", "calendar");
                jsonParam.put("return", "*");
                jsonParam.put("month", m.toString());
                jsonParam.put("year", y.toString());
                jsonParam.put("day", d.toString());
                jsonRequest.put("request", jsonParam);

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsonRequest.toString());

                os.flush();
                os.close();

                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG", conn.getResponseMessage());
                Log.i("REQUEST", jsonRequest.toString());

                final JSONObject jsonRes = new JSONObject(convertStreamToString(conn.getInputStream()));

                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            try {

                                boolean ResponseError = false;
                                if (jsonRes.has("total_results")) {
                                    if (jsonRes.getString("total_results").equals("0"))
                                        ResponseError = true;
                                    if (!jsonRes.has("results"))
                                        ResponseError = true;
                                    if (!jsonRes.has("total_results"))
                                        ResponseError = true;
                                    if (jsonRes.has("success"))
                                        if (jsonRes.get("success").equals("false"))
                                            ResponseError = true;
                                }

                                if(ResponseError){
                                    LoaderDialog.hide();
                                    Toast.makeText(getActivity(), "No results for this date", Toast.LENGTH_LONG).show();
                                } else {
                                    Log.i("RESPONSE", jsonRes.toString());
                                    Log.i("RESPONSE", jsonRes.toString());

                                    JSONArray results = new JSONArray(jsonRes.getString("results"));

                                    JSONObject movie = new JSONObject(results.get(0).toString());

                                    LayoutInflater factory = LayoutInflater.from(getActivity());
                                    final View movieInfoDialogView = factory.inflate(R.layout.calendar_popup, null);
                                    final AlertDialog movieInfoDialog = new AlertDialog.Builder(getActivity()).create();
                                    movieInfoDialog.getWindow().setDimAmount(0.7f);

                                    TextView title = movieInfoDialogView.findViewById(R.id.txtTitle);
                                    title.setText(movie.getString("title"));

                                    String Overview = movie.getString("overview");
                                    if (Overview.length() > 150) {
                                        Overview = Overview.substring(0, 150) + "...";
                                    }

                                    TextView desc = movieInfoDialogView.findViewById(R.id.txtDesc);
                                    desc.setText(Overview);

                                    ImageView posterPopupBanner = movieInfoDialogView.findViewById(R.id.imgPosterBanner);
                                    Picasso.get().load("https://image.tmdb.org/t/p/w500/" + movie.getString("poster_path")).fit().centerCrop().into(posterPopupBanner);

                                    movieInfoDialog.setView(movieInfoDialogView);

                                    LoaderDialog.hide();
                                    movieInfoDialog.show();
                                }

                            } catch (JSONException e) {
                                LoaderDialog.hide();
                                Toast.makeText(getActivity(), "No results for this date", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    conn.disconnect();
                    runningThread = null;
                } catch(Exception e){
                    runningThread = null;
                    e.printStackTrace();
                }
        }
        });

        if (runningThread == null) //ensure that only one API call is made at a time
        thread.start();
    }

}