package com.jennachoo.badges;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import static android.view.MotionEvent.ACTION_CANCEL;

public class MainActivity extends AppCompatActivity {

    final String LOG_TAG = "KABADGE";

    Hashtable<String, String[]> badgeSet;
    Hashtable<Integer, String[]> categorySet;
    int displayWidth;
    int displayHeight;
    int num_cols;
    boolean badgeNeedsUpdate;
    boolean categoryNeedsUpdate;
    boolean vertical;
    boolean redraw;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d("JENNA", "onCreate");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        displayWidth = dm.widthPixels;
        displayHeight = dm.heightPixels;


        if (redraw && badgeSet != null && categorySet != null) {
            Log.d("JENNA", "here");
            display();
        }

        badgeNeedsUpdate = false;
        categoryNeedsUpdate = false;
        FetchCategoriesTask task2 = new FetchCategoriesTask();
        task2.execute();
        FetchBadgesTask task = new FetchBadgesTask();
        task.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //you can manually refresh for updates
        if (id == R.id.action_refresh){
            FetchCategoriesTask task2 = new FetchCategoriesTask();
            task2.execute();
            FetchBadgesTask task = new FetchBadgesTask();
            task.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //opens the pop up window for detailed info
    public void pop(String name){
        String[] line = badgeSet.get(name);
        int cat = Integer.parseInt(line[3]);
        String catName = categorySet.get(cat)[0];
        String catIcon = categorySet.get(cat)[2];

        Intent i = new Intent(MainActivity.this, Popup.class);
        i.putExtra("name", name);
        i.putExtra("info", line);
        i.putExtra("category", catName);
        i.putExtra("categoryIcon", catIcon);
        startActivity(i);
    }


    //sets up the display
    public void display(){
        Log.d(LOG_TAG, "display");
        redraw = false;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270){
            num_cols = 7;
        } else  {
            num_cols = 5;
        }
        TableLayout table = (TableLayout) findViewById(R.id.layout);
        table.removeAllViews();
        ArrayList<String> keySet = new ArrayList<String>();
        keySet.addAll(badgeSet.keySet());
        Log.d(LOG_TAG,"key set size" + keySet.size());
        for (int r = 0; r <= (keySet.size()/num_cols); r++){
            TableRow row = new TableRow(getApplicationContext());
            row.setGravity(Gravity.CENTER_HORIZONTAL);
            for (int c = 0; c < num_cols; c++){
                int index = num_cols * r + c;
                if (index < keySet.size()) {
                    ImageView image = new ImageView(getApplicationContext());
                    final String name = keySet.get(index);
                    String[] line = badgeSet.get(name);
                    String url = line[2];
                    Log.d(LOG_TAG, url);
                    int newWidth = (int) ((displayWidth / num_cols) * 0.8);
                    Picasso.with(getApplicationContext()).load(url).resize(newWidth, newWidth).into(image);
                    image.setPadding(7, 7, 7, 7);
                    image.setBackgroundColor(Color.GRAY);
                    image.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                                case (MotionEvent.ACTION_DOWN): {
                                    v.setBackgroundColor(Color.LTGRAY);
                                    break;
                                }
                                case (MotionEvent.ACTION_UP): {
                                    v.setBackgroundColor(Color.GRAY);
                                    pop(name);
                                    break;
                                }
                                //scroll up
                                case (MotionEvent.ACTION_CANCEL): {
                                    v.setBackgroundColor(Color.GRAY);
                                    break;
                                }
                            }
                            return true;
                        }
                    });
                    row.addView(image);
                }
            }
            table.addView(row);
        }
        Log.d(LOG_TAG, "display done");
    }

    //parse badgeJsonStr and saves the data in badgeSet
    private Hashtable<String,String[]> getBadgesDataFromJson(String badgesJsonStr)
            throws JSONException {

        final String KA_DESCRIPTION = "description";
        final String KA_BADGE_CATEGORY = "badge_category";
        final String KA_SAFE_EXTENDED_DESCRIPTION = "safe_extended_description";
        final String KA_POINTS = "points";
        final String KA_ICONS = "icons";
        final String KA_NAME = "name";


        HashSet<String> badges = new HashSet<String>();
        JSONArray badgeArray = new JSONArray(badgesJsonStr);
        Hashtable<String,String[]> badgeSet = new Hashtable<String,String[]> ();

        for (int i = 0; i < badgeArray.length(); i++) {
            JSONObject badgeObj = badgeArray.getJSONObject(i);
            int category = badgeObj.getInt(KA_BADGE_CATEGORY);
            String description = badgeObj.getString(KA_DESCRIPTION);
            String extended_description = badgeObj.getString(KA_SAFE_EXTENDED_DESCRIPTION);
            String points = badgeObj.getString(KA_POINTS);
            JSONObject icons = badgeObj.getJSONObject(KA_ICONS);
            String icon_large = icons.getString("large");
            String name = badgeObj.getString(KA_NAME);
            badgeSet.put(description, new String[] {extended_description,points,icon_large,Integer.toString(category),name});
            badges.add(description);
        }
        return badgeSet;
    }

    //parse categoryJsonStr and saves the data in categorySet
    private Hashtable<Integer,String[]> getCategoriesDataFromJson(String categoriesJsonStr)
            throws JSONException {

        final String KA_CATEGORY = "category";
        final String KA_TYPE_LABEL = "type_label";
        final String KA_TRANSLATED_DESCRIPTION = "translated_description";
        final String KA_ICON_SRC = "icon_src";

        JSONArray categoryArray = new JSONArray(categoriesJsonStr);
        Hashtable<Integer,String[]> categorySet = new Hashtable<Integer,String[]>(categoryArray.length());

        for (int i = 0; i < categoryArray.length(); i++) {
            JSONObject categoryObj = categoryArray.getJSONObject(i);
            int category = categoryObj.getInt(KA_CATEGORY);
            String label = categoryObj.getString(KA_TYPE_LABEL);
            String description = categoryObj.getString(KA_TRANSLATED_DESCRIPTION);
            String icon_src = categoryObj.getString(KA_ICON_SRC);
            categorySet.put(category, new String[]{label, description, icon_src});
        }

        return categorySet;
    }

    //fetches badge information from http://www.khanacademy.org/api/v1/badges
    //saves the Json string to badgeJsonStr and parses it into badgeSet
    public class FetchBadgesTask extends AsyncTask<Void, Void, Void> {

        final String TAG = "FetchBadgesTask";

        @Override
        protected Void doInBackground(Void... params) {

            String badgeJsonStr = null;

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL("http://www.khanacademy.org/api/v1/badges");

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                String tempBadgeJsonStr = buffer.toString();
                if (badgeJsonStr == null || !badgeJsonStr.equals(tempBadgeJsonStr)) {
                    badgeJsonStr = tempBadgeJsonStr;
                    try{
                        badgeSet = getBadgesDataFromJson(badgeJsonStr);
                    } catch (JSONException e){
                        Log.e(TAG, e.getMessage(),e);
                        e.printStackTrace();
                    }
                    badgeNeedsUpdate = true;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error ", e);
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "Error closing stream", e);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (badgeNeedsUpdate) {
                Log.d(TAG, "badge needs update");
                display();
            }
        }

    }

    //fetches category information from http://www.khanacademy.org/api/v1/badges/categories
    //saves it to categoryJsonStr and parse the json string into categorySet
    public class FetchCategoriesTask extends AsyncTask<Void, Void, Void> {

        final String TAG = "FetchCategoriesTask";

        @Override
        protected Void doInBackground(Void... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String categoryJsonStr = null;


            try {
                URL url = new URL("http://www.khanacademy.org/api/v1/badges/categories");

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }

                String tempCategoryJsonStr = buffer.toString();
                if (categoryJsonStr == null || !categoryJsonStr.equals(tempCategoryJsonStr)) {
                    categoryJsonStr = tempCategoryJsonStr;
                    try{
                        categorySet = getCategoriesDataFromJson(categoryJsonStr);
                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                        e.printStackTrace();
                    }
                    categoryNeedsUpdate = true;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error ", e);
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "Error closing stream", e);
                    }
                }
            }
            return null;
        }

    }

}
