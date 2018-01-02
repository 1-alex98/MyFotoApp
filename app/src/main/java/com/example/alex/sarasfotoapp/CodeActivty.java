package com.example.alex.sarasfotoapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.alex.sarasfotoapp.R;

public class CodeActivty extends AppCompatActivity {

    public static final String returnKey="return";
    private static final String PREFERENCE_KEY="Sara";
    private static final String PASSWORD_KEY="password";
    private static SharedPreferences sharedPreferences;
    private EditText passwordMainTextField;
    private EditText passwordRepeatTextField;
    private Button saveButton;
    private ViewMode viewMode;
    public static String password;

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
        REQUEST_AND_RETURN;
    }
}
