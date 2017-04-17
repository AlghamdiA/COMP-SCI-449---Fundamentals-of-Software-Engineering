package app.groupstudy.adapter;

import android.content.Context;
import android.net.ParseException;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import app.groupstudy.R;
import app.groupstudy.database.ChatGroup;
import app.groupstudy.helper.CircleTransform;


public class MyChatsRecyclerViewAdapter extends RecyclerView.Adapter<MyChatsRecyclerViewAdapter.ViewHolder> {

    private final List<ChatGroup> mValues;
    private static String today;
    private Context mContext;

    public MyChatsRecyclerViewAdapter(List<ChatGroup> items, Context mContext) {
        mValues = items;
        this.mContext = mContext;
        Calendar calendar = Calendar.getInstance();
        today = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        ChatGroup chatGroup = mValues.get(position);
        holder.subject.setText(chatGroup.getSubject());
        holder.timestamp.setText(getTimeStamp(chatGroup.getTimestamp()));

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        if (!TextUtils.isEmpty(chatGroup.getPhotoUrl())) {
            Glide.with(mContext).load(chatGroup.getPhotoUrl())
                    .thumbnail(0.5f)
                    .crossFade()
                    .transform(new CircleTransform(mContext))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imgProfile);
            holder.imgProfile.setVisibility(View.VISIBLE);
        } else {
            holder.imgProfile.setVisibility(View.GONE);
        }

        holder.description.setText(chatGroup.getLastMessage());
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView subject;
        public final TextView description, timestamp;
        public final ImageView imgProfile;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            subject = (TextView) view.findViewById(R.id.subject);
            description = (TextView) view.findViewById(R.id.description);
            timestamp = (TextView) view.findViewById(R.id.timestamp);
            imgProfile = (ImageView) view.findViewById(R.id.img_profile);
        }
    }

    public static String getTimeStamp(long dateStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp;
        today = today.length() < 2 ? "0" + today : today;

        SimpleDateFormat todayFormat = new SimpleDateFormat("dd");
        String dateToday = todayFormat.format(new Date(dateStr));
        format = dateToday.equals(today) ? new SimpleDateFormat("hh:mm a") : new SimpleDateFormat("MMM d, yy hh:mm a");
        String date1 = format.format(new Date(dateStr));
        timestamp = date1.toString();

        return timestamp;
    }

    public static String getTimeStamp1(long dateStr) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = "";

        today = today.length() < 2 ? "0" + today : today;

        try {
            Date date = new Date(dateStr);
            SimpleDateFormat todayFormat = new SimpleDateFormat("dd");
            String dateToday = todayFormat.format(date);
            format = dateToday.equals(today) ? new SimpleDateFormat("hh:mm a") : new SimpleDateFormat("dd LLL, hh:mm a");
            String date1 = format.format(date);
            timestamp = date1.toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timestamp;
    }
}
