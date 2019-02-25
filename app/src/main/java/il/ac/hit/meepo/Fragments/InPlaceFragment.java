package il.ac.hit.meepo.Fragments;


import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import il.ac.hit.meepo.Adapters.UserInPlaceAdapter;
import il.ac.hit.meepo.Helpers.SwipeController;
import il.ac.hit.meepo.Helpers.SwipeControllerActions;
import il.ac.hit.meepo.Models.Place;
import il.ac.hit.meepo.Models.User;
import il.ac.hit.meepo.OtherUserProfileActivity;
import il.ac.hit.meepo.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class InPlaceFragment extends Fragment {
    RecyclerView recyclerView;
    UserInPlaceAdapter userInPlaceAdapter = null;
    List<User> listOfUsersInPlaceNow;
    View view;
    SwipeController swipeController = null;
    boolean watchOtherUserDeatails;

    boolean alreadyDeltedFromList = false;

    private FirebaseAuth mAuth;
    private FirebaseUser mFirebaseUser;
    private FirebaseDatabase mDataBase;
    private DatabaseReference reference;
    private String LogedInUserId;

    DatabaseReference usersDb;
    String currentUId;

    Place currentPlace;

    boolean swipeBack;
    private static final String TAG = "InPlaceFragment";

    public InPlaceFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView:  start ");
        watchOtherUserDeatails = false;

        view = inflater.inflate(R.layout.fragment_in_place, container, false);
        recyclerView = view.findViewById(R.id.rv_users_in_place);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        Bundle arguments = getArguments();
        currentPlace = (Place)arguments.getSerializable("currentPlace");
        listOfUsersInPlaceNow = new ArrayList<>();

        setFireBaseDetails();
        setupRecyclerView();
        setUsersDataAdapter();

        //update
        usersDb = FirebaseDatabase.getInstance().getReference().child("Users");
        currentUId = mAuth.getCurrentUser().getUid();
        //

        return view;
    }

    private void setFireBaseDetails() {

        mAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mAuth.getCurrentUser();
        mDataBase = FirebaseDatabase.getInstance();
        LogedInUserId = mFirebaseUser.getUid();

    }

    @Override
    public void onPause() {
        super.onPause();
        if(!watchOtherUserDeatails) {
            DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("Users").child(mFirebaseUser.getUid());
            HashMap<String, Object> map = new HashMap<>();
            map.put("lastLocationPlaceId", "Not in place");
            myRef.updateChildren(map);
            HashMap<String, Object> map2 = new HashMap<>();
            List<String> dummyList = new ArrayList<>();
            String d = "default";
            dummyList.add(d);
            map2.put("likedByUserList", dummyList);
            myRef.updateChildren(map2);
            Log.d(TAG, "onPause: im Not in place");
        }
        Log.d(TAG, "onPause: ");
    }

    private void setupRecyclerView() {


        swipeController = new SwipeController(new SwipeControllerActions() {
            @Override
            public void onRightClicked(int position) {
                User objOtherUser = listOfUsersInPlaceNow.get(position);
                String stringOtherUserID = objOtherUser.getId();
                usersDb.child(stringOtherUserID).child("connections").child("nope").child(currentUId).setValue(true);

                userInPlaceAdapter.users.remove(position);
                userInPlaceAdapter.notifyDataSetChanged();

//                userInPlaceAdapter.notifyItemRemoved(position);
//                userInPlaceAdapter.notifyItemRangeChanged(position, userInPlaceAdapter.getItemCount());
            }

            @Override
            public void onLeftClicked(int position) {
                // update
                User objOtherUser = listOfUsersInPlaceNow.get(position);
                String stringOtherUserID = objOtherUser.getId();
                usersDb.child(stringOtherUserID).child("connections").child("yeps").child(currentUId).setValue(true);

                isConnectionMatch(stringOtherUserID);
                userInPlaceAdapter.users.remove(position);
                userInPlaceAdapter.notifyDataSetChanged();

            }
        });


        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(recyclerView);

        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                swipeController.onDraw(c);
            }
        });
    }

    private void setUsersDataAdapter() {
        Log.d(TAG, "setUsersDataAdapter: ");

        // TODO: CHECK IF : DESIRBLE SEX , NOT ME , I CURRENT PLACE, NOT IN MY NOPS
        reference = mDataBase.getReference("Users");
        userInPlaceAdapter = new UserInPlaceAdapter(listOfUsersInPlaceNow);
        recyclerView.setAdapter(userInPlaceAdapter);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: ");

                listOfUsersInPlaceNow.clear();
                for (DataSnapshot snapshot: dataSnapshot.getChildren()){
                    if(snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (!user.getId().equals(LogedInUserId) && user.getLastLocationPlaceId().equals(currentPlace.getmPlaceId())){
                           if(!(snapshot.child("connections").child("yeps").hasChild(currentUId) ||snapshot.child("connections").child("nope").hasChild(currentUId)) ) {
                               listOfUsersInPlaceNow.add(user);
                           }
                        }
                    }
                }
                userInPlaceAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        userInPlaceAdapter.setListener(new UserInPlaceAdapter.MyUserInPlaceListener() {
            @Override
            public void onUserClicked(int position, View view) {
                Intent intent = new Intent(getContext(), OtherUserProfileActivity.class);
                intent.putExtra("user_object" ,listOfUsersInPlaceNow.get(position));
                watchOtherUserDeatails = true;//TODO: DELETE THIS
                startActivity(intent);
            }

            @Override
            public void onUserLongClicked(int position, View view) {

            }
        });
    }


    private void isConnectionMatch(String otherUserUid){
        DatabaseReference currentUserConnectionDB = usersDb.child(currentUId).child("connections").child("yeps").child(otherUserUid);
        currentUserConnectionDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Toast.makeText(getContext(), "MATCH", Toast.LENGTH_SHORT).show();

                    String key = FirebaseDatabase.getInstance().getReference().child("NewChat").push().getKey();
                    usersDb.child(dataSnapshot.getKey()).child("connections").child("matches").child(currentUId).setValue("true");
                    usersDb.child(dataSnapshot.getKey()).child("connections").child("matches").child(currentUId).child("ChatId").setValue(key);
                    usersDb.child(currentUId).child("connections").child("matches").child(dataSnapshot.getKey()).setValue("true");
                    usersDb.child(currentUId).child("connections").child("matches").child(dataSnapshot.getKey()).child("ChatId").setValue(key);
                    //TODO: MATCH NOTIFICATION UPDATE TO
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
