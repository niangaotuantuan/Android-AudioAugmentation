package com.example.subspectrum;

import android.util.Log;

public class AppLog {
        private static final String APP_TAG = "AudioRecorder";
        
        public static int logString(String message){
                return Log.i(APP_TAG,message);
        }
}