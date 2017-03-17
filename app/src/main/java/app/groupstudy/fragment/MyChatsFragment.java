package app.groupstudy.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.groupstudy.R;
import app.groupstudy.adapter.MyChatsRecyclerViewAdapter;
import app.groupstudy.database.Chat;

public class MyChatsFragment extends Fragment {
    private String TAG = MyChatsFragment.class.getSimpleName();
    private static final String ARG_LOAD_PRIVATE = "load-private";
    private RecyclerView recyclerView;
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    private ValueEventListener mUsersListener;
    private List<Chat> chats;
    private MyChatsRecyclerViewAdapter adapter;
    private TextView msgEmpty;
    private boolean loadOwn;
    private Map<String, Boolean> myGroups;

    public MyChatsFragment() {
    }

    public static MyChatsFragment newInstance(boolean loadPrivate) {
        MyChatsFragment fragment = new MyChatsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_LOAD_PRIVATE, loadPrivate);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            loadOwn = getArguments().getBoolean(ARG_LOAD_PRIVATE);
        }

        mFirebaseDatabase = FirebaseDatabase.getInstance().getReference("chats");
        chats = new ArrayList<>();
        myGroups = new HashMap<>();
        adapter = new MyChatsRecyclerViewAdapter(chats, myGroups, getActivity());

        mUsersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                getChatGroups(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(uid).
                child("chats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Boolean> groups = (Map<String, Boolean>) dataSnapshot.getValue();

                if (groups != null) {
                    if (groups.size() > 0) {
                        myGroups.clear();
                    }
                    for (Map.Entry<String, Boolean> entry : groups.entrySet()) {
                        myGroups.put(entry.getKey(), entry.getValue());
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseDatabase.addValueEventListener(mUsersListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mUsersListener != null) {
            mFirebaseDatabase.removeEventListener(mUsersListener);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_list, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        msgEmpty = (TextView) view.findViewById(R.id.msg_empty_data);


        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);
        displayList();
        return view;
    }

    private void displayList() {
        // TODO - check group count
        Log.e(TAG, "Chat private: " + loadOwn + ", count: " + chats.size());
        if (chats.size() > 0) {
            msgEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            msgEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }

    private void getChatGroups(DataSnapshot dataSnapshot) {
        Log.e(TAG, "load private: " + loadOwn);
        if (dataSnapshot.getChildrenCount() > 0)
            chats.clear();

        for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
            Chat chat = singleSnapshot.getValue(Chat.class);
            String key = singleSnapshot.getKey();
            chat.setId(key);

            chats.add(chat);

            // TODO - filter the chat groups depending on the type
            /*if (loadOwn && myGroups.containsKey(chat.getId())) {
                chats.add(chat);
            } else if (!loadOwn) {
                chats.add(chat);
            }*/
        }

        displayList();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
