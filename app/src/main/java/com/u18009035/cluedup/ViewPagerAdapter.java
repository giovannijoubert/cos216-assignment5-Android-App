package com.u18009035.cluedup;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
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

public class ViewPagerAdapter extends PagerAdapter {
    private Context context;
    private JSONArray movieArray;

    ViewPagerAdapter(Context context, JSONArray movieArray) {
        this.context = context;
        this.movieArray = movieArray;
    }

    @Override
    public int getCount() {
        return movieArray.length();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull@Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        final ImageView poster = new ImageView(context);

        try {
            //Interpret movie JSON
            final JSONObject singleMovie = new JSONObject(movieArray.get(position).toString());

            final String posterUrl = singleMovie.getString("poster");
            final String fanartUrl = singleMovie.getString("fanart");
            final String movieTitle = singleMovie.getString("title");
            final String movieDesc = singleMovie.getString("synopsis");
            final String movieAge = singleMovie.getString("ageRating");
            final String movieImdbRating = singleMovie.getString("imdbRating");
            final String moveiCluedRating = singleMovie.getString("cluedRating");

            poster.setForeground(ContextCompat.getDrawable(context, R.drawable.movie_poster));
            Picasso.get().load(posterUrl).fit().centerCrop().into(poster);
            container.addView(poster);

            poster.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    LayoutInflater factory = LayoutInflater.from(context);
                    final View movieInfoDialogView = factory.inflate(R.layout.poster_popup, null);
                    final AlertDialog movieInfoDialog = new AlertDialog.Builder(context).create();
                    movieInfoDialog.getWindow().setDimAmount(0.7f);

                    //Get popup text fields
                    TextView title = movieInfoDialogView.findViewById(R.id.txtTitle);
                    TextView desc = movieInfoDialogView.findViewById(R.id.txtDesc);
                    TextView age = movieInfoDialogView.findViewById(R.id.txtAge);
                    TextView imdb = movieInfoDialogView.findViewById(R.id.txtIMDB);
                    TextView clued = movieInfoDialogView.findViewById(R.id.txtCLUED);

                    //Set popup text
                    title.setText(movieTitle);
                    desc.setText(movieDesc);
                    age.setText(movieAge);
                    imdb.setText(movieImdbRating + " IMDB");
                    clued.setText(moveiCluedRating + " CluedUP");

                    ImageView posterPopupBanner = movieInfoDialogView.findViewById(R.id.imgPosterBanner);
                    Picasso.get().load(fanartUrl).fit().centerCrop().into(posterPopupBanner);

                    SeekBar seekRater = movieInfoDialogView.findViewById(R.id.seekRating2);
                    seekRater.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            try{
                                String imdbID = singleMovie.getString("imdbid");
                                SubmitRating(seekBar.getProgress(), imdbID);
                                Toast.makeText(context, "Rating Submitted", Toast.LENGTH_LONG).show();
                            } catch(JSONException e){}
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {

                        }
                    });

                    movieInfoDialog.setView(movieInfoDialogView);

                    movieInfoDialog.show();
                }
            });

            return poster;

        } catch(JSONException e) {
            Toast.makeText(context, "Something went wrong", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    Runnable runningThread = null;
    private void SubmitRating(final Integer Rating, final String imdbID){
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

                    Integer rate = (Integer)Rating;
                    rate = rate*10;
                    String ratingSubmit = rate.toString();



                    //Create JSON request
                    JSONObject jsonRequest = new JSONObject();
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("type", "rate");
                    jsonParam.put("key", api);
                    jsonParam.put("imdbid", imdbID);
                    jsonParam.put("uRating", ratingSubmit);
                    jsonRequest.put("request", jsonParam);

                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(jsonRequest.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG", conn.getResponseMessage());

                    JSONObject jsonRes = new JSONObject(convertStreamToString(conn.getInputStream()));

                    Log.i("MOVIE", jsonRes.toString());



                    conn.disconnect();
                    runningThread = null;
                } catch(Exception e) {
                    e.printStackTrace();
                    runningThread = null;
                }
            }
            });
            if(runningThread == null)
            thread.start();

    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}