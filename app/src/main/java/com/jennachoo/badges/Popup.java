package com.jennachoo.badges;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.io.File;
import java.io.InputStream;

/**
 * Created by jennachoo on 12/24/15.
 */
public class Popup extends Activity {
    int dimension;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int displayWidth = dm.widthPixels;
        int displayHeight = dm.heightPixels;

        //opens a popup window
        dimension = Math.min((int) (displayHeight * 0.9),(int) (displayWidth * 0.9));
        getWindow().setLayout(dimension,dimension);

        String name =   getIntent().getStringExtra("name");
        String[] info = getIntent().getStringArrayExtra("info");
        String category = getIntent().getStringExtra("category");
        String categoryIcon = getIntent().getStringExtra("categoryIcon");
        String description = info[0];
        String points = info[1] + " points";
        String icon= info[2];


        //sets up display for the pop up window
        int newWidth = (int) (dimension* 0.6);
        String path = icon;
        setImage(R.id.icon_image,path,newWidth);

        int newCatWidth = (int) (dimension * 0.1);
        String path2 = categoryIcon;
        setImage(R.id.category_img, path2, newCatWidth);

        TextView name_txt = (TextView) findViewById(R.id.name_txt);
        name_txt.setText(name.toCharArray(),0,name.length());

        TextView category_txt = (TextView) findViewById(R.id.category_txt);
        category_txt.setText(category.toCharArray(),0,category.length());

        TextView points_txt = (TextView) findViewById(R.id.points_txt);
        points_txt.setText(points.toCharArray(),0,points.length());

        TextView description_txt = (TextView) findViewById(R.id.description_txt);
        description_txt.setText(description.toCharArray(),0,description.length());

        Button button = (Button) findViewById(R.id.close_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    //gets image at path(url), resizes it to newWidth and saves it to imageView at imageID
    public void setImage(int imageID, String path, int newWidth){
        ImageView image = (ImageView) findViewById(imageID);
        Picasso.with(getApplicationContext()).load(path).resize(newWidth,newWidth).into(image);
    }

}
