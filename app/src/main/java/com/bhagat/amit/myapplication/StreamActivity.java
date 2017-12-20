package com.bhagat.amit.myapplication;


import android.Manifest;
import android.app.Activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.Calendar;


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

    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;
    public static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 112;

    private LinearLayoutManager layoutManager;

    private MediaPlayer mMediaPlayer;
    private ImageView mPlayerControl;
    private static ProgressBar mProgressBar;
    private ImageButton btnDownload;
    private int clickedPos;


    private Button btnNext, btnPrev;

    private static final String LOG_TAG = StreamActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        mSelectedMelodyImage = findViewById(R.id.selected_track_image);
        mSelectedMelodyTitle = findViewById(R.id.selected_track_title);

        btnNext = findViewById(R.id.btn_next);
        btnPrev = findViewById(R.id.btn_prev);
        btnPrev.setEnabled(false);

        btnDownload = findViewById(R.id.btn_download);

        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDownload(melodies.get(clickedPos).getMelodyUrl());
            }
        });

        initMedia();

        this.melodies = new ArrayList<>();
        this.responseMelodies = new ArrayList<>();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

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
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new StreamAdapter(this, melodies, this);
        mRecyclerView.setAdapter(mAdapter);

        if (isNetworkConnected()) {
            loadData();
        } else {
            Toast.makeText(this, "Device is not connected to internet", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadData() {

        OlaStudioService service = OlaStudio.getService();

        service.getRecentTracks().enqueue(new Callback<ArrayList<Melody>>() {
            @Override
            public void onResponse(Call<ArrayList<Melody>> call, Response<ArrayList<Melody>> response) {
                mProgressBar.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    Log.d(LOG_TAG, "response: " + response.toString());
                    ArrayList<Melody> melodies = response.body();
                    if (melodies != null) {
                        Log.d(LOG_TAG, "melody not null" + melodies.size());
                        TOTAL_LIST_ITEMS = melodies.size();
                        loadMelodies(melodies);

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

    private void initMedia() {

        mMediaPlayer = new MediaPlayer();
        mPlayerControl = findViewById(R.id.player_control);

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(LOG_TAG, "onPrepared:");
                togglePlayPause();
                if (firstDefaultClick) {
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

    private void loadMelodies(ArrayList<Melody> newMelodies) {

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

        clickedPos = position;

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
        }
        mMediaPlayer.reset();


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

    private void paginateData() {

        int val = TOTAL_LIST_ITEMS % NUM_ITEMS_PAGE;
        val = (val == 0) ? 0 : 1;
        pageCount = TOTAL_LIST_ITEMS / NUM_ITEMS_PAGE + val;

        Log.d(LOG_TAG, "page count: " + pageCount);


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
    private void CheckEnable() {
        if (increment + 1 == pageCount) {
//            Toast.makeText(this, "You are on the last page", Toast.LENGTH_SHORT).show();
            btnNext.setEnabled(false);
            btnPrev.setEnabled(true);
            btnNext.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_disabled));
            btnPrev.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));


        } else if (increment == 0) {
//            Toast.makeText(this, "You are on the first page", Toast.LENGTH_SHORT).show();
            btnPrev.setEnabled(false);
            btnNext.setEnabled(true);
            btnPrev.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_disabled));
            btnNext.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
        } else {
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

        Log.d(LOG_TAG, "filter list size: " + filteredList.size());

        melodies.clear();
        melodies.addAll(filteredList);
        mAdapter.notifyDataSetChanged();


        //Select first item by default
        onMelodyItemClick(0);
        firstDefaultClick = true;

    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null;

    }

    private void startDownload(String url) {
        new DownloadFileAsync(this).execute(url);
    }

     private static class DownloadFileAsync extends AsyncTask<String, String, String> {

         WeakReference<Activity> activityWeakReference;

         public DownloadFileAsync(Activity activity) {
             this.activityWeakReference = new WeakReference<Activity>(activity);
         }

         @Override
         protected void onPreExecute() {
             super.onPreExecute();
             Toast.makeText(activityWeakReference.get(),"Download statrted", Toast.LENGTH_SHORT).show();
             mProgressBar.setVisibility(View.VISIBLE);
         }

         @Override
         protected String doInBackground(String... aurl) {
             InputStream input = null;
             OutputStream output = null;
             HttpURLConnection conn = null;
             try {

                 URL url = new URL(aurl[0]);
                 conn = (HttpURLConnection) url.openConnection();

                 boolean redirect = false;

                 // normally, 3xx is redirect
                 int status = conn.getResponseCode();
                 if (status != HttpURLConnection.HTTP_OK) {
                     if (status == HttpURLConnection.HTTP_MOVED_TEMP
                             || status == HttpURLConnection.HTTP_MOVED_PERM
                             || status == HttpURLConnection.HTTP_SEE_OTHER)
                         redirect = true;
                 }

                 System.out.println("Response Code ... " + status);

                 if (redirect) {

                     // get redirect url from "location" header field
                     String newUrl = conn.getHeaderField("Location");

                     // open the new connnection again
                     conn = (HttpURLConnection) new URL(newUrl).openConnection();


                     System.out.println("Redirect to URL : " + newUrl);

                 }


//                 // expect HTTP 200 OK, so we don't mistakenly save error report
//                 // instead of the file
//                 if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
//                     return "Server returned HTTP " + conn.getResponseCode()
//                             + " " + conn.getResponseMessage();
//                 }


                 // download the file
                 input = new BufferedInputStream(conn.getInputStream(),8192);


                 boolean writePermission = checkWritePermission(activityWeakReference.get());
                 if (writePermission) {

                     String root = Environment.getExternalStorageDirectory().toString();

                     Log.d(LOG_TAG, "root:"+root);

                     File myDir = new File(root + "/studio/songs");
                     if (!myDir.exists()) {
                         myDir.mkdirs();
                     }

                     String timeStamp = String.valueOf(Calendar.getInstance().getTimeInMillis());

                     String fname = timeStamp + ".mp3";

                     Log.d(LOG_TAG, "root name: "+root+"/studio/songs" + fname);

                     File file = new File(myDir, fname);
                     if (!file.exists()) {
                         file.createNewFile();
                     }

//                     if (file.exists()) file.delete();

                     try {

                         output = new FileOutputStream(file.getAbsoluteFile());

                         byte[] buffer = new byte[1024];
                         int byteReaded = input.read(buffer);
                         while (byteReaded != -1) {
                             output.write(buffer, 0, byteReaded);
                             byteReaded = input.read(buffer);
                         }
                         output.flush();
                         output.close();
                         input.close();
                     } catch (Exception e) {
                         e.printStackTrace();
                     }
                 } else {
                     Toast.makeText(activityWeakReference.get(), "Permission denied", Toast.LENGTH_SHORT).show();
                 }
             } catch (Exception e) {
                 e.printStackTrace();
             }

             if (conn != null)
                 conn.disconnect();

             return null;
         }

         @Override
         protected void onPostExecute(String result) {
             super.onPostExecute(result);
             mProgressBar.setVisibility(View.GONE);
             Toast.makeText(activityWeakReference.get(),"Download completed", Toast.LENGTH_SHORT).show();
             Log.d(LOG_TAG, "result: " + result);
//             if (result != null) {
//                 Toast.makeText(activityWeakReference.get(), "File DownLoaded: " + result, Toast.LENGTH_LONG).show();
//                 Log.d(LOG_TAG, result);
//             }
//             else
//                 Toast.makeText(activityWeakReference.get(),"Error", Toast.LENGTH_SHORT).show();
//         }


         }
     }

    public static boolean checkWritePermission(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                } else {

                    ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
                return false;
            } else {

                return true;
            }
        } else {
            return true;
        }
    }



}

