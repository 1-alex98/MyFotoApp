package com.example.alex.sarasfotoapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import static android.view.View.GONE;

public class ShowOffActivity extends AppCompatActivity {

    private File file;
    private ImageView imageView;
    private CharSequence code;
    private int pointer=0;
    private ImageButton left;
    private ImageButton right;
    private ProgressDialog progressDialog;
    private ArrayList<Bitmap> restoredFiles = new ArrayList<>();
    private CountDownLatch bitMapLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_off);
        code=getString(R.string.code);
        imageView= (ImageView)findViewById(R.id.imageView);
        file= (File) getIntent().getSerializableExtra("open");
        left=(ImageButton)findViewById(R.id.left);
        right = (ImageButton) findViewById(R.id.right);
        if(!file.exists()){
            Toast.makeText(this,"File does not exist",Toast.LENGTH_LONG).show();
        }
        LoadPictureTask loadPictureTask= new LoadPictureTask();
        loadPictureTask.execute();
    }

    private Bitmap loadBitmap(final int i) {
        if (restoredFiles.size() == 0) return null;
        return restoredFiles.get(i % restoredFiles.size());
    }

    private void loadBitmaps(File file) throws IOException {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                left.setVisibility(GONE);
                right.setVisibility(GONE);
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
        while (fileInputStream.available() > 1) {
            outputFile = File.createTempFile("temp_"+count+"_", ".png", outputDir);
            FileOutputStream fileOutputStream= new FileOutputStream(outputFile);
            while(true)
            {
                byte[] buffer=new byte[bytes.length];

                fileInputStream.mark(buffer.length+2);
                if(fileInputStream.read(buffer)<0)break;
                int counter;
                if(buffer[0]==bytes[0]&&Arrays.deepEquals(toContainer(buffer),bytes)){
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
            final File toutputFile = outputFile;
            final int tcount = count;
            bitMapLoading = new CountDownLatch(1);
            Tools.Executor.execute(new Runnable() {
                @Override
                public void run() {
                    restoredFiles.add(BitmapFactory.decodeFile(toutputFile.getAbsolutePath()));
                    bitMapLoading.countDown();
                    if (tcount == 0) openFirstImage();
                }
            });
            count++;
        }
        fileInputStream.close();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    bitMapLoading.await();
                } catch (InterruptedException e) {
                    Log.e("error", "thread interrupted", e);
                }
                left.setVisibility(View.VISIBLE);
                right.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openFirstImage() {
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(loadBitmap(0));
            }
        });
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
    protected void onPause() {
        super.onPause();
        if(!isFinishing())CodeActivty.password=null;
    }

    public void move(View view) {
        if(view.getId()==R.id.right)pointer++;
        else pointer--;
        imageView.setImageBitmap(loadBitmap(pointer));
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
