package com.example.alex.sarasfotoapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CodeActivty extends AppCompatActivity {

    public static final String returnKey="return";
    public static final String EXTRA_NOT_STARTUP = "notStartup";
    private static final String PREFERENCE_KEY="Sara";
    private static final String PASSWORD_KEY="password";
    public static String password;
    private static SharedPreferences sharedPreferences;
    private EditText passwordMainTextField;
    private EditText passwordRepeatTextField;
    private Button saveButton;
    private ViewMode viewMode;
    private EditText mainPassWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_activty);
        if(sharedPreferences==null){
            sharedPreferences= getSharedPreferences(PREFERENCE_KEY,MODE_PRIVATE);
        }
        passwordMainTextField=(EditText)findViewById(R.id.password1);
        passwordRepeatTextField=(EditText)findViewById(R.id.password2);
        saveButton= (Button)findViewById(R.id.saveButton);
        if (sharedPreferences.contains(PASSWORD_KEY)){
            requestPassword();
        }else{
            setPassword();
        }
        passwordMainTextField.selectAll();
        mainPassWord = (EditText) findViewById(R.id.password1);
        mainPassWord.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (viewMode.equals(ViewMode.REQUEST) || viewMode.equals(ViewMode.REQUEST_AND_RETURN)) {
                    saveOrGo(v);
                }
                return false;
            }
        });
        if (!getIntent().getBooleanExtra(EXTRA_NOT_STARTUP, false)) {
            try {
                deleteTemp();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    private void deleteTemp() throws IOException {
        File dir = new File(getFilesDir().getPath() + File.separator + "sentImages");
        if (dir.exists()) {
            delete(dir);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.code_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo contextMenuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.changePassword:
                changePasswordDialog();
                break;

        }

        return true;
    }

    private void changePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change password...");
        builder.setMessage("Enter your old and new password");
        final View inflate = getLayoutInflater().inflate(R.layout.change_password_dialog, null);
        final TextView error = (TextView) inflate.findViewById(R.id.error);
        error.setVisibility(View.GONE);
        builder.setView(inflate);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                error.setVisibility(View.VISIBLE);
                EditText old = (EditText) inflate.findViewById(R.id.oldPass);
                EditText firstNew = (EditText) inflate.findViewById(R.id.firstPass);
                EditText secondNew = (EditText) inflate.findViewById(R.id.secondPass);
                if (!old.getText().toString().equals(sharedPreferences.getString(PASSWORD_KEY, ""))) {
                    error.setText("Old password wrong");
                    old.selectAll();
                    return;
                }
                if (!firstNew.getText().toString().equals(secondNew.getText().toString())) {
                    error.setText("New passwords don't match");
                    firstNew.selectAll();
                    return;
                }
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putString(PASSWORD_KEY, firstNew.getText().toString());
                edit.apply();
                Toast.makeText(CodeActivty.this, "Password changed", Toast.LENGTH_LONG).show();
                alertDialog.dismiss();
            }
        });

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setPositiveButton("Close App", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory( Intent.CATEGORY_HOME );
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setTitle("Back Clicked");
        builder.setMessage("Do you wanna leave the App?");
        builder.create().show();
    }

    private void setPassword() {
        viewMode=ViewMode.SET;
        saveButton.setText("Save");
    }

    private void requestPassword() {
        viewMode=getIntent().getBooleanExtra(returnKey,false)?ViewMode.REQUEST_AND_RETURN:ViewMode.REQUEST;
        passwordRepeatTextField.setVisibility(View.GONE);
        saveButton.setText("Go");
    }


    public void saveOrGo(View view) {
        if(viewMode.equals(ViewMode.REQUEST_AND_RETURN)){
            if(passwordMainTextField.getText().toString().equals(sharedPreferences.getString(PASSWORD_KEY,""))){
                password=passwordMainTextField.getText().toString();
                finish();
            }else {
                showWrongPassword();
            }
            return;
        }else if(viewMode.equals(ViewMode.SET)){
            if (!passwordMainTextField.getText().toString().equals(passwordRepeatTextField.getText().toString())){
                passwordNotTheSame();
                return;
            }else{
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.putString(PASSWORD_KEY,passwordMainTextField.getText().toString());
                edit.apply();
            }
        }else if(viewMode.equals(ViewMode.REQUEST)){
            if(!passwordMainTextField.getText().toString().equals(sharedPreferences.getString(PASSWORD_KEY,""))){
                showWrongPassword();
                return;
            }
        }
        password=passwordMainTextField.getText().toString();
        Intent intent= new Intent(this,PackageActivity.class);
        startActivity(intent);
        finish();
    }

    private void passwordNotTheSame() {
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle("Oh oh!");
        builder.setTitle("Passwords do not mattch!");
        builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                passwordMainTextField.getText().clear();
                passwordRepeatTextField.getText().clear();
                passwordMainTextField.selectAll();
            }
        });
    }

    private void showWrongPassword() {
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setMessage("Wrong Password!");
        builder.setTitle("Oh oh!");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
        passwordMainTextField.selectAll();
    }

    private enum ViewMode {
        SET,
        REQUEST,
        REQUEST_AND_RETURN
    }
}
