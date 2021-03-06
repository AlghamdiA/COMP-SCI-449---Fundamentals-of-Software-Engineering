package app.groupstudy.fragment;

import android.content.Context;
import android.content.Intent;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import app.groupstudy.R;
import app.groupstudy.activity.ChatMessagesActivity;
import app.groupstudy.adapter.MyChatsRecyclerViewAdapter;
import app.groupstudy.database.ChatGroup;
import app.groupstudy.helper.RecyclerTouchListener;

public class MyChatsFragment extends Fragment {
    private String TAG = MyChatsFragment.class.getSimpleName();
    private static final String ARG_LOAD_PRIVATE = "load-private";
    private RecyclerView recyclerView;
    private DatabaseReference mFirebaseDatabaseAllGroups;
    private DatabaseReference mFirebaseDatabaseUserChats;
    private FirebaseDatabase mFirebaseInstance;
    private ValueEventListener mAllChatsListener;
    private ValueEventListener mUserChatListener;
    private MyChatsRecyclerViewAdapter adapter;
    private TextView msgEmpty;
    private Map<String, Boolean> myGroups;
    private List<ChatGroup> chatGroups;

    public MyChatsFragment() {
    }

    public static MyChatsFragment newInstance() {
        MyChatsFragment fragment = new MyChatsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseDatabaseAllGroups = mFirebaseInstance.getReference("chats");
        mFirebaseDatabaseUserChats = mFirebaseInstance.getReference("users")
                .child(uid).child("chats");
        chatGroups = new ArrayList<>();
        myGroups = new HashMap<>();
        adapter = new MyChatsRecyclerViewAdapter(chatGroups, getActivity());

        mAllChatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                getAllChatGroups(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mUserChatListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                getUserChatGroups(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    private void getUserChatGroups(DataSnapshot dataSnapshot) {
        Map<String, Boolean> groups = (Map<String, Boolean>) dataSnapshot.getValue();
        if (groups != null) {
            myGroups.clear();
            for (Map.Entry<String, Boolean> entry : groups.entrySet()) {
                Log.e(TAG, "My Group: " + entry.getKey());
                myGroups.put(entry.getKey(), entry.getValue());
            }
        } else {
            myGroups.clear();
        }

        filterChatGroups();
        displayList();
    }

    private void getAllChatGroups(DataSnapshot dataSnapshot) {
        chatGroups.clear();
        for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
            ChatGroup chatGroup = singleSnapshot.getValue(ChatGroup.class);
            String key = singleSnapshot.getKey();
            chatGroup.setId(key);
            chatGroups.add(chatGroup);
        }

        filterChatGroups();
        displayList();
    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseDatabaseAllGroups.addValueEventListener(mAllChatsListener);
        mFirebaseDatabaseUserChats.addValueEventListener(mUserChatListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mFirebaseDatabaseAllGroups.addValueEventListener(mAllChatsListener);
        mFirebaseDatabaseUserChats.addValueEventListener(mUserChatListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAllChatsListener != null) {
            mFirebaseDatabaseAllGroups.removeEventListener(mAllChatsListener);
        }

        if (mUserChatListener != null) {
            mFirebaseDatabaseUserChats.removeEventListener(mUserChatListener);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_chats, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        msgEmpty = (TextView) view.findViewById(R.id.msg_empty_data);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                try {
                    Intent intent = new Intent(getActivity(), ChatMessagesActivity.class);
                    intent.putExtra("group", chatGroups.get(position));
                    startActivity(intent);
                } catch (Exception e) {
                }
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        displayList();
        return view;
    }

    private void displayList() {
        if (chatGroups.size() > 0) {
            msgEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            msgEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
    }


    private void filterChatGroups() {
        Log.e(TAG, "filterChatGroups mine:" + myGroups.size() + ", all: " + chatGroups.size());
        for (Iterator<ChatGroup> iterator = chatGroups.iterator(); iterator.hasNext(); ) {
            ChatGroup chatGroup = iterator.next();
            if (!myGroups.containsKey(chatGroup.getId())) {
                iterator.remove();
            }
        }
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
