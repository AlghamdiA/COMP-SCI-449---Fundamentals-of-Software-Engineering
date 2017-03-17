package app.groupstudy.database;


import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

public class MyFirebaseDatabase {
    private String TAG = MyFirebaseDatabase.class.getSimpleName();
    FirebaseUser firebaseUser;
    FirebaseAuth mFirebaseAuth;
    private MyFirebaseDatabaseListener listener;

    public MyFirebaseDatabase(MyFirebaseDatabaseListener listener) {
        mFirebaseAuth = FirebaseAuth.getInstance();
        this.listener = listener;
    }

    public void insertCurrentUser() {
        firebaseUser = mFirebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("users");

            // creating firebaseUser object
            User user = new User();
            user.setName(firebaseUser.getDisplayName());
            user.setPhotoUrl(firebaseUser.getPhotoUrl().toString());
            user.setEmail(firebaseUser.getEmail());

            // pushing firebaseUser to 'users' node using the userId
            mDatabase.child(firebaseUser.getUid()).setValue(user);
        }
    }

    public void insertGroup(final Chat chat, Map<String, Boolean> members, String path) {
        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        final String id = mDatabase.getReference("chats").push().getKey();

        // create chat node
        mDatabase.getReference("chats").child(id).setValue(chat);

        // create members node
        mDatabase.getReference("members").child(id).setValue(members);

        // store chat under user node
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Boolean> group = new HashMap<>();
        group.put(id, true);
        mDatabase.getReference("users").child(userId).child("chats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Boolean> data = (Map<String, Boolean>) dataSnapshot.getValue();
                if (data == null)
                    data = new HashMap<>();
                data.put(id, true);
                mDatabase.getReference("users").child(userId).child("chats").setValue(data);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // upload file
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

                    // update chat group image path
                    mDatabase.getReference("chats").child(id).child("photoUrl").setValue(downloadUrl.toString());
                    listener.onGroupCreated();
                }
            });
        } else {
            listener.onGroupCreated();
        }
    }

    public interface MyFirebaseDatabaseListener {
        void onGroupCreated();

        void onGroupCreateFailed();
    }
}
