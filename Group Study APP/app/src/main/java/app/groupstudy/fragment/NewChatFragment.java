package app.groupstudy.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.util.Map;

import app.groupstudy.R;
import app.groupstudy.activity.MainActivity;
import app.groupstudy.adapter.ParticipantsAdapter;
import app.groupstudy.database.ChatGroup;
import app.groupstudy.database.MyFirebaseDatabase;
import app.groupstudy.database.User;
import app.groupstudy.helper.RecyclerTouchListener;

import static android.app.Activity.RESULT_OK;

public class NewChatFragment extends Fragment implements MyFirebaseDatabase.MyFirebaseDatabaseListener {
    private String TAG = NewChatFragment.class.getSimpleName();
    private List<User> participants;
    private RecyclerView recyclerView;
    private ParticipantsAdapter adapter;
    private ProgressBar progressBar, progressBarButton;
    private Button btnCreateGroup;
    private EditText groupName;
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    private ValueEventListener mUsersListener;
    private SwitchCompat switchVisibility;
    private LinearLayout layoutChooseImage;
    private ImageView imgPreview;
    private static final int REQUEST_IMAGE = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private String picturePath;
    private MyFirebaseDatabase myFirebaseDatabase;
    FirebaseUser firebaseUser;
    FirebaseAuth mFirebaseAuth;
    private TextView txtMsgNoParticipants;

    public NewChatFragment() {
        // Required empty public constructor
    }

    public static NewChatFragment newInstance(String param1, String param2) {
        NewChatFragment fragment = new NewChatFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseInstance = FirebaseDatabase.getInstance();

        // get reference to 'users' node
        mFirebaseDatabase = mFirebaseInstance.getReference("users");
        if (getArguments() != null) {
        }

        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_new_chat, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        progressBarButton = (ProgressBar) view.findViewById(R.id.progressBarButton);
        btnCreateGroup = (Button) view.findViewById(R.id.btn_create_chat_group);
        groupName = (EditText) view.findViewById(R.id.input_group_name);
        switchVisibility = (SwitchCompat) view.findViewById(R.id.switch_visibility);
        layoutChooseImage = (LinearLayout) view.findViewById(R.id.layout_choose_image);
        imgPreview = (ImageView) view.findViewById(R.id.img_preview);
        txtMsgNoParticipants = (TextView) view.findViewById(R.id.txt_no_participants);
        txtMsgNoParticipants.setVisibility(View.GONE);
        fetchParticipants();

        progressBarButton.setVisibility(View.GONE);
        disableButton();

        btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createChatGroup();
            }
        });

        layoutChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        imgPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        myFirebaseDatabase = new MyFirebaseDatabase(this);

        groupName.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                toggleButtonState();
            }
        });

        return view;
    }

    private void chooseImage() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Snackbar.make(MainActivity.coordinatorLayout,
                        "Please Grant Permissions",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission
                                                .WRITE_EXTERNAL_STORAGE},
                                        REQUEST_PERMISSIONS);
                            }
                        }).show();
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_CONTACTS},
                        REQUEST_PERMISSIONS);
            }
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
            imgPreview.setImageBitmap(BitmapFactory.decodeFile(picturePath));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if ((grantResults.length > 0) && (grantResults[0] +
                        grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                } else {
                    Snackbar.make(MainActivity.coordinatorLayout, "Enable Permissions from settings",
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(intent);
                                }
                            }).show();
                }
                return;
            }
        }
    }

    private void getAllUsers(DataSnapshot dataSnapshot) {
        firebaseUser = mFirebaseAuth.getCurrentUser();
        if (firebaseUser == null)
            return;
        if (dataSnapshot.getChildrenCount() > 0)
            participants.clear();

        for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
            User user = singleSnapshot.getValue(User.class);
            user.setuId(singleSnapshot.getKey());

            // skip current user
            if (!firebaseUser.getUid().equals(user.getuId()))
                participants.add(user);
            adapter.notifyDataSetChanged();
        }

        refreshList();
    }

    private void createChatGroup() {
        String groupName = this.groupName.getText().toString().trim();

        if (TextUtils.isEmpty(groupName)) {
            Toast.makeText(getActivity(), getString(R.string.error_empty_group_name), Toast.LENGTH_LONG).show();
            return;
        }

        if (adapter.getSelectedIndexes().size() == 0) {
            Toast.makeText(getActivity(), getString(R.string.error_empty_participants), Toast.LENGTH_LONG).show();
            return;
        }

        disableButton();
        progressBarButton.setVisibility(View.VISIBLE);

        ChatGroup chatGroup = new ChatGroup();
        chatGroup.setSubject(groupName);
        chatGroup.setMemberCount(adapter.getSelectedIndexes().size() + 1);
        chatGroup.setTimestamp(System.currentTimeMillis());
        chatGroup.setPublic(switchVisibility.isChecked());
        Map<String, Boolean> members = new HashMap<>();
        for (int pos : adapter.getSelectedIndexes()) {
            members.put(participants.get(pos).getuId(), true);
        }

        // save the group
        Log.e(TAG, "insertGroup");
        myFirebaseDatabase.insertGroup(chatGroup, members, picturePath);
    }

    private void fetchParticipants() {
        participants = new ArrayList<>();
        adapter = new ParticipantsAdapter(getActivity(), participants);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                adapter.toggleSelectedRow(position);
                toggleButtonState();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    private void toggleButtonState() {
        if (groupName.getText().toString().trim().length() > 0 && adapter.getSelectedIndexes().size() > 0) {
            enableButton();
        } else {
            disableButton();
        }
    }

    private void refreshList() {
        Log.e(TAG, "refreshList");
        progressBar.setVisibility(View.GONE);
        if (participants.size() > 0) {
            txtMsgNoParticipants.setVisibility(View.GONE);
        } else {
            txtMsgNoParticipants.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();
        mUsersListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                getAllUsers(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        mFirebaseDatabase.addValueEventListener(mUsersListener);
    }

    private void disableButton() {
        Log.e(TAG, "disableButton");
        btnCreateGroup.setAlpha(.5f);
        btnCreateGroup.setClickable(false);
        btnCreateGroup.setEnabled(false);
    }

    private void enableButton() {
        btnCreateGroup.setAlpha(1f);
        btnCreateGroup.setClickable(true);
        btnCreateGroup.setEnabled(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mUsersListener != null) {
            mFirebaseDatabase.removeEventListener(mUsersListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onGroupCreated() {
        Log.e(TAG, "onGroupCreated");
        Intent registrationComplete = new Intent(MainActivity.NAVIGATE_VIEW);
        registrationComplete.putExtra("index", 0);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(registrationComplete);
    }

    @Override
    public void onGroupCreateFailed() {
    }
}