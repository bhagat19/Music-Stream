package com.bhagat.amit.myapplication;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;


import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.OkHttpDownloader;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import retrofit2.Retrofit;

/**
 * Created by amit on 16/12/17.
 */

public class StreamAdapter extends RecyclerView.Adapter implements Filterable {

    public static final int SELECTED_ITEM = 0;
    public static final int SEARCHABLE_ITEM = 1;

    private Context mContext;
    private OnMelodyItemListener mListener;

    private static final String LOG_TAG = StreamAdapter.class.getSimpleName();


    private ArrayList<Melody> melodyArrayList = new ArrayList<>();
    private ArrayList<Melody> mFilteredList = new ArrayList<>();

    public StreamAdapter(Context context, ArrayList<Melody> melodyList, OnMelodyItemListener listener){
        mContext = context;
        melodyArrayList = melodyList;
        this.mListener = listener;
        mFilteredList = melodyList;
    }



    public interface OnMelodyItemListener{
        void onMelodyItemClick(int position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.melody_list_item, parent, false);
        return new MelodyItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        ((MelodyItemViewHolder) holder).tvTitle.setText(mFilteredList.get(position).getTitle());
        ((MelodyItemViewHolder) holder).tvArtist.setText(mFilteredList.get(position).getArtists());
        Log.d("Adapter:", mFilteredList.get(position).getImageUrl());
//                Picasso.with(mContext).
//                        load(melodyArrayList.get(position).getImageUrl()).
//                        resize(48,48).
//                        into(((MelodyItemViewHolder)holder).ivCoverImage);

        Picasso.Builder builder = new Picasso.Builder(mContext);

        builder.listener(new Picasso.Listener() {
            @Override
            public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
                exception.printStackTrace();
                Log.d("Adapter: ", "image load failed: " + exception.toString());
            }
        });

        builder.downloader(new OkHttp3Downloader(mContext));

        builder.build().
                load(mFilteredList.get(position).getImageUrl()).
                resize(48, 48).
                into(((MelodyItemViewHolder) holder).ivCoverImage);


    }

    @Override
    public int getItemCount() {
        return mFilteredList.size();
    }

    private class MelodyItemViewHolder extends RecyclerView.ViewHolder{
        TextView tvTitle,tvArtist;
        ImageView ivCoverImage;

        private MelodyItemViewHolder(View itemView){
            super(itemView);
            tvTitle = itemView.findViewById(R.id.melody_title);
            tvArtist = itemView.findViewById(R.id.melody_artist);
            ivCoverImage = itemView.findViewById(R.id.melody_image);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   mListener.onMelodyItemClick(getAdapterPosition());
                }
            });
        }

    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {

                String charString = charSequence.toString();

                if (charString.isEmpty()) {
                    mFilteredList = melodyArrayList;
                } else {

                    ArrayList<Melody> filteredList = new ArrayList<>();

                    for (Melody melody : melodyArrayList) {

                        if (melody.getTitle().toLowerCase().contains(charString)) {
                            Log.d(LOG_TAG, "inside filter: "+melody.getTitle());

                            filteredList.add(melody);
                        }
                    }

                    mFilteredList = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mFilteredList;
                return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mFilteredList = (ArrayList<Melody>) filterResults.values;
                Log.d(LOG_TAG, "filter list: " + mFilteredList.size());
                notifyDataSetChanged();
            }
        };
    }

}
