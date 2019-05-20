package com.u18009035.cluedup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import static com.u18009035.cluedup.Login.PREFS_NAME;

public class Discover extends AppCompatActivity {

    private static final String PREF_API = "api";

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_discover:
                    //Switch to Discover Fragment
                    FragmentTransaction ftd = getSupportFragmentManager().beginTransaction();
                    ftd.replace(R.id.fragment_frame, new DiscoverFragment());
                    ftd.commit();

                    return true;
                case R.id.navigation_calendar:
                    //Switch to Calendar Fragment
                    FragmentTransaction ftc = getSupportFragmentManager().beginTransaction();
                    ftc.replace(R.id.fragment_frame, new CalendarFragment());
                    ftc.commit();

                    return true;
                case R.id.navigation_logout:
                    //Set session API = null
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_API, null).commit();
                    finish(); //Restart app
                    return true;
            }
            return false;
        }
    };

    @Override
    public void onBackPressed() {
        //go exit on back button press (instead of login page)
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    public static String api = "api";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Force Portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_discover);

        //Retrieve saved API Key
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        api = pref.getString(PREF_API, null);
        BottomNavigationView navView = findViewById(R.id.nav_view);

        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        final ImageView search = findViewById(R.id.imgSearch);
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BottomNavigationView bottomNavigationView;
                bottomNavigationView = findViewById(R.id.nav_view);
                bottomNavigationView.setSelectedItemId(R.id.navigation_discover);

                EditText searchBox = findViewById(R.id.txtSearch);
                String searchQry = searchBox.getText().toString();

                //Close soft keyboard when search is initiated
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);

                if (searchQry.equals("")) {
                    Toast.makeText(getApplicationContext(), "You need to enter a search query", Toast.LENGTH_LONG).show();
                } else {

                    Bundle bundle = new Bundle();
                    bundle.putString("title", searchQry);
                    DiscoverFragment fragInfo = new DiscoverFragment();
                    fragInfo.setArguments(bundle);

                    //Replace fragment with new, search data
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ft.replace(R.id.fragment_frame, fragInfo);
                    ft.commit();

                }
            }
        });

        //Initial load of Discover Fragment
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_frame, new DiscoverFragment());
        ft.commit();
    }

}