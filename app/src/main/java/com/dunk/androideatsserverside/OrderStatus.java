package com.dunk.androideatsserverside;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dunk.androideatsserverside.Common.Common;
import com.dunk.androideatsserverside.Interface.APIService;
import com.dunk.androideatsserverside.Interface.ItemClickListener;
import com.dunk.androideatsserverside.ViewHolder.FoodViewHolder;
import com.dunk.androideatsserverside.ViewHolder.OrderViewHolder;
import com.dunk.androideatsserverside.model.Food;
import com.dunk.androideatsserverside.model.MyResponse;
import com.dunk.androideatsserverside.model.Notification;
import com.dunk.androideatsserverside.model.Order;
import com.dunk.androideatsserverside.model.Request;
import com.dunk.androideatsserverside.model.Sender;
import com.dunk.androideatsserverside.model.Token;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderStatus extends AppCompatActivity {

    @BindView(R.id.listOrders)
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;
    FirebaseDatabase db;
    DatabaseReference requests;
    String phone;
    MaterialSpinner spinner;

    FirebaseRecyclerAdapter<Request, OrderViewHolder> adapter;

    APIService mService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);
        ButterKnife.bind(this);

        //firebase
        db = FirebaseDatabase.getInstance();
        requests = db.getReference("Requests");

        //init service
        mService = Common.getFCMService();

        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        loadOrders(); // load all orders


    }

    private void loadOrders() {

        Query query = requests;

        FirebaseRecyclerOptions<Request> options =
                new FirebaseRecyclerOptions.Builder<Request>()
                        .setQuery(query, new SnapshotParser<Request>() {
                            @NonNull
                            @Override
                            public Request parseSnapshot(@NonNull DataSnapshot snapshot) {
                                List<Order> tfoods = (ArrayList<Order>) snapshot.child("foods").getValue();
                                System.out.println(tfoods);

                                return new Request(
                                        snapshot.child("address").getValue().toString(),
                                        snapshot.child("name").getValue().toString(),
                                        snapshot.child("phone").getValue().toString(),
                                        snapshot.child("status").getValue().toString(),
                                        snapshot.child("total").getValue().toString(),
                                        tfoods);
                            }

                        }).build();


        adapter = new FirebaseRecyclerAdapter<Request, OrderViewHolder>(options) {
            @Override
            public OrderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.order_layout, parent, false);

                return new OrderViewHolder(view);
            }


            @Override
            protected void onBindViewHolder(OrderViewHolder viewHolder, final int position, final Request model) {
                viewHolder.txtOrderId.setText("Order Id: " +"#" + adapter.getRef(position).getKey());
                viewHolder.txtOrderStatus.setText("Status: " + Common.convertCodeToStatus(model.getStatus()));
                viewHolder.txtOrderPhone.setText("Phone: " + model.getPhone());
                viewHolder.txtOrderAddress.setText("Address: " + model.getAddress());

                System.out.println(model.getAddress());

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onclick(View view, int position, boolean isLongClick) {
                        Intent trackOderIntent = new Intent(OrderStatus.this, TrackingOrder.class);
                        Common.currentRequest = model;
                        startActivity(trackOderIntent);
                    }
                });
            }
        };
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if (item.getTitle().equals(Common.UPDATE)){
            showUpdatedDialog(adapter.getRef(item.getOrder()).getKey(), adapter.getItem(item.getOrder()));
        }
        else if (item.getTitle().equals(Common.DELETE)){
            deleteOrder(adapter.getRef(item.getOrder()).getKey());
        }

        return super.onContextItemSelected(item);
    }

    private void showUpdatedDialog(String key, final Request item) {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Updated Order");
        alertDialog.setMessage("Please choose status");

        LayoutInflater inflator = this.getLayoutInflater();
        final View view = inflator.inflate(R.layout.update_order_layout, null);

        spinner = (MaterialSpinner)view.findViewById(R.id.statusSpinner);
        spinner.setItems("Placed", "Shipping", "Shipped");

        alertDialog.setView(view);

        final String localKey = key;
        alertDialog.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                item.setStatus(String.valueOf(spinner.getSelectedIndex()));
                requests.child(localKey).setValue(item);
                sendOrderStatusToUser(localKey,item);
            }
        });

        alertDialog.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });

        alertDialog.show();

    }

    private void sendOrderStatusToUser(final String key, Request item) {
        DatabaseReference tokens = db.getReference("Tokens");
        tokens.orderByKey().equalTo(item.getPhone())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapShot:dataSnapshot.getChildren()){
                            Token token = postSnapShot.getValue(Token.class);

                            //create raw payload to send
                            Notification notification = new Notification ("Eats","You order " + key + " was updated");
                            Sender content = new Sender(token.getToken(),notification);

                            mService.sendNotification(content)
                                    .enqueue(new Callback<MyResponse>() {
                                        @Override
                                        public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                            if (response.code() == 200){
                                                if (response.body().success == 1)
                                                {
                                                    Toast.makeText(OrderStatus.this, "Order updated", Toast.LENGTH_SHORT).show();
                                                    finish();
                                                }
                                                else
                                                {
                                                    Toast.makeText(OrderStatus.this, "Order updated but failed to send notification", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<MyResponse> call, Throwable t) {
                                            Log.e("ERROR", t.getMessage());
                                        }
                                    });

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void deleteOrder(String key) {
        requests.child(key).removeValue();
        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
    }
}
