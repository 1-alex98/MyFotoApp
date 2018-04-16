package com.example.alex.sarasfotoapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class ShowOffActivity extends AppCompatActivity {

    private File file;
    private ImageView imageView;
    private int pointer=1;
    private ProgressDialog progressDialog;
    private ArrayList<Bitmap> restoredFiles = new ArrayList<>();
    private int pictureNumber = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_off);
        imageView= (ImageView)findViewById(R.id.imageView);
        file= (File) getIntent().getSerializableExtra("open");
        if(!file.exists()){
            Toast.makeText(this,"File does not exist",Toast.LENGTH_LONG).show();
        }
        LoadPictureTask loadPictureTask= new LoadPictureTask();
        loadPictureTask.execute();
    }

    private Bitmap loadBitmap() {
        if(pointer>restoredFiles.size()){

            if(restoredFiles.size()<pictureNumber){
                Toast.makeText(this,"Not all files are loaded yet",Toast.LENGTH_LONG).show();
            }
            pointer=1;

        }else if(pointer<1) {
            if(restoredFiles.size()<pictureNumber){
                Toast.makeText(this,"Not all files are loaded yet",Toast.LENGTH_LONG).show();
            }
            pointer=restoredFiles.size();
        }
        return restoredFiles.get(pointer -1);
    }


    private void loadBitmaps(File file) throws IOException {
       BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(file));
       pictureNumber=IOUtils.readInt(fileInputStream);
       ArrayList<Integer> sizes= new ArrayList<>();
       for (int i=0;i<pictureNumber;i++){
           sizes.add(IOUtils.readInt(fileInputStream));
       }
       for(int length:sizes){
           byte[] data= new byte[length];
           fileInputStream.read(data);
           Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, length);
           restoredFiles.add(bitmap);
           runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   if(progressDialog!=null && progressDialog.isShowing()){
                       progressDialog.dismiss();
                       imageView.setImageBitmap(loadBitmap());
                   }
               }
           });
       }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (CodeActivty.password==null){
            Intent intent= new Intent(this,CodeActivty.class);
            intent.putExtra(CodeActivty.EXTRA_NOT_STARTUP, true);
            intent.putExtra(CodeActivty.returnKey,true);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(!isFinishing())CodeActivty.password=null;
    }

    public void move(View view) {
        if(view.getId()==R.id.right)pointer++;
        else pointer--;
        imageView.setImageBitmap(loadBitmap());
    }

    private class LoadPictureTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog = new ProgressDialog(ShowOffActivity.this);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setTitle("Please wait");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                    countDownLatch.countDown();
                }
            });
            try {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                loadBitmaps(file);
            } catch (IOException e) {
                Toast.makeText(ShowOffActivity.this, "Image could not be loaded", Toast.LENGTH_LONG).show();
                Log.e("error", "loding image", e);
            }
            return null;
        }
    }


}
