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
import com.google.firebase.database.Query;
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

public class AllChatsFragment extends Fragment {
    private String TAG = AllChatsFragment.class.getSimpleName();
    private RecyclerView recyclerView;
    private DatabaseReference mFirebaseDatabaseAllGroups;
    private FirebaseDatabase mFirebaseInstance;
    private ValueEventListener mAllChatsListener;
    private MyChatsRecyclerViewAdapter adapter;
    private TextView msgEmpty;
    private List<ChatGroup> chatGroups;

    public AllChatsFragment() {
    }

    public static AllChatsFragment newInstance() {
        AllChatsFragment fragment = new AllChatsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseDatabaseAllGroups = mFirebaseInstance.getReference("chats");
        chatGroups = new ArrayList<>();
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
    }

    private void getAllChatGroups(DataSnapshot dataSnapshot) {
        chatGroups.clear();
        for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
            ChatGroup chatGroup = singleSnapshot.getValue(ChatGroup.class);
            if (chatGroup.isPublic()) {
                String key = singleSnapshot.getKey();
                chatGroup.setId(key);
                chatGroups.add(chatGroup);
            }
        }
        displayList();
    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseDatabaseAllGroups.addValueEventListener(mAllChatsListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAllChatsListener != null) {
            mFirebaseDatabaseAllGroups.removeEventListener(mAllChatsListener);
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
                Intent intent = new Intent(getActivity(), ChatMessagesActivity.class);
                intent.putExtra("group", chatGroups.get(position));
                startActivity(intent);
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
