package cs121.homework3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

import cs121.homework3.GSON.Gsonstuff;
import cs121.homework3.GSON.LocationData;
import cs121.homework3.GSON.ResultList;
import cs121.homework3.Settings.SettingsActivity;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class ChatActivity extends AppCompatActivity {

    private LocationData locationData = LocationData.getLocationData();

    ListView scrollview;
    MyAdapter adapter2;


    private List<ResultList> resultlist;
    private String result;
    private String timestamp;
    private String message;
    private String nickname;
    private String messageId;
    private String userId;

    double finallatitude;
    double finallongitude;
    double latitude;
    double longitude;
    private String client_userId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Get lat and long
        latitude = locationData.getLocation().getLatitude();
        longitude = locationData.getLocation().getLongitude();
        //My settings has an option to spoof lcoation, so the actual location is kept as finallat and finallong
        finallatitude = latitude;
        finallongitude = longitude;
        Log.d("Location", "Lat: " + latitude + " || Long: " + longitude);

        //Retrofit Stuff
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://luca-teaching.appspot.com/localmessages/default/")
                .addConverterFactory(GsonConverterFactory.create())    //parse Gson string
                .client(httpClient)    //add logging
                .build();
        //End of Retrofit stuff

        //Get data on startup
        contentRefresh(retrofit);

        //Two floating action buttons are used: one to refresh, the other to send messages
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendmessage(retrofit);
            }
        });

        FloatingActionButton fabmini = (FloatingActionButton) findViewById(R.id.fabmini);
        fabmini.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contentRefresh(retrofit);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //Opens the settings activity
            Intent intentsettings = new Intent(this, SettingsActivity.class);
            startActivity(intentsettings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //This function is called when refreshing
    public void contentRefresh(Retrofit retrofit) {
        GetService service = retrofit.create(GetService.class);

        //Gets nickname and userId from preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        nickname = settings.getString("username_text", null);
        client_userId = settings.getString("user_id", null);

        //Creates messageId string
        SecureRandomString srs = new SecureRandomString();
        messageId = srs.nextString();

        //I have a spoofmode in my settings, which can spoof another person's userId and nickname
        if (settings.getBoolean("spoofmode_switch", true)) {
            client_userId = settings.getString("spoofed_user_id", null);
            nickname = settings.getString("spoofed_username", null);
        }

        //I have another spoofmode in my settings to spoof the location of Classroom Unit 1
        latitude = finallatitude;
        longitude = finallongitude;
        if (settings.getBoolean("spoofmode_location_switch", true)) {
            //Location of Classroom Unit 1
            latitude = 36.998097;
            longitude = -122.056881;
        }

        //Retrofit stuff
        Call<Gsonstuff> queryResponseCall =
                service.get_messages(latitude, longitude, client_userId);


        //Call retrofit asynchronously
        queryResponseCall.enqueue(new Callback<Gsonstuff>() {
            @Override
            public void onResponse(Response<Gsonstuff> response) {
                View parentView = findViewById(R.id.mainrelativelayout);
                if (response.code() == 500) {
                    //Snackbar for 500 ERROR
                    makeErrorSnackbar(parentView, "Error Code 500");
                    return;
                }
                if (response.body().resultList == null) {
                    //Snackbar for Server Error
                    makeErrorSnackbar(parentView, "Server Error");
                    return;
                }
                if (response.body().result.equals("ok")) {
                    //Toast for on success
                    Toast toast = Toast.makeText(ChatActivity.this, "Content Refreshed", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();

                    //Update variables with new data
                    resultlist = response.body().resultList;
                    //Reverse the list, so that newest is on bottom
                    Collections.reverse(resultlist);

                    //Adapter stuff for the listview
                    adapter2 = new MyAdapter(ChatActivity.this, R.layout.list_element_yourmessage, R.layout.list_element_mymessage, resultlist, client_userId);
                    scrollview = (ListView) findViewById(R.id.scrollview);
                    scrollview.setAdapter(adapter2);
                    adapter2.notifyDataSetChanged();

                    //Function that scrolls the list to the bottom
                    scrollMyListViewToBottom();

                } else {
                    //Snackbar for Other Error
                    makeErrorSnackbar(parentView, "Other Error");
                    return;
                }
            }

            @Override
            public void onFailure(Throwable t) {
                // Log error here since request failed
                Log.d("Error", "onFailure");
            }

        });
    }


    //This function is called when sending a message
    public void sendmessage(Retrofit retrofit) {
        SetService service = retrofit.create(SetService.class);
        final Retrofit retrofit2 = retrofit;

        //Gets message from the edittext
        EditText messagetext = (EditText) findViewById(R.id.input_message);
        message = messagetext.getText().toString();

        //I have a spoofmode in my settings, which can spoof another person's userId and nickname
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getBoolean("spoofmode_switch", true)) {
            client_userId = settings.getString("spoofed_user_id", null);
            nickname = settings.getString("spoofed_username", null);
        }

        //I have a spoofmode in my settings, which can spoof another person's userId and nickname
        latitude = finallatitude;
        longitude = finallongitude;
        if (settings.getBoolean("spoofmode_location_switch", true)) {
            latitude = 36.998097;
            longitude = -122.056881;
        }

        //Retrofit Stuff
        Call<Gsonstuff> queryResponseCall =
                service.post_message(latitude, longitude, client_userId, nickname, message, messageId);

        messagetext.setText("Sending Message...");

        //Call retrofit asynchronously
        queryResponseCall.enqueue(new Callback<Gsonstuff>() {
            @Override
            public void onResponse(Response<Gsonstuff> response) {
                View parentView = findViewById(R.id.mainrelativelayout);
                if (response.code() == 500) {
                    //Snackbar for 500 ERROR
                    makeErrorSnackbar(parentView, "Error Code 500");
                    return;
                }
                if (response.body().resultList == null) {
                    //Snackbar for Server Error
                    makeErrorSnackbar(parentView, "Server Error");
                    return;
                }
                if (response.body().result.equals("ok")) {
                    //Toast for on success
                    Toast toast = Toast.makeText(ChatActivity.this, "Message Sent", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.TOP, 0, 0);
                    toast.show();

                    EditText messagetext2 = (EditText) findViewById(R.id.input_message);
                    messagetext2.setText("");

                    contentRefresh(retrofit2);

                } else {
                    //Snackbar for Other Error
                    makeErrorSnackbar(parentView, "Other Error");
                    return;
                }
            }

            @Override
            public void onFailure(Throwable t) {
                // Log error here since request failed
                Log.d("Error", "onFailure");
            }

        });
    }

    public void Addtobottomoflist () {

    }

    //Function to make an error snackbar when there is an error
    public void makeErrorSnackbar(View parentView, final String ErrorToast) {
        Snackbar.make(parentView, "ERROR", Snackbar.LENGTH_LONG)
                .setActionTextColor(Color.RED)
                .setAction("See Details", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(
                                ChatActivity.this,
                                ErrorToast,
                                Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    //Used for getting the list of messages
    public interface GetService {
        @GET("get_messages")
        Call<Gsonstuff> get_messages(@Query("lat") double latitude,
                                     @Query("lng") double longitude,
                                     @Query("user_id") String client_user_id
        );
    }

    //Used for posting a message
    public interface SetService {
        @GET("post_message")
        Call<Gsonstuff> post_message(@Query("lat") double latitude,
                                     @Query("lng") double longitude,
                                     @Query("user_id") String client_user_id,
                                     @Query("nickname") String nickname,
                                     @Query("message") String message,
                                     @Query("message_id") String message_id
        );
    }

    //Taken from the Professor's code
    public final class SecureRandomString {
        private SecureRandom random = new SecureRandom();

        public String nextString() {
            return new BigInteger(130, random).toString(32);
        }

    }


    //This function calls the scollMyListViewToBottom function with a delay
    //Used as the onclick for writing a message, that way when the keyboard pops up, it will keep the chat bottom
    //above the keyboard
    public void scrolltobottom(View v) {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                scrollMyListViewToBottom();
            }
        }, 300);
    }

    //
    //This function is to scoll to the bottom of the list
    //This function was taken from:
    //http://stackoverflow.com/questions/3606530/listview-scroll-to-the-end-of-the-list-after-updating-the-list
    //
    private void scrollMyListViewToBottom() {
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                scrollview.setSelection(adapter2.getCount() - 1);
            }
        });
    }
    //
    //
    //

}
