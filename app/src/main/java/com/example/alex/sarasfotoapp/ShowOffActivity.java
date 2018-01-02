package com.example.alex.sarasfotoapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.IntegerRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    private CharSequence code;
    private int pointer=0;
    private ImageButton left;
    private ProgressDialog progressDialog;
    private class LoadPictureTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] params) {
            final CountDownLatch countDownLatch= new CountDownLatch(1);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog= new ProgressDialog(ShowOffActivity.this);
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
                final Bitmap bitmap = loadBitmap(file, pointer);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                        if(progressDialog!=null)progressDialog.dismiss();
                    }
                });
            } catch (IOException e) {
                Toast.makeText(ShowOffActivity.this,"Image could not be loaded",Toast.LENGTH_LONG).show();
                Log.e("error","loding image",e);
            }
            return null;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_off);
        code=getString(R.string.code);
        imageView= (ImageView)findViewById(R.id.imageView);
        file= (File) getIntent().getSerializableExtra("open");
        left=(ImageButton)findViewById(R.id.left);
        if(!file.exists()){
            Toast.makeText(this,"File does not exist",Toast.LENGTH_LONG).show();
        }
        LoadPictureTask loadPictureTask= new LoadPictureTask();
        loadPictureTask.execute();
    }

    private Bitmap loadBitmap(File file, final int i) throws IOException {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                left.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
            }
        });

        File outputDir = this.getCacheDir(); // context being the Activity pointer

        BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(file));
        char[] chars = getString(R.string.code).toCharArray();
        Byte[] bytes=new Byte[chars.length];
        int in=0;
        for(char cha:chars){
            bytes[in++]=(byte) cha;
        }

        int count=0;
        File outputFile=null;
        long progress=0;
        progressDialog.setMax(fileInputStream.available());
        while(count<i+1){
            outputFile = File.createTempFile("temp_"+count+"_", ".png", outputDir);
            FileOutputStream fileOutputStream= new FileOutputStream(outputFile);
            while(true)
            {
                byte[] buffer=new byte[bytes.length];

                fileInputStream.mark(buffer.length+2);
                if(fileInputStream.read(buffer)<0)break;
                int counter;
                if(buffer[0]==bytes[0]&&Arrays.deepEquals(toContainer(buffer),bytes)){
                    progress*=1;
                    break;
                }else {

                    for (counter=1;counter!=buffer.length;counter++){
                        if(buffer[counter]==bytes[0]){
                            break;
                        }
                    }
                    fileOutputStream.write( Arrays.copyOfRange(buffer, 0, counter));
                }
                fileInputStream.reset();
                fileInputStream.skip(counter);
                progressDialog.setProgress((int) (progress+=counter));
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            count++;
        }
        fileInputStream.close();

        //TODO: save files to reuse
        Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
        if (bitmap==null&&pointer!=0){
            pointer=0;
            return loadBitmap(file,pointer);
        }
        return bitmap;
    }

    private Byte[] toContainer(byte[] buffer) {
        Byte[] bytes= new Byte[buffer.length];
        int i=0;
        for(byte b: buffer){
            bytes[i++]=b;
        }
        return bytes;
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (CodeActivty.password==null){
            Intent intent= new Intent(this,CodeActivty.class);
            intent.putExtra(CodeActivty.returnKey,true);
            startActivity(intent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!isFinishing())CodeActivty.password=null;
    }

    public void move(View view) {
        if(view.getId()==R.id.right)pointer++;
        else pointer--;
        LoadPictureTask loadPictureTask= new LoadPictureTask();
        loadPictureTask.execute();
    }


}
