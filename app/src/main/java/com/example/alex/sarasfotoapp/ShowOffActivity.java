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
    private CharSequence code;
    private int pointer=0;
    private ImageButton left;
    private ImageButton right;
    private ProgressDialog progressDialog;
    private ArrayList<Bitmap> restoredFiles = new ArrayList<>();
    private CountDownLatch bitMapLoading;
    private int pictureNumber = -1;

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
        if (pictureNumber > 0 && restoredFiles.size() - 1 < modTheOtherWay(i,pictureNumber)) {
            Toast.makeText(this, "Some pictures are not jet loaded!", Toast.LENGTH_LONG).show();
        }
        return restoredFiles.get(modTheOtherWay(i,restoredFiles.size()));
    }

    private int modTheOtherWay(int i, int n){
        i= i>=0?i%n:n-(Math.abs(i)%n)-1;
        return i;
    }

    private void loadBitmaps(File file) throws IOException {
        File outputDir = this.getCacheDir(); // context being the Activity pointer
        BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(file));
        char[] chars = getString(R.string.code).toCharArray();
        Byte[] bytes=new Byte[chars.length];
        int in=0;
        for(char cha:chars){
            bytes[in++]=(byte) cha;
        }

        int count=0;
        File outputFile;
        long progress=0;
        int preread[]= new int[2];
        boolean readSecondInt=false;

        if ((preread[0]=fileInputStream.read()) == 127) {
            readSecondInt=true;
            if ((preread[1] = fileInputStream.read()) > 0) {
                pictureNumber = preread[1];
            }
        }
        int bytesToLoad = pictureNumber > 0 ? fileInputStream.available() / pictureNumber : fileInputStream.available();
        progressDialog.setMax(bytesToLoad);
        Log.i("loading photo","Having"+bytesToLoad+" bytes to load");
        while (fileInputStream.available() > 1) {
            outputFile = File.createTempFile("temp_"+count+"_", ".png", outputDir);
            FileOutputStream fileOutputStream= new FileOutputStream(outputFile);
            if(count==0 && pictureNumber<0){
                fileOutputStream.write(preread[0]);
                if(readSecondInt){
                    fileOutputStream.write(preread[1]);
                }
            }
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
                    try {
                        ExifInterface exif = new ExifInterface(toutputFile.getAbsolutePath());
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                        Log.d("EXIF", "Exif: " + orientation);
                        Matrix matrix = new Matrix();
                        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                            matrix.postRotate(90);
                        }
                        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                            matrix.postRotate(180);
                        }
                        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                            matrix.postRotate(270);
                        }

                        Bitmap bitmap = BitmapFactory.decodeFile(toutputFile.getAbsolutePath());
                        restoredFiles.add(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true));
                    } catch (IOException e) {
                        Log.e("photo loading","exif failed",e);
                        e.printStackTrace();
                    }
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
                Toast.makeText(ShowOffActivity.this, "All pictures loaded!", Toast.LENGTH_LONG).show();
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
