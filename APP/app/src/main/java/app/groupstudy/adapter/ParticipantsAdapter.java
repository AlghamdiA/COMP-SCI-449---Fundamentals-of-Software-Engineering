package app.groupstudy.adapter;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import app.groupstudy.R;
import app.groupstudy.database.User;
import app.groupstudy.helper.CircleTransform;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsAdapter.MyViewHolder> {

    private List<User> participants;
    private Context context;
    private List<Integer> selectedIndexes;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView name;
        public ImageView profilePic;
        public CheckBox checkBox;

        public MyViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.name);
            profilePic = (ImageView) view.findViewById(R.id.img_profile);
            checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        }
    }


    public ParticipantsAdapter(Context context, List<User> participants) {
        this.context = context;
        this.participants = participants;
        selectedIndexes = new ArrayList<>();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.participant_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        User user = participants.get(position);
        holder.name.setText(user.getName());

        if (!TextUtils.isEmpty(user.getPhotoUrl())) {
            Glide.with(context).load(user.getPhotoUrl())
                    .thumbnail(0.5f)
                    .crossFade()
                    .transform(new CircleTransform(context))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.profilePic);
        }
        holder.checkBox.setChecked(selectedIndexes.contains(position));
    }

    public void toggleSelectedRow(int position) {
        if (!selectedIndexes.contains(position))
            selectedIndexes.add(position);
        else
            selectedIndexes.remove(Integer.valueOf(position));

        notifyDataSetChanged();
    }

    public void resetSelectedItems() {
        selectedIndexes.clear();
        notifyDataSetChanged();
    }

    public List<Integer> getSelectedIndexes() {
        return selectedIndexes;
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }
}
