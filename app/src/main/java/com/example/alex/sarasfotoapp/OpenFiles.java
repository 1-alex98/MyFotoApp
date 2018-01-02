package com.example.alex.sarasfotoapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import static android.R.attr.action;

public class OpenFiles extends AppCompatActivity {
    private static String pathname;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressDialog= new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setTitle("Please Wait");
        progressDialog.show();
        pathname = getFilesDir().getPath() + File.separator + "stored";
        if(getIntent().getData()==null){
            finish();
            return;
        }
        try {
            load();
        } catch (IOException e) {
            Toast.makeText(this,"File not loaded",Toast.LENGTH_LONG).show();
            Log.e("error","loading file",e);
            finish();
        }
    }

    private void end() {
        if(progressDialog!=null&&progressDialog.isShowing())progressDialog.dismiss();
        Intent intent= new Intent(this,PackageActivity.class);
        startActivity(intent);
    }

    private void load() throws IOException {
        initDir();
        Intent intent=getIntent();
        ContentResolver contentResolver= getContentResolver();
        Uri uri = intent.getData();
        String name = getContentName(contentResolver, uri);

        Log.v("tag" , "Content intent detected: " + action + " : " + intent.getDataString() + " : " + intent.getType() + " : " + name);
        InputStream input = contentResolver.openInputStream(uri);
        askForName(input,name);

    }

    private void askForName(final InputStream input, String name) {
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle("Pleas name your import");
        final EditText editText= new EditText(this);
        editText.getText().insert(0,name.replace(".sara",""));
        editText.selectAll();
        builder.setView(editText);
        builder.setNegativeButton("Cancel Import", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                end();
            }
        });
        builder.setPositiveButton("Import", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String importfilepath = pathname +File.separator+ editText.getText().toString()+".sara";
                try {
                    new File(importfilepath).createNewFile();
                } catch (IOException e) {
                    Log.e("error","creating import file",e);
                }
                InputStreamToFile(input, importfilepath);
                end();
            }
        });
        builder.create().show();
    }

    private void initDir() {
        File file= new File(pathname);
        file.mkdirs();
    }

    private String getContentName(ContentResolver resolver, Uri uri){
        Cursor cursor = resolver.query(uri, null, null, null, null);
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex >= 0) {
            return cursor.getString(nameIndex);
        } else {
            return null;
        }
    }

    private void InputStreamToFile(InputStream in, String file) {
        try {
            OutputStream out = new FileOutputStream(new File(file));

            int size = 0;
            byte[] buffer = new byte[1024];

            while ((size = in.read(buffer)) != -1) {
                out.write(buffer, 0, size);
            }

            out.close();
        }
        catch (Exception e) {
            Log.e("MainActivity", "InputStreamToFile exception: ", e);
        }
    }
}
