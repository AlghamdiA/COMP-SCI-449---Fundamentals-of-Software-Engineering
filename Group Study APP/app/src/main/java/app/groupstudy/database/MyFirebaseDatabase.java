package app.groupstudy.database;


import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import app.groupstudy.R;
import app.groupstudy.app.MyApplication;

public class MyFirebaseDatabase {
    private String TAG = MyFirebaseDatabase.class.getSimpleName();
    FirebaseUser firebaseUser;
    FirebaseAuth mFirebaseAuth;
    private MyFirebaseDatabaseListener listener;

    public MyFirebaseDatabase(MyFirebaseDatabaseListener listener) {
        mFirebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = mFirebaseAuth.getCurrentUser();
        this.listener = listener;
    }

    public void insertCurrentUser() {
        firebaseUser = mFirebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("users");

            // creating firebaseUser object
            final User user = new User();
            user.setName(firebaseUser.getDisplayName());
            user.setPhotoUrl(firebaseUser.getPhotoUrl().toString());
            user.setEmail(firebaseUser.getEmail());

            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.hasChild(firebaseUser.getUid())) {
                        Log.e(TAG, "User not existed. Creating new");
                        // pushing firebaseUser to 'users' node using the userId
                        mDatabase.child(firebaseUser.getUid()).setValue(user);
                    } else {
                        Log.e(TAG, "User already existed. Skipping new.");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        } else {
            Log.e(TAG, "firebase current user is null!");
        }
    }

    public void insertGroup(final ChatGroup chatGroup, Map<String, Boolean> members, String path) {
        Log.e(TAG, "insertGroup");
        // adding chat node
        firebaseUser = mFirebaseAuth.getCurrentUser();
        if (firebaseUser == null)
            return;

        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        final String groupId = mDatabase.getReference("chats").push().getKey();
        chatGroup.setLastMessage("");
        chatGroup.setAdminId(firebaseUser.getUid());

        // create chatGroup node
        mDatabase.getReference("chats").child(groupId).setValue(chatGroup);

        // adding current user to members
        if (firebaseUser != null) {
            members.put(firebaseUser.getUid(), true);
        }

        // create members node
        mDatabase.getReference("members").child(groupId).setValue(members);


        // adding chat node to each user/chats
        for (String memberId : members.keySet()) {
            insertChatMemberNode(mDatabase, memberId, groupId);
        }

        // upload image upload
        if (!TextUtils.isEmpty(path)) {
            Uri uri = Uri.fromFile(new File(path));
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            StorageReference riversRef = storageRef.child("images/" + uri.getLastPathSegment());

            UploadTask uploadTask = riversRef.putFile(uri);

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    listener.onGroupCreated();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    @SuppressWarnings("VisibleForTests")
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();

                    // update chatGroup group image path
                    mDatabase.getReference("chats").child(groupId).child("photoUrl").setValue(downloadUrl.toString());
                    listener.onGroupCreated();
                }
            });
        } else {
            listener.onGroupCreated();
        }
    }

    private void insertChatMemberNode(final FirebaseDatabase mDatabase, final String memberId, final String groupId) {
        Map<String, Object> map = new HashMap<>();
        map.put(groupId, true);
        mDatabase.getReference("users").child(memberId).child("chats").updateChildren(map);
    }

    public void leaveGroup(final ChatGroup chatGroup) {
        firebaseUser = mFirebaseAuth.getCurrentUser();
        if (firebaseUser == null)
            return;

        // remove user from members node
        FirebaseDatabase.getInstance().getReference("members").child(chatGroup.getId())
                .child(firebaseUser.getUid()).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                // update chat member count
                FirebaseDatabase.getInstance().getReference("members").child(chatGroup.getId())
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                long count = 0;
                                if (dataSnapshot != null)
                                    count = dataSnapshot.getChildrenCount();

                                FirebaseDatabase.getInstance().getReference("chats").child(chatGroup.getId()).
                                        child("memberCount").setValue(count);

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }
        });

        // remove group from user/chats
        FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid()).
                child("chats").child(chatGroup.getId()).removeValue();


        Toast.makeText(MyApplication.getInstance().getApplicationContext(),
                MyApplication.getInstance().getApplicationContext().getString(R.string.msg_left_group) + " `" + chatGroup.getSubject() + "`",
                Toast.LENGTH_LONG).show();
    }

    public void joinGroup(final ChatGroup chatGroup) {
        firebaseUser = mFirebaseAuth.getCurrentUser();

        // adding users/chat node
        HashMap<String, Object> map = new HashMap<>();
        map.put(chatGroup.getId(), true);
        FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid())
                .child("chats").updateChildren(map);


        // adding members node
        map = new HashMap<>();
        map.put(firebaseUser.getUid(), true);
        FirebaseDatabase.getInstance().getReference("members").child(chatGroup.getId()).
                updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // update chat member count
                FirebaseDatabase.getInstance().getReference("members").child(chatGroup.getId())
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                long count = 0;
                                if (dataSnapshot != null)
                                    count = dataSnapshot.getChildrenCount();

                                FirebaseDatabase.getInstance().getReference("chats").child(chatGroup.getId()).
                                        child("memberCount").setValue(count);

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
            }
        });
    }

    public void deleteGroup(final ChatGroup chatGroup) {
        Log.e(TAG, "deleteGroup");
        firebaseUser = mFirebaseAuth.getCurrentUser();
        // update users/chat from all users
        FirebaseDatabase.getInstance().getReference("members").child(chatGroup.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String, Boolean> members = (Map<String, Boolean>) dataSnapshot.getValue();
                        if (members != null) {
                            for (Map.Entry<String, Boolean> entry : members.entrySet()) {
                                Log.e(TAG, "deleting group from: " + entry.getKey() + "/" + chatGroup.getId());
                                FirebaseDatabase.getInstance().getReference("users").child(entry.getKey())
                                        .child("chats").child(chatGroup.getId()).removeValue(new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        Log.e(TAG, "OnRemoved: " + databaseError + ", reference: " + databaseReference.getKey());
                                        // delete members node
                                        FirebaseDatabase.getInstance().getReference("members").child(chatGroup.getId()).removeValue();

                                        // delete chat
                                        FirebaseDatabase.getInstance().getReference("chats").child(chatGroup.getId()).removeValue();
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

        Toast.makeText(MyApplication.getInstance().getApplicationContext(),
                MyApplication.getInstance().getApplicationContext().getString(R.string.msg_delete_group) + " `" + chatGroup.getSubject() + "`",
                Toast.LENGTH_LONG).show();
    }

    public void updateChatGroupIcon(final ChatGroup chatGroup, String picturePath) {
        if (!TextUtils.isEmpty(picturePath)) {
            Uri uri = Uri.fromFile(new File(picturePath));
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            StorageReference riversRef = storageRef.child("images/" + uri.getLastPathSegment());

            UploadTask uploadTask = riversRef.putFile(uri);

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    listener.onGroupCreated();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    @SuppressWarnings("VisibleForTests")
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();

                    // update chatGroup group image path
                    FirebaseDatabase.getInstance().getReference("chats").child(chatGroup.getId())
                            .child("photoUrl").setValue(downloadUrl.toString());
                }
            });
        }
    }

    public interface MyFirebaseDatabaseListener {
        void onGroupCreated();

        void onGroupCreateFailed();
    }
}
