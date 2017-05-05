# Get Started

This guide shows you how to integrate ConSense SDK into your video player app. You can also download the sample video player app from GitHub, and test video ads while playing the app's content video.

## Prerequisites

Before you begin, you'll need the following:

* Android Studio 1.0+
* Android 2.3+

## Add ConSense SDK to your video player app

1. Copy ConSense SDK to your project folder.

2. Add the following to your settings.gradle:

```groovy
include ':app', ':consense'
```

3. Add the following to your application-level build.gradle file:

```groovy
dependencies {
    ...
    compile project(':consense')
}
```

## Implement ConSense player interface

1. Implement interface methods in your video player:

```java
...
import com.viscovery.consense.ConsenseManager.ConsensePlayer;

public class VideoPlayer extends VideoView implements ConsensePlayer {
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

## Setup ConSense manager

1. Prepare a container view to render ConSense content

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/container"
    android:background="@android:color/black"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.viscovery.player.VideoPlayer
        android:id="@+id/player"
        android:layout_gravity="center"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</FrameLayout>
```

2. Initialize ConSense manager with your API key (sample API key below):

```java
...
import com.viscovery.consense.ConsenseManager;

public class MainActivity extends AppCompatActivity {
    ...
    private static final String API_KEY = "89494098-2877-38f8-b424-369ab8de602";

    ...
    private ConsenseManager mConsenseManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final String path = getString(R.string.video_url);
        final ViewGroup container = (ViewGroup) findViewById(R.id.container);
        final MediaController controller = new MediaController(this, false);
        controller.setAnchorView(container);
        mPlayer = (VideoPlayer) findViewById(R.id.player);
        mPlayer.setMediaController(controller);
        mPlayer.setOnInfoListener(this);
        mConsenseManager = new ConsenseManager(this, container, mPlayer, API_KEY);
        mConsenseManager.setVideoPath(path);
    }
}
```

3. Start ConSense manager on video player start:

```java
...
import android.media.MediaPlayer.OnInfoListener;

public class MainActivity extends AppCompatActivity implements OnInfoListener {
    ...
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                mConsenseManager.start();
                break;
            default:
                break;
        }
        return false;
    }
}
```

## Download and run the sample video player app

1. Download the sample video player app from [GitHub](https://github.com/viscovery/consense-android/).


2. Start Android Studio and select **Open an existing Android Studio project**, or if Android Studio is already running, select **File > New > Import Project**. Then choose consense-android/build.gradle.
3. Run a Gradle sync by selecting **Tools > Android > Sync Project with Gradle Files**.
4. Ensure that the player app compiles and runs on a physical Android device or an Android Virtual Device using **Run > Run 'app'**. It's normal for the content video to take a few moments to load before playing.