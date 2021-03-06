package hr.unipu.duda.justintime.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import hr.unipu.duda.justintime.R;
import hr.unipu.duda.justintime.adapters.QueueAdapter;
import hr.unipu.duda.justintime.model.Facility;
import hr.unipu.duda.justintime.model.Queue;
import hr.unipu.duda.justintime.util.AppController;

public class QueueListActivity extends AppCompatActivity {

    ProgressDialog progressDialog;
    RecyclerView recyclerView;
    RecyclerView.Adapter adapter;
    Facility facility;
    List<Queue> queues;
    RequestQueue volleyQueue;
    SwipeRefreshLayout swipeContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue_list);
        facility = new Facility();
        facility.setId(getIntent().getStringExtra("id"));
        facility.setName(getIntent().getStringExtra("name"));
        setTitle(facility.getName() + " - redovi" );

        volleyQueue = Volley.newRequestQueue(this);

        recyclerView = (RecyclerView) findViewById(R.id.queueRecyclerView);
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        swipeContainer.setSize(SwipeRefreshLayout.LARGE);
        swipeContainer.setColorSchemeResources(R.color.colorPrimary);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                populateQueues();
            }
        });




    }

    @Override
    protected void onResume() {
        super.onResume();
        populateQueues();
    }

    private void populateQueues() {
        swipeContainer.setRefreshing(true);
        //dohvaćanje svih redova trenutne ustanove
        queues = new ArrayList<>();
        String url = AppController.API_URL + "/facilities/" +facility.getId();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("populateQueues", "onResponse: " +response.toString());
                try {
                    JSONArray queuesArray = response.getJSONArray("queues");
                    for (int i=0;i<queuesArray.length();i++) {
                        JSONObject object = queuesArray.getJSONObject(i);
                        Queue queue = new Queue();
                        queue.setId(object.getString("_id"));
                        queue.setName(object.getString("name"));
                        queue.setFacility(facility);
                        queue.setCurrent(object.getInt("current"));
                        queue.setNext(object.getInt("next"));
                        queues.add(queue);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                adapter = new QueueAdapter(QueueListActivity.this, queues);
                recyclerView.setAdapter(adapter);

                swipeContainer.setRefreshing(false);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("onErrorResponse", "onErrorResponse: " + error.getMessage());

                swipeContainer.setRefreshing(false);

                AlertDialog.Builder builder = new AlertDialog.Builder(QueueListActivity.this);
                builder.setMessage("Neuspješan dohvat podataka, molim pokušajte ponovno!")
                        .setNegativeButton("U redu", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                populateQueues();
                            }
                        })
                        .create().show();
            }
        });

        volleyQueue.add(request);
    }


}
