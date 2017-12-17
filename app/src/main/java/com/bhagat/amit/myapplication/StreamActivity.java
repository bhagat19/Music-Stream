package com.bhagat.amit.myapplication;


import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    private ArrayList<Melody> responseMelodies;
    private TextView mSelectedMelodyTitle;
    private ImageView mSelectedMelodyImage;
    private int TOTAL_LIST_ITEMS;
    private static int NUM_ITEMS_PAGE = 5;
    private static final int DEFAULT_ITEMS = 10;
    private int pageCount;
    private int increment = 0;
    boolean firstTime = true;
    private boolean firstDefaultClick = false;

    private LinearLayoutManager layoutManager;

    private MediaPlayer mMediaPlayer;
    private ImageView mPlayerControl;
    private ProgressBar mProgressBar;

    private int firstVisibleItemPosition, lastVisibleItemPosition;

    private Button btnNext,btnPrev;

    private static final String LOG_TAG = StreamActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        mSelectedMelodyImage = findViewById(R.id.selected_track_image);
        mSelectedMelodyTitle = findViewById(R.id.selected_track_title);

        btnNext     = findViewById(R.id.btn_next);
        btnPrev     = findViewById(R.id.btn_prev);
        btnPrev.setEnabled(false);

        initMedia();

        this.melodies = new ArrayList<>();
        this.responseMelodies = new ArrayList<>();

//        final LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
//            @Override
//            public void onLayoutChildren(final RecyclerView.Recycler recycler, final RecyclerView.State state) {
//                super.onLayoutChildren(recycler, state);
//
//                if (firstTime) {
//                    final int firstVisibleItemPosition = findFirstVisibleItemPosition();
//                    final int lastVisibleItemPosition = findLastVisibleItemPosition();
//                    Log.d(LOG_TAG, "first pos: "+firstVisibleItemPosition);
//                    Log.d(LOG_TAG, "last pos: "+lastVisibleItemPosition);
//
//                    if (firstVisibleItemPosition != -1 && lastVisibleItemPosition != -1){
//                        NUM_ITEMS_PAGE = lastVisibleItemPosition - firstVisibleItemPosition;
//                        firstTime = false;
//                        Log.d(LOG_TAG, "items_page: "+NUM_ITEMS_PAGE);
//                    }else {
//                        NUM_ITEMS_PAGE = 5;
//                    }
//
//                }
//            }
//        };

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new StreamAdapter(this, melodies, this);
        mRecyclerView.setAdapter(mAdapter);

        loadData();
    }

    private void loadData(){

        OlaStudioService service = OlaStudio.getService();

        service.getRecentTracks().enqueue(new Callback<ArrayList<Melody>>() {
            @Override
            public void onResponse(Call<ArrayList<Melody>> call, Response<ArrayList<Melody>> response) {

                if (response.isSuccessful()) {
                    Log.d(LOG_TAG,"response: "+response.toString());
                    ArrayList<Melody> melodies = response.body();
                    if (melodies != null) {
                        Log.d(LOG_TAG, "melody not null"+melodies.size());
                        TOTAL_LIST_ITEMS = melodies.size();
                        loadMelodies(melodies);
//                        showMessage(melodies.get(0).getTitle());
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

    private void initMedia(){

        mMediaPlayer = new MediaPlayer();
        mPlayerControl = findViewById(R.id.player_control);

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(LOG_TAG, "onPrepared:");
                togglePlayPause();
                if (firstDefaultClick){
                    togglePlayPause();
                }

            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlayerControl.setImageResource(R.drawable.ic_play);
            }
        });

    }

    private void showMessage(String message) {
        Toast.makeText(StreamActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void loadMelodies(ArrayList<Melody> newMelodies){

        melodies.clear();
        melodies.addAll(newMelodies);
        responseMelodies.addAll(newMelodies);

//        if (firstTime) {
//            mAdapter.notifyDataSetChanged();
//            firstTime = false;
//        }

        paginateData();
    }

    @Override
    public void onMelodyItemClick(int position) {

        firstDefaultClick = false;

        Melody melody = mAdapter.getItem(position);
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

    private void paginateData(){

        int val = TOTAL_LIST_ITEMS % NUM_ITEMS_PAGE;
        val = (val == 0) ? 0 : 1;
        pageCount = TOTAL_LIST_ITEMS / NUM_ITEMS_PAGE + val;

        Log.d(LOG_TAG, "page count: "+pageCount);


        loadList(0);

        btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                increment++;
                loadList(increment);
                CheckEnable();
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                increment--;
                loadList(increment);
                CheckEnable();
            }
        });

    }

    /**
     * Method for enabling and disabling Buttons
     */
    private void CheckEnable()
    {
        if(increment + 1 == pageCount)
        {
            Toast.makeText(this, "You are on the last page", Toast.LENGTH_SHORT).show();
            btnNext.setEnabled(false);
            btnPrev.setEnabled(true);
            btnNext.setBackgroundColor(ContextCompat.getColor(this,R.color.grey_disabled));
            btnPrev.setBackgroundColor(ContextCompat.getColor(this,R.color.colorPrimary));


        }
        else if(increment == 0)
        {
            Toast.makeText(this, "You are on the first page", Toast.LENGTH_SHORT).show();
            btnPrev.setEnabled(false);
            btnNext.setEnabled(true);
            btnPrev.setBackgroundColor(ContextCompat.getColor(this,R.color.grey_disabled));
            btnNext.setBackgroundColor(ContextCompat.getColor(this,R.color.colorAccent));
        }
        else
        {
            btnPrev.setEnabled(true);
            btnNext.setEnabled(true);
        }
    }

    private void loadList(int number) {
        ArrayList<Melody> filteredList = new ArrayList<Melody>();

        int start = number * NUM_ITEMS_PAGE;
        for (int i = start; i < (start) + NUM_ITEMS_PAGE; i++) {
            if (i < responseMelodies.size()) {
                filteredList.add(responseMelodies.get(i));
            } else {
                break;
            }
        }

        Log.d(LOG_TAG, "filter list size: "+filteredList.size());

        melodies.clear();
        melodies.addAll(filteredList);
        mAdapter.notifyDataSetChanged();

//        mAdapter = new StreamAdapter(this,filteredList,this);
//        mRecyclerView.setAdapter(mAdapter);


        onMelodyItemClick(0);
        firstDefaultClick = true;

    }




}

