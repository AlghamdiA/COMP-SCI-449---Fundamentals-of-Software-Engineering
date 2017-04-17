package app.groupstudy.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.appindexing.builders.PersonBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import app.groupstudy.R;
import app.groupstudy.database.ChatGroup;
import app.groupstudy.database.ChatMessage;
import app.groupstudy.database.MyFirebaseDatabase;
import app.groupstudy.helper.CircleTransform;

public class ChatMessagesActivity extends AppCompatActivity implements MyFirebaseDatabase.MyFirebaseDatabaseListener {

    private ChatGroup chatGroup;
    private static final String TAG = ChatMessagesActivity.class.getSimpleName();
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_IMAGE = 2;
    private static final int REQUEST_IMAGE_GROUP_ICON = 3;
    private static final String MESSAGE_URL = "https://group-study-87684.firebaseio.com/message/";
    private String mUsername;
    private String mPhotoUrl;
    private LinearLayout mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder> mFirebaseAdapter;
    private ProgressBar mProgressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;
    private boolean isGroupAdmin;
    private MyFirebaseDatabase myDb;
    private Map<String, Boolean> myGroups;
    private static String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_messages);
        chatGroup = (ChatGroup) getIntent().getSerializableExtra("group");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (chatGroup == null) {
            // group id is null, close this activity
            Toast.makeText(getApplicationContext(), getString(R.string.msg_group_not_found), Toast.LENGTH_LONG).show();
            finish();
        }
        displayToolbarIcon();
        Calendar calendar = Calendar.getInstance();
        today = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

        getSupportActionBar().setTitle(" " + chatGroup.getSubject());
        myDb = new MyFirebaseDatabase(this);
        myGroups = new HashMap<>();

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            if (chatGroup.getAdminId() != null && chatGroup.getAdminId().equals(mFirebaseUser.getUid())) {
                isGroupAdmin = true;
            }
        }

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ChatMessage, MessageViewHolder>(
                ChatMessage.class,
                R.layout.list_item_chat_message,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(chatGroup.getId())) {
            @Override
            protected ChatMessage parseSnapshot(DataSnapshot snapshot) {
                ChatMessage chatMessage = super.parseSnapshot(snapshot);
                if (chatMessage != null) {
                    chatMessage.setId(snapshot.getKey());
                }
                return chatMessage;
            }

            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder,
                                              ChatMessage friendlyMessage, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (friendlyMessage.getText() != null) {
                    viewHolder.messageTextView.setText(friendlyMessage.getText());
                    viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.messageImageView.setVisibility(ImageView.GONE);
                } else {
                    String imageUrl = friendlyMessage.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(viewHolder.messageImageView.getContext())
                                                    .load(downloadUrl)
                                                    .into(viewHolder.messageImageView);
                                        } else {
                                            Log.w(TAG, "Getting download url was not successful.",
                                                    task.getException());
                                        }
                                    }
                                });
                    } else {
                        Glide.with(viewHolder.messageImageView.getContext())
                                .load(friendlyMessage.getImageUrl())
                                .into(viewHolder.messageImageView);
                    }
                    viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                    viewHolder.messageTextView.setVisibility(TextView.GONE);
                }


                viewHolder.messengerTextView.setText(friendlyMessage.getName());
                if (friendlyMessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(ChatMessagesActivity.this,
                            R.drawable.ic_account_circle_black_24dp));
                } else {
                    Glide.with(ChatMessagesActivity.this)
                            .load(friendlyMessage.getPhotoUrl())
                            .transform(new CircleTransform(viewHolder.messageImageView.getContext()))
                            .into(viewHolder.messengerImageView);
                }

                if (friendlyMessage.getText() != null) {
                    // write this message to the on-device index
                    FirebaseAppIndex.getInstance().update(getMessageIndexable(friendlyMessage));
                }

                // timestamp
                viewHolder.messageTimestamp.setText(getTimeStamp(friendlyMessage.getTimestamp()));

                // log a view action on it
                FirebaseUserActions.getInstance().end(getMessageViewAction(friendlyMessage));
            }


        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mAddMessageImageView = (ImageView) findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });

        mSendButton = (LinearLayout) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChatMessage friendlyMessage = new ChatMessage(mMessageEditText.getText().toString(), mUsername,
                        mPhotoUrl, null);
                mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(chatGroup.getId()).push().setValue(friendlyMessage);

                // save last message
                mFirebaseDatabaseReference.child("chats").child(chatGroup.getId()).child("lastMessage").setValue(friendlyMessage.getText());
                mMessageEditText.setText("");

                // add the group to user/chats if the group is public
                if (chatGroup.isPublic()) {
                    myDb.joinGroup(chatGroup);
                }
            }
        });

        mMessageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    mSendButton.performClick();
                    return true;
                }
                return false;
            }
        });

        mFirebaseDatabaseReference.child("users").child(mFirebaseUser.getUid()).child("chats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Boolean> groups = (Map<String, Boolean>) dataSnapshot.getValue();
                if (groups != null) {
                    for (Map.Entry<String, Boolean> entry : groups.entrySet()) {
                        Log.e(TAG, "mygroup: " + entry.getKey());
                        myGroups.put(entry.getKey(), entry.getValue());
                    }

                    Log.e(TAG, "myGroups size: " + myGroups.size());
                }

                invalidateOptionsMenu();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(chatGroup.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getChildrenCount() == 0)
                            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void displayToolbarIcon() {
        getSupportActionBar().setIcon(R.drawable.ic_group_default);
        if (!TextUtils.isEmpty(chatGroup.getPhotoUrl()))
            Log.e(TAG, "chat icon: " + chatGroup.getPhotoUrl());
        Glide.with(getApplicationContext())
                .load(chatGroup.getPhotoUrl())
                .asBitmap()
                .transform(new CircleTransform(getApplicationContext()))
                .into(new SimpleTarget<Bitmap>(100, 100) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                        Drawable d = new BitmapDrawable(getResources(), resource);
                        getSupportActionBar().setIcon(d);
                    }
                });
    }

    public static String getTimeStamp(long dateStr) {
        SimpleDateFormat format;
        String timestamp;
        today = today.length() < 2 ? "0" + today : today;

        SimpleDateFormat todayFormat = new SimpleDateFormat("dd");
        String dateToday = todayFormat.format(new Date(dateStr));
        format = dateToday.equals(today) ? new SimpleDateFormat("hh:mm a") : new SimpleDateFormat("MMM d, yy hh:mm a");
        String date1 = format.format(new Date(dateStr));
        timestamp = date1.toString();

        return timestamp;
    }

    private Action getMessageViewAction(ChatMessage friendlyMessage) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(friendlyMessage.getName(), MESSAGE_URL.concat(friendlyMessage.getId()))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    private Indexable getMessageIndexable(ChatMessage friendlyMessage) {
        PersonBuilder sender = Indexables.personBuilder()
                .setIsSelf(mUsername == friendlyMessage.getName())
                .setName(friendlyMessage.getName())
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/sender"));

        PersonBuilder recipient = Indexables.personBuilder()
                .setName(mUsername)
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId() + "/recipient"));

        Indexable messageToIndex = Indexables.messageBuilder()
                .setName(friendlyMessage.getText())
                .setUrl(MESSAGE_URL.concat(friendlyMessage.getId()))
                .setSender(sender)
                .setRecipient(recipient)
                .build();

        return messageToIndex;
    }

    @Override
    public void onGroupCreated() {

    }

    @Override
    public void onGroupCreateFailed() {

    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageTextView;
        public ImageView messageImageView;
        public TextView messengerTextView;
        public ImageView messengerImageView;
        public TextView messageTimestamp;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.msg_full);
            messageImageView = (ImageView) itemView.findViewById(R.id.msg_img_attachment);
            messengerTextView = (TextView) itemView.findViewById(R.id.msg_author);
            messengerImageView = (ImageView) itemView.findViewById(R.id.msg_profile_image);
            messageTimestamp = (TextView) itemView.findViewById(R.id.msg_timestamp);
        }
    }

    @SuppressWarnings("VisibleForTests")
    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        storageReference.putFile(uri).addOnCompleteListener(ChatMessagesActivity.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            ChatMessage friendlyMessage =
                                    new ChatMessage(null, mUsername, mPhotoUrl,
                                            task.getResult().getMetadata().getDownloadUrl()
                                                    .toString());
                            mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(chatGroup.getId()).child(key)
                                    .setValue(friendlyMessage);
                        } else {
                            Log.w(TAG, "Image upload task was not successful.",
                                    task.getException());
                        }
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_leave_group) {
            leaveGroup();
        } else if (id == R.id.action_delete_group) {
            deleteGroup();
        } else if (id == R.id.action_edit_group) {
            editGroupIcon();
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteGroup() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int choice) {
                switch (choice) {
                    case DialogInterface.BUTTON_POSITIVE:
                        myDb.deleteGroup(chatGroup);
                        finish();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to delete this group? This can't be undone!")
                .setPositiveButton("DELETE", dialogClickListener)
            .setNegativeButton("NOT NOW", dialogClickListener).show();
    }

    private void editGroupIcon() {
        openGalleryForGroupIcon();
    }

    private void leaveGroup() {
        // remove chat group from users/chats
        myDb.leaveGroup(chatGroup);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isGroupAdmin) {
            getMenuInflater().inflate(R.menu.menu_group_admin, menu);
        } else if (myGroups.containsKey(chatGroup.getId())) {
            getMenuInflater().inflate(R.menu.menu_group_member, menu);
        }
        return true;
    }

    private void openGalleryForGroupIcon() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_IMAGE_GROUP_ICON);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE_GROUP_ICON && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getApplicationContext().getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Glide.with(getApplicationContext())
                    .load(picturePath)
                    .asBitmap()
                    .transform(new CircleTransform(getApplicationContext()))
                    .into(new SimpleTarget<Bitmap>(100, 100) {
                        @Override
                        public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                            Drawable d = new BitmapDrawable(getResources(), resource);
                            getSupportActionBar().setIcon(d);
                        }
                    });

            myDb.updateChatGroupIcon(chatGroup, picturePath);

            // update chat group icon
        } else if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK && null != data) {
            final Uri uri = data.getData();
            Log.d(TAG, "Uri: " + uri.toString());

            ChatMessage tempMessage = new ChatMessage(null, mUsername, mPhotoUrl,
                    "");
            mFirebaseDatabaseReference.child(MESSAGES_CHILD).child(chatGroup.getId()).push()
                    .setValue(tempMessage, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError,
                                               DatabaseReference databaseReference) {
                            if (databaseError == null) {
                                String key = databaseReference.getKey();
                                StorageReference storageReference =
                                        FirebaseStorage.getInstance()
                                                .getReference(mFirebaseUser.getUid())
                                                .child(key)
                                                .child(uri.getLastPathSegment());

                                putImageInStorage(storageReference, uri, key);
                            } else {
                                Log.w(TAG, "Unable to write message to database.",
                                        databaseError.toException());
                            }
                        }
                    });

        }
    }
}
