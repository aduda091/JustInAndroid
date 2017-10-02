package hr.unipu.duda.justintime.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import hr.unipu.duda.justintime.R;
import hr.unipu.duda.justintime.model.User;
import hr.unipu.duda.justintime.requests.LoginRequest;
import hr.unipu.duda.justintime.util.UserController;

public class LoginActivity extends AppCompatActivity {
    EditText etUsername;
    EditText etPassword;
    TextInputLayout inputLayoutPassword;

    RequestQueue queue;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        queue = Volley.newRequestQueue(this);

        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);
        inputLayoutPassword = (TextInputLayout) findViewById(R.id.inputLayoutPassword);
        final TextView tvRegisterLink = (TextView) findViewById(R.id.tvRegisterLink);
        Button btnLogin = (Button) findViewById(R.id.btnLogin);

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Prijava u tijeku");
        progressDialog.setCancelable(false);

        if(getIntent().getExtras() != null) {
            //korisnik se upravo registrirao
            etUsername.setText(getIntent().getStringExtra("username"));
            etPassword.setText(getIntent().getStringExtra("password"));

        }

        if(UserController.getInstance().isRemembered()) {
            // TODO: ako je istekao token, popuni polja da se zatraži novi
            //korisnički podaci su već spremljeni
            /*User user = UserController.getInstance().getUser();
            etUsername.setText(user.getMail());
            etPassword.setText(user.getPassword());*/

            //preusmjeri korisnika s login ekrana ako je već prijavljen
            Intent intent = new Intent(LoginActivity.this, FacilityListActivity.class);
            startActivity(intent);
            finish();
        }

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(validateFields()) return;

                final String username = etUsername.getText().toString();
                final String password = etPassword.getText().toString();

                if(!progressDialog.isShowing()) progressDialog.show();

                LoginRequest loginRequest = new LoginRequest(username, password, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("response", "onResponse: " +response.toString());
                        try {
                            String token = response.getString("access_token");
                            getUserData(token, password);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        try {
                            Log.d("onError", "Error: " + error
                                    + "\nStatus Code " + error.networkResponse.statusCode
                                    + "\nResponse Data " + error.networkResponse.data.toString()
                                    + "\nCause " + error.getCause()
                                    + "\nmessage" + error.getMessage());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if(progressDialog.isShowing()) progressDialog.dismiss();
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                        builder.setMessage("Neuspješna prijava, pokušajte ponovno")
                                .setNegativeButton("U redu", null)
                                .create()
                                .show();
                    }
                });

                queue.add(loginRequest);

            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });



    }

    private boolean validateFields() {
        boolean hasErrors = false;

        if(etUsername.getText().toString().isEmpty()) {
            etUsername.requestFocus();
            etUsername.setError("E-mail ne smije biti prazan");
            hasErrors = true;
        }
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(etUsername.getText().toString()).matches()) {
            etUsername.requestFocus();
            etUsername.setError("E-mail mora biti valjan");
            hasErrors = true;
        }
        if(etPassword.getText().toString().trim().isEmpty()) {
            etPassword.requestFocus();
            //etPassword.setError("Lozinka ne smije biti prazna");
            inputLayoutPassword.setError("Lozinka ne smije biti prazna");
            hasErrors = true;
        }

        return hasErrors;
    }

    private void getUserData(final String token, final String password) {

        String url = "https://justin-time.herokuapp.com/user/me?access_token="+token;
        Map<String, String> params = new HashMap<>();
        params.put("access_token", token);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("/meResponse", "response: " +response.toString());
                User user = new User();
                try {
                    user.setId(response.getString("id"));
                    user.setFirstName(response.getString("firstName"));
                    user.setLastName(response.getString("lastName"));
                    user.setMail(response.getString("mail"));
                    user.setPassword(password);
                    user.setToken(token);

                    //spremi učitane podatke u localStorage
                    UserController.getInstance().saveUser(user);

                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setMessage("Hvala na prijavi, " +user.getFirstName() + " " + user.getLastName() + "!")
                            .setPositiveButton("Nema na čemu", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(LoginActivity.this, FacilityListActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            })
                            .create()
                            .show();
                    if(progressDialog.isShowing())progressDialog.dismiss();

                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("onError", "Error: " + error
                        + "\nStatus Code " + error.networkResponse.statusCode
                        + "\nResponse Data " + error.networkResponse.data.toString()
                        + "\nCause " + error.getCause()
                        + "\nmessage" + error.getMessage());
                if(progressDialog.isShowing()) progressDialog.dismiss();

                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setMessage("Neuspješna prijava, pokušajte ponovno")
                        .setNegativeButton("U redu", null)
                        .create()
                        .show();
            }
        });

        queue.add(request);
    }

//    @Override
//    public void onBackPressed() {
//        new AlertDialog.Builder(this)
//                .setTitle("Izlaz")
//                .setMessage("Želite li izaći iz aplikacije?")
//                .setNegativeButton("Ne", null)
//                .setPositiveButton("Da", new DialogInterface.OnClickListener() {
//
//                    public void onClick(DialogInterface arg0, int arg1) {
//                        /*moveTaskToBack(true);
//                        android.os.Process.killProcess(android.os.Process.myPid());
//                        finishAndRemoveTask();
//                        System.exit(1);*/
//                        finish();
//                    }
//                }).create().show();
//
//    }
}
