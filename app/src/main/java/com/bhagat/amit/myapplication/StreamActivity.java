package com.bhagat.amit.myapplication;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StreamActivity extends AppCompatActivity implements StreamAdapter.OnMelodyItemListener {

    private RecyclerView mRecyclerView;
    private StreamAdapter mAdapter;
    private ArrayList<Melody> melodies;
    private TextView mSelectedMelodyTitle;
    private ImageView mSelectedMelodyImage;

    private MediaPlayer mMediaPlayer;
    private ImageView mPlayerControl;

    private static final String LOG_TAG = StreamActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        mSelectedMelodyImage = (ImageView) findViewById(R.id.selected_track_image);
        mSelectedMelodyTitle = (TextView) findViewById(R.id.selected_track_title);

        mMediaPlayer = new MediaPlayer();
        mPlayerControl = (ImageView)findViewById(R.id.player_control);

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                togglePlayPause();
            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlayerControl.setImageResource(R.drawable.ic_play);
            }
        });


        this.melodies = new ArrayList<>();

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new StreamAdapter(this, melodies, this);
        mRecyclerView.setAdapter(mAdapter);


        OlaStudioService service = OlaStudio.getService();
        service.getRecentTracks().enqueue(new Callback<ArrayList<Melody>>() {
            @Override
            public void onResponse(Call<ArrayList<Melody>> call, Response<ArrayList<Melody>> response) {

                if (response.isSuccessful()) {
                    Log.d(LOG_TAG,"response: "+response.toString());
                    ArrayList<Melody> melodies = response.body();
                    if (melodies != null) {
                        Log.d(LOG_TAG, "melody not null"+melodies.size());
                        loadMelodies(melodies);
                        showMessage(melodies.get(0).getTitle());
                    }
                } else {
                    showMessage("Error code " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ArrayList<Melody>> call, Throwable t) {
                showMessage("Network Error: " + t.getMessage());

            }
        });
    }

    private void showMessage(String message) {
        Toast.makeText(StreamActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void loadMelodies(ArrayList<Melody> newMelodies){
        melodies.clear();
        melodies.addAll(newMelodies);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onMelodyItemClick(int position) {

        Melody melody = melodies.get(position);
        mSelectedMelodyTitle.setText(melody.getTitle());

        Picasso.Builder builder = new Picasso.Builder(this);

        builder.listener(new Picasso.Listener() {
            @Override
            public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
                exception.printStackTrace();
                Log.d(LOG_TAG, "image load failed: " + exception.toString());
            }
        });

        builder.downloader(new OkHttp3Downloader(this));
        builder.build().
                load(melody.getImageUrl()).
                resize(48, 48).
                into(mSelectedMelodyImage);

        //handle mediaPlayer

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
        }

        try {
            mMediaPlayer.setDataSource(melody.getMelodyUrl());
            mMediaPlayer.prepareAsync();
        } catch (IOException | IllegalArgumentException | SecurityException | IllegalStateException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "exception caught: " + e.toString());
        }

        mPlayerControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });


    }

    private void togglePlayPause() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlayerControl.setImageResource(R.drawable.ic_play);
        } else {
            mMediaPlayer.start();
            mPlayerControl.setImageResource(R.drawable.ic_pause);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem search = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) search.getActionView();
        search(searchView);
        return true;
    }


    private void search(SearchView searchView) {

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }
}
