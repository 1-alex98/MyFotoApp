package com.example.alex.sarasfotoapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.support.v4.content.FileProvider.getUriForFile;

public class PackageActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private boolean openother =false;
    private static String pathname;
    private ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package);
        pathname = getFilesDir().getPath() + File.separator + "stored";
        listView=(ListView) findViewById(R.id.listView);

    }

    private void loadNewPackage() {
        List<File> listFiles = getListFiles(new File(pathname));
        List<String> fileNames= new ArrayList<>();
        final HashMap<String,File> fileLookUp= new HashMap<>();
        for(File file:listFiles){
            String insert = file.getName().replace(".sara", "");
            fileNames.add(insert);
            fileLookUp.put(insert,file);
        }
        final StableArrayAdapter adapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, fileNames);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openother=true;
                Intent intent= new Intent(PackageActivity.this, ShowOffActivity.class);
                intent.putExtra("open",fileLookUp.get(adapter.getItem(position)));
                startActivity(intent);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder= new AlertDialog.Builder(PackageActivity.this);
                builder.setTitle("Delete?");
                builder.setMessage("Delete this file?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fileLookUp.get(adapter.getItem(position)).delete();
                        loadNewPackage();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                return true;
            }
        });

    }


    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                if(file.getName().endsWith(".sara")){
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadNewPackage();
        if (CodeActivty.password==null){
            Intent intent= new Intent(this,CodeActivty.class);
            intent.putExtra(CodeActivty.returnKey,true);
            startActivity(intent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(openother)return;
        CodeActivty.password=null;
        openother =false;
    }

    public void createPackage(View view) {
        openother =true;
        Intent intent = new Intent();
// Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
// Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ArrayList<Bitmap> imagesEncodedList;
        try {
            // When an Image is picked
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                    && null != data) {
                // Get the Image from data
                imagesEncodedList = new ArrayList<>();
                if(data.getData()!=null){

                    Uri imageUri=data.getData();
                    imagesEncodedList.add(getBitmapFromUri(imageUri));


                } else {
                    if (data.getClipData() != null) {
                        ClipData mClipData = data.getClipData();
                        ArrayList<Uri> mArrayUri = new ArrayList<Uri>();
                        for (int i = 0; i < mClipData.getItemCount(); i++) {

                            ClipData.Item item = mClipData.getItemAt(i);
                            Uri uri = item.getUri();
                            mArrayUri.add(uri);

                            imagesEncodedList.add(getBitmapFromUri(uri));
                        }
                        Log.v("LOG_TAG", "Selected Images" + mArrayUri.size());
                    }
                }

                createFile(imagesEncodedList);
            } else {
                Toast.makeText(this, "You haven't picked Image",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        java.io.FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        // just some safety built in
        if( uri == null ) {
            // TODO perform some logging or show user feedback
            return null;
        }
        // try to retrieve the image from the media store first
        // this will only work for images selected from gallery
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            if(path!=null)return path;
        }
        // this is our fallback here
        return uri.getPath();
    }

    private void createFile(final ArrayList<Bitmap> imagesEncodedList) throws IOException {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("Name your new package");
        final EditText editText= new EditText(this);
        builder.setView(editText);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name="default";
                if(!editText.getText().toString().isEmpty()){
                    name=editText.getText().toString();
                }
                try {
                    File dir = new File(getFilesDir().getPath()+File.separator+"sentImages");
                    dir.mkdirs();
                    File out= new File(dir.getPath(),name+".sara");
                    out.createNewFile();
                    writeIntoFile(imagesEncodedList,out);
                } catch (IOException e) {
                    Log.e("érror","output file not created",e);
                    e.printStackTrace();
                    Toast.makeText(PackageActivity.this,"Error 17",Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.create().show();

    }

    private void writeIntoFile(final ArrayList<Bitmap> imagesEncodedList, final File outputFile) throws IOException {
        final ProgressDialog progressDialog= new ProgressDialog(this);
        progressDialog.setMessage("Please wait");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<File> Files= new ArrayList<>();
                for(Bitmap bitmap:imagesEncodedList){
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
                    byte[] bitmapdata = bos.toByteArray();
                    try {
                        writeToFile(outputFile, bitmapdata);
                    } catch (IOException e) {
                        Log.e("error","123..writing files",e);
                        e.printStackTrace();
                    }
                }
                progressDialog.dismiss();
                send(outputFile);
            }
        }).run();

    }

    private void send(File outputFile) {
        String[] files = {outputFile.getPath()};
        String[] mimeTypes={"application/sara"};
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM,getUriForFile(this,"com.example.alex.fileprovider",outputFile));
        shareIntent.setType("application/sara");
        startActivity(Intent.createChooser(shareIntent, "choose..."));

    }

    private void writeToFile(File outputFile, byte[] bitmapdata) throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFile,true);
        fos.write(bitmapdata);
        fos.flush();
        //TODO: save more infos in the sara file, like where the pictures start and how many are stored in the paticular file
        char[] chars = getString(R.string.code).toCharArray();
        byte[] bytes=new byte[chars.length];
        int i=0;
        for(char cha:chars){
            bytes[i++]=(byte) cha;
        }
        fos.write(bytes);
//
        fos.flush();
        fos.close();
    }

    public void mergeFiles(File[] files, File mergedFile) {

        FileWriter fstream = null;
        BufferedWriter out = null;

        try {
            fstream = new FileWriter(mergedFile, true);
            out = new BufferedWriter(fstream);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        for (File f : files) {
            System.out.println("merging: " + f.getName());
            FileInputStream fis;
            try {
                fis = new FileInputStream(f);
                BufferedReader in = new BufferedReader(new InputStreamReader(fis));


                while (in.ready()) {
                    out.write(in.read());
                }

                in.close();
                out.write("\n"+getString(R.string.code)+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
}
