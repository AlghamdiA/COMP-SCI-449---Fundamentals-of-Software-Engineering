package com.example.vaibhav.loginsignup;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    public static String filename = "userdatabase";
    SharedPreferences somedata;
    LinearLayout r1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        final Button log = (Button) findViewById(R.id.login);
        final Button sign = (Button) findViewById(R.id.signup);

        log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog1 = new Dialog(MainActivity.this);

                dialog1.requestWindowFeature(Window.FEATURE_NO_TITLE);
                View decorView1 = dialog1.getWindow().getDecorView();
                decorView1.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                dialog1.setContentView(R.layout.custom_dialog_login);
                dialog1.setCanceledOnTouchOutside(false);

                dialog1.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
                dialog1.show();
                Button login = (Button) dialog1.findViewById(R.id.login);
                final EditText userId = (EditText) dialog1.findViewById(R.id.userid);
                final EditText password = (EditText) dialog1.findViewById(R.id.password);
                login.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        String userid = "", Password = "";
                        int size=0;
                        try {
                            somedata = getSharedPreferences(filename, 0);
                            size=somedata.getInt("array_size",0);

                        } catch (Exception e) {
                        }
                        int present=0;
                        for(int i=1; i<=size; i++)
                        {
                            userid = somedata.getString("userid_"+i, "Could't find Data");
                            Password = somedata.getString("password_"+i, "Could't find Data");
                            if (userid.equals(userId.getText().toString()) && (Password.equals(password.getText().toString()))) {
                                dialog1.dismiss();
                                Intent in=new Intent(MainActivity.this,HomeActivity.class);
                                in.putExtra("flag","0");
                                startActivity(in);
                                present=1;
                                break;

                            }
                        }
                        if(present==0)
                            Toast.makeText(MainActivity.this, "username or password is not correct", Toast.LENGTH_SHORT).show();



                    }
                });
                dialog1.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
            }
        });


        sign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog1 = new Dialog(MainActivity.this);
                dialog1.setCanceledOnTouchOutside(false);
                dialog1.requestWindowFeature(Window.FEATURE_NO_TITLE);

                dialog1.setContentView(R.layout.custom_dialog_signup);
                // button
                dialog1.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
                dialog1.show();
                final EditText date = (EditText) dialog1.findViewById(R.id.date);
                date.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // calender class's instance and get current date , month and year from calender
                        final Calendar c = Calendar.getInstance();
                        int mYear = c.get(Calendar.YEAR); // current year
                        int mMonth = c.get(Calendar.MONTH); // current month
                        int mDay = c.get(Calendar.DAY_OF_MONTH); // current day
                        // date picker dialog
                        DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                                new DatePickerDialog.OnDateSetListener() {

                                    @Override
                                    public void onDateSet(DatePicker view, int year,
                                                          int monthOfYear, int dayOfMonth) {
                                        date.setText(dayOfMonth + "/"
                                                + (monthOfYear + 1) + "/" + year);

                                    }
                                }, mYear, mMonth, mDay);
                        datePickerDialog.show();
                    }
                });


                Button signup = (Button) dialog1.findViewById(R.id.signUp);
                final EditText Firstname = (EditText) dialog1.findViewById(R.id.firstname);
                final EditText Lastname = (EditText) dialog1.findViewById(R.id.lastname);
                final EditText Edu = (EditText) dialog1.findViewById(R.id.edu);
                final EditText userId = (EditText) dialog1.findViewById(R.id.userid);
                final EditText password = (EditText) dialog1.findViewById(R.id.password);
                signup.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub


                        String userid = userId.getText().toString();
                        String Password = password.getText().toString();
                        String FirstName = Firstname.getText().toString();
                        String LastName = Lastname.getText().toString();
                        String Educ=Edu.getText().toString();


                        int size=0;
                        try {
                            somedata = getSharedPreferences(filename, 0);

                            size = somedata.getInt("array_size", 0);
                        } catch (Exception e) {
                        }
                        String check;
                        int exist=0;
                        if(!userid.isEmpty()&&(!Password.isEmpty())&&(!FirstName.isEmpty())&&(!Educ.isEmpty())&&(!LastName.isEmpty())) {
                            for(int i=1;i<=size;++i){
                                check=somedata.getString("userid_"+i, "Could't find Data");
                                if(userid.equals(check)){
                                    exist=1;
                                    break;
                                }
                            }
                            if(exist==0) {
                                size++;
                                SharedPreferences.Editor editor = somedata.edit();
                                editor.putInt("array_size", size);
                                editor.putString("userid_" + size, userid);
                                editor.putString("password_" + size, Password);
                                editor.commit();
                                Intent i = new Intent(MainActivity.this, HomeActivity.class);
                                i.putExtra("flag", "0");
                                startActivity(i);
                                dialog1.dismiss();
                            }
                            else{
                                Toast.makeText(MainActivity.this,"Email Id already exists",Toast.LENGTH_SHORT).show();
                            }
                        }
                        else{
                            Toast.makeText(MainActivity.this,"All information are required",Toast.LENGTH_SHORT).show();


                        }
                    }
                });
                dialog1.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
