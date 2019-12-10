package com.abhijeet.mp3player.helpers;

import android.media.MediaPlayer;

/**
 * Created by Rahul Kumar on 6/29/2017.
 */

public class Listeners {

    public interface LoadSongListener {
        void onSongLoaded();
    }

    public interface LoadImageListener {
        void onImageLoaded();
    }

    public interface MediaPlayerListener {
        void onMediaPlayerStarted(MediaPlayer mp);
    }
}
