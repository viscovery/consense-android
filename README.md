# Get Started

This guide shows you how to integrate VidSense SDK into your video player app. You can also download the sample video player app from GitHub, and test video ads while playing the app's content video.

## Prerequisites

Before you begin, you'll need the following:

* Android Studio 1.0+
* Android 4.0+

## Add VidSense SDK to your video player app

1. Copy VidSense SDK to your project folder.

2. Add the following to your settings.gradle:

```groovy
include ':app', ':ad'
```

3. Add the following to your application-level build.gradle file:

```groovy
dependencies {
    ...
    compile project(':ad')
}
```

## Implement VidSense player interface

1. Implement interface methods in your video player:

```java
...
import com.viscovery.ad.AdSdkManager.AdSdkPlayer;

public class VideoPlayer extends VideoView implements AdSdkPlayer {
    ...
    @Override
    public void setVideoPath(String path) {
        // your implementation
    }

    @Override
    public void pause() {
        // your implementation
    }

    @Override
    public void resume() {
        // your implementation
    }

    @Override
    public int getCurrentPosition() {
        // your implementation
    }

    @Override
    public int getDuration() {
        // your implementation
    }
}
```

## Setup VidSense manager

1. Prepare a container view to render VidSense content

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <RelativeLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.viscovery.player.VideoPlayer
            android:id="@+id/player"
            android:layout_centerInParent="true"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />

        <FrameLayout
            android:id="@+id/container"
            android:layout_alignLeft="@id/player"
            android:layout_alignTop="@id/player"
            android:layout_alignRight="@id/player"
            android:layout_alignBottom="@id/player"
            android:layout_width="0dp"
            android:layout_height="0dp" />

    </RelativeLayout>

    <FrameLayout
        android:id="@+id/outstream"
        android:background="@android:color/darker_gray"
        android:layout_width="match_parent"
        android:layout_height="@dimen/outstream_height" />

</LinearLayout>
```

2. Initialize VidSense manager with your API key (sample API key below):

```java
...
import com.viscovery.ad.AdSdkManager;

public class MainActivity extends AppCompatActivity {
    ...
    private static final String API_KEY = "89494098-2877-38f8-b424-369ab8de602";

    ...
    private AdSdkManager mAdSdkManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final String path = getString(R.string.video_url);
        final ViewGroup container = (ViewGroup) findViewById(R.id.container);
        final ViewGroup outstream = (ViewGroup) findViewById(R.id.outstream);
        mController = new MediaController(this, false);
        mController.setAnchorView(container);
        mPlayer = (VideoPlayer) findViewById(R.id.player);
        mPlayer.setMediaController(mController);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnInfoListener(this);
        mAdSdkManager = new AdSdkManager(this, container, mPlayer, API_KEY);
        /*
         * You can use following code instead to test with mock ads:
         * mAdSdkManager = new AdSdkManager(this, container, mPlayer, API_KEY, true);
         */
        mAdSdkManager.setOutstreamContainer(outstream);
        mAdSdkManager.setVideoPath(path);
    }
}
```

3. Start VidSense manager on video player start:

```java
...
import android.media.MediaPlayer.OnInfoListener;

public class MainActivity extends AppCompatActivity implements OnInfoListener {
    ...
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                mAdSdkManager.start();
                break;
            default:
                break;
        }
        return false;
    }
}
```

## Download and run the sample video player app

1. Download the sample video player app from [GitHub](https://github.com/viscovery/viscovery-ad-sdk-android/).
2. Start Android Studio and select **Open an existing Android Studio project**, or if Android Studio is already running, select **File > New > Import Project**. Then choose viscovery-ad-sdk-android/build.gradle.
3. Run a Gradle sync by selecting **Tools > Android > Sync Project with Gradle Files**.
4. Ensure that the player app compiles and runs on a physical Android device or an Android Virtual Device using **Run > Run 'app'**. It's normal for the content video to take a few moments to load before playing.

## Request ads manually with Google IMA SDK

1. Use the following with your ads manager:

```java
private class DownloadAsyncTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "DownloadAsyncTask";
    private static final String API_KEY = "89494098-2877-38f8-b424-369ab8de602";
    private static final String URL_FORMAT =
            "https://vsp.viscovery.com/api/vmap?api_key=%s&video_url=%s&platform=mobile";

    @Override
    protected String doInBackground(String... params) {
        final byte[] data = params[0].getBytes();
        final String encoded = Base64.encodeToString(data, Base64.DEFAULT);
        final String url = String.format(URL_FORMAT, API_KEY, encoded);
        try {
            final HttpURLConnection connection =
                    (HttpURLConnection) new URL(url).openConnection();
            final int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                switch (code) {
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        Log.e(TAG, "Unauthorized");
                        break;
                    case HttpURLConnection.HTTP_NO_CONTENT:
                        Log.e(TAG, "Not found");
                        break;
                    default:
                        break;
                }
                return null;
            }

            final InputStream stream = connection.getInputStream();
            return new Scanner(stream).useDelimiter("\\A").next();
        } catch (IOException e) {
            final String message = e.getMessage();
            Log.e(TAG, message);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (result == null) {
            return;
        }

        try {
            final JSONObject object = new JSONObject(result);
            final String content = object.getString("context");
            final AdsRequest request = mSdkFactory.createAdsRequest();
            request.setAdsResponse(content);
            // your settings
            mAdsLoader.requestAds(request);
        } catch (JSONException e) {
            final String message = e.getMessage();
            Log.e(TAG, message);
        }
    }
}
```