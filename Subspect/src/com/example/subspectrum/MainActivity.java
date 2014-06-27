/* 
 * The FFTbase.jave code is found at: 
 * http://www.wikijava.org/wiki/The_Fast_Fourier_Transform_in_Java_%28part_1%29
 * The author is Orlando Selenu and the code is released under GNU Free Documentation license
 * 
 * Permission is granted for anyone to copy, use, modify, or distribute this
 * program and accompanying programs and documents for any purpose, provided
 * this copyright notice is retained and prominently displayed, along with
 * a note saying that the original programs are available from the author.
 */
package com.example.subspectrum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;//录音模块
import android.media.MediaPlayer;//声音播放模块
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.view.Menu;

//import com.mathworks.toolbox.javabuilder.*;
//import sub.*;

public class MainActivity extends Activity {
	
	//parameters for recording
	private static final int RECORDER_BPP = 16;
	private static final int bytesPerSample = RECORDER_BPP/8;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    //private static final int RECORDER_SAMPLERATE = 44100; // 不同设备不一样，可能需要修改
    //private static final int RECORDER_SAMPLERATE = 11025; // 不同设备不一样，可能需要修改
    //private static final int RECORDER_SAMPLERATE = 22050; // 不同设备不一样，可能需要修改
    private static final int RECORDER_SAMPLERATE = 8000; // 不同设备不一样，可能需要修改
    //private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    //set a AudioRecord
    private AudioRecord recorder = null;
    private MediaPlayer mMediaPlayer;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private String recordFileName = null;
    private String recordFileName1 = null;			//denoised signal
    
    //buttons
	Button b_start, b_end, b_play, b_stop, b_denoised;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mMediaPlayer = new MediaPlayer();
		setButtonHandlers();
		enableButtons(false);		//start is blocked when doing record, stop is blocked when no record.
		enableButtonsPlaying(false);
		
		//確定錄音資料夾存在
		 File audioRecDir = new File("/sdcard/audioRecorder");
		 if(audioRecDir.exists()==false) audioRecDir.mkdir();
		
	}
	
	private void enableButton(int id,boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
	}

	private void enableButtons(boolean isRecording) {
		enableButton(R.id.button_start,!isRecording);
		enableButton(R.id.button_end,isRecording);
	}
	
	private void enableButtonsPlaying(boolean isPlaying){			//play and stop 
		enableButton(R.id.button_play,!isPlaying);
		enableButton(R.id.button_denoised,!isPlaying);
		enableButton(R.id.button_stop,isPlaying);
	}
	
	private void setButtonHandlers() {
		// TODO Auto-generated method stub
		//set button handlers
		b_start = (Button)findViewById(R.id.button_start);
		b_end = (Button)findViewById(R.id.button_end);
		b_play = (Button)findViewById(R.id.button_play);
		b_stop = (Button)findViewById(R.id.button_stop);
		b_denoised = (Button)findViewById(R.id.button_denoised);
		b_start.setOnClickListener(btnClick);
		b_end.setOnClickListener(btnClick);
		b_play.setOnClickListener(btnClick);
		b_stop.setOnClickListener(btnClick);
		b_denoised.setOnClickListener(btnClick);
	}
	
	private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        
        if(!file.exists()){
                file.mkdirs();
        }
        
        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
	}
		
	private String getDenoisedFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        
        if(!file.exists()){
                file.mkdirs();
        }
        
        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + "denoised" + AUDIO_RECORDER_FILE_EXT_WAV);
	}

	private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
        
        if(!file.exists()){
                file.mkdirs();
        }
        
        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);
        
        if(tempFile.exists())
                tempFile.delete();
        
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
	}
	
	private void startRecording(){
		//get buffer size
		// buffer size 每个手机不同，采样率不同，如果设置不好会出问题
		// 解决办法：修改采样率 和 通道值，要设置成 8000和MONO
	    //AppLog.logString("freq: " + RECORDER_SAMPLERATE);
		bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
		//AppLog.logString("bufferSize: " + bufferSize);
		if (bufferSize < 4096)
            bufferSize = 4096;
		//AppLog.logString("bufferSize 2: " + bufferSize);
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
		                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
		  if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                  //do something please
                 // Log.d("Recorder", "Audio recorder initialised at " + recorder.getSampleRate());
			  AppLog.logString("Recorder" + "Audior recorder initialised at " + recorder.getSampleRate());
                  //break;
          }
          else {
        	  	  recorder.stop();
                  recorder.release();
                  recorder = null;
          }
	
		  
        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
                
                @Override
                public void run() {
                	writeAudioDataToFile();				//to temp file, with .raw
                }
        },"AudioRecorder Thread");
        recordingThread.start();
	}
	
	private void writeAudioDataToFile(){
        byte data[] = new byte[bufferSize];
        
        //File sd = Environment.getExternalStorageDirectory();
        //boolean can_write = sd.canWrite();
        //AppLog.logString("can write: " + can_write);
        String filename = getTempFilename();
        AppLog.logString("TempFilename: " + filename);
        
        FileOutputStream os = null;
        
        try {
        		os = new FileOutputStream(filename);
        	//now attach OutputStream to the file object, instead of a String representation
        	//os = new FileOutputStream(outputFile);
                
        } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        
        int read = 0;
        
        if(null != os){
                while(isRecording){
                        read = recorder.read(data, 0, bufferSize);
                        //processing audio signal
                        //byte processedData[] = audioProcessing(data, bufferSize);
                        
                        if(AudioRecord.ERROR_INVALID_OPERATION != read){
                                try {
                                        os.write(data);
                                } catch (IOException e) {
                                        e.printStackTrace();
                                }
                        }
                }
                
                try {
                        os.close();
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }
        else
        {
        	AppLog.logString("create file failed");
        }
	}
	
	private byte[] audioProcessing(byte[] data, int bufferSize, double[] ham) {
		// TODO Auto-generated method stub
		byte processedData[] = new byte[bufferSize];
		//convert byte to double
        int length = bufferSize/2;
        
        double[] dbuffer =  new double[length];
        
        for (int i = 0, j = 0; i < bufferSize; i += 2 , j++) {
            //dbuffer[j] = (double)((data[i]) | data[i + 1] << 8);
            dbuffer[j] = (double)((short)(((data[i]) & 0xff) | ((data[i + 1] & 0xff) << 8)));
        }
        
        
        
        //convert double to Complex, and add hamming window
        double[] dbufferReal = new double[length];
        double[] dbufferImagery = new double[length];
        for(int i = 0; i < length; i++)
        {
        	dbufferReal[i] = dbuffer[i]*ham[i];
        	dbufferImagery[i] = 0;
        }
        
        //fft(for test)
        //direct FFT
        double[] transformedArray = FFTbase.fft(dbufferReal, dbufferImagery, true);
        double[] transformedReal = new double[length];
        double[] transformedImagery = new double[length];
        double[] cosTheta = new double[length];					//real
        double[] sinTheta = new double[length];					//imagery
        double[] transformedMagnitude = new double[length];
        for(int i = 0; i<length;i++)
        {
        	transformedReal[i] = transformedArray[2*i];
        	transformedImagery[i] = transformedArray[2*i+1];
        	transformedMagnitude[i] = Math.sqrt(transformedReal[i]*transformedReal[i]+transformedImagery[i]*transformedImagery[i]);
        	cosTheta[i] = transformedReal[i]/transformedMagnitude[i];
        	sinTheta[i] = transformedImagery[i]/transformedMagnitude[i];
        }
        
        
        //sub spectrum
        double threshold = 15;
        for(int i = 0; i < length; i++)
        {
        	//AppLog.logString("quiet-"+i+" "+transformedMagnitude[i]);
        	transformedMagnitude[i] -= threshold;
        	if(transformedMagnitude[i]<0)
        	{
        		transformedMagnitude[i] = 0;
        	}
        	transformedReal[i] = transformedMagnitude[i] * cosTheta[i];
        	transformedImagery[i] = transformedMagnitude[i] * sinTheta[i];
        }
        
        //inverse FFT
        transformedArray = FFTbase.fft(transformedReal, transformedImagery, false);
        
        //convert Complex back to double and remove hamming window
        for(int i = 0; i<length;i++)
        {
        	dbuffer[i] = transformedArray[2*i]/ham[i];
        }
        
        
        
        
        //convert double back to byte
        for (int i = 0, j = 0; j < length; j++,i+=2){
        	int temp = (int)dbuffer[j];
        	processedData[i] = (byte)(temp & 0xff);
        	processedData[i+1] = (byte)((temp >> 8) & 0xff);
        }
		return processedData;
	}

	//hamming window
	private double[] hamming(int length) {
		// TODO Auto-generated method stub
		double ham[] = new double[length];
		double pi = 3.1415926;
		if(length>0){
			for(int i = 0; i < length; i++)
			{
				ham[i] = 0.54 - 0.46 * Math.cos(2*pi*i/(length-1));
			}
			return ham;
		}
		else{
			return null;
		}
	}

	private void stopRecording(){
        if(null != recorder){
                isRecording = false;
                
                recorder.stop();
                recorder.release();
                
                recorder = null;
                recordingThread = null;
        }
        copyWaveFile(getTempFilename(),getFilename());
        deleteTempFile();
	}
	
	//delete temp file
	private void deleteTempFile() {
        File file = new File(getTempFilename());
        
        file.delete();
	}
	
	//convert the raw data into two wav files
	private void copyWaveFile(String inFilename,String outFilename){
		recordFileName = outFilename;
		
		recordFileName1 = getDenoisedFilename();
		
        FileInputStream in = null;
        FileOutputStream out = null;
        FileOutputStream outDenoised = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels;
        //check the RECORDER_CHANNELS state, channels = 1 for mono and channels = 2 for stereo
        if(RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO){channels = 1;}
        else if(RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_STEREO){channels = 2;}
        
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
        
        byte[] data = new byte[bufferSize];
        
        //generate a hamming window
        double[] ham = hamming(bufferSize/2);
        
        try {
                in = new FileInputStream(inFilename);
                out = new FileOutputStream(recordFileName);
                outDenoised = new FileOutputStream(recordFileName1);
                
                totalAudioLen = in.getChannel().size();
                totalDataLen = totalAudioLen + 36;
                
                AppLog.logString("File size: " + totalDataLen);
                
                WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                                longSampleRate, channels, byteRate);
                
                WriteWaveFileHeader(outDenoised, totalAudioLen, totalDataLen,longSampleRate, channels, byteRate);
                
                while(in.read(data) != -1){
                	byte processedData[] = audioProcessing(data, bufferSize, ham);
                	out.write(data);
                	outDenoised.write(processedData);
                }
                
                in.close();
                out.close();
                outDenoised.close();
        } catch (FileNotFoundException e) {
                e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
        }
	}
	
	//add .wav header to the file
	private void WriteWaveFileHeader(
		FileOutputStream out, long totalAudioLen,
		long totalDataLen, long longSampleRate, int channels,
		long byteRate) throws IOException {
    
	    byte[] header = new byte[44];
	    
	    header[0] = 'R';  // RIFF/WAVE header
	    header[1] = 'I';
	    header[2] = 'F';
	    header[3] = 'F';
	    header[4] = (byte) (totalDataLen & 0xff);
	    header[5] = (byte) ((totalDataLen >> 8) & 0xff);
	    header[6] = (byte) ((totalDataLen >> 16) & 0xff);
	    header[7] = (byte) ((totalDataLen >> 24) & 0xff);
	    header[8] = 'W';
	    header[9] = 'A';
	    header[10] = 'V';
	    header[11] = 'E';
	    header[12] = 'f';  // 'fmt ' chunk
	    header[13] = 'm';
	    header[14] = 't';
	    header[15] = ' ';
	    header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
	    header[17] = 0;
	    header[18] = 0;
	    header[19] = 0;
	    header[20] = 1;  // format = 1
	    header[21] = 0;
	    header[22] = (byte) channels;
	    header[23] = 0;
	    header[24] = (byte) (longSampleRate & 0xff);
	    header[25] = (byte) ((longSampleRate >> 8) & 0xff);
	    header[26] = (byte) ((longSampleRate >> 16) & 0xff);
	    header[27] = (byte) ((longSampleRate >> 24) & 0xff);
	    header[28] = (byte) (byteRate & 0xff);
	    header[29] = (byte) ((byteRate >> 8) & 0xff);
	    header[30] = (byte) ((byteRate >> 16) & 0xff);
	    header[31] = (byte) ((byteRate >> 24) & 0xff);
	    header[32] = (byte) (channels * RECORDER_BPP / 8);  // block align
	    header[33] = 0;
	    header[34] = RECORDER_BPP;  // bits per sample
	    header[35] = 0;
	    header[36] = 'd';
	    header[37] = 'a';
	    header[38] = 't';
	    header[39] = 'a';
	    header[40] = (byte) (totalAudioLen & 0xff);
	    header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
	    header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
	    header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

	    out.write(header, 0, 44);
	}
	
	private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                switch(v.getId()){
                        case R.id.button_start:{
                        	AppLog.logString("Start Recording");
                                
                        	enableButtons(true);
                        	startRecording();
                                                
                        	break;
                        }
                        case R.id.button_end:{
                            AppLog.logString("Stop Recording");
                                
                            enableButtons(false);
                            stopRecording();
                                
                            break;
                        }
                        case R.id.button_play:{
                        	//play the wav file that has been processed
                        	AppLog.logString("play");
                        	isPlaying = true;
                        	enableButtonsPlaying(isPlaying);
                        	playMusic(recordFileName);			//play original sound
                        	break;
                        }
                        case R.id.button_denoised:{
                        	AppLog.logString("play denoised");
                        	isPlaying = true;
                        	enableButtonsPlaying(isPlaying);
                        	playMusic(recordFileName1);		//play denoised sound
                        	break;
                        }
                        case R.id.button_stop:{
                        	AppLog.logString("stop");
                        	isPlaying = false;
                        	enableButtonsPlaying(isPlaying);
                        	if(mMediaPlayer.isPlaying()){
                        		mMediaPlayer.reset();
                        	}
                        	break;
                        }
                }
        }
	}; 
	
	//create menu response
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected void playMusic(String filePath) {
		// TODO Auto-generated method stub
		try{
			if(filePath!=null){
				mMediaPlayer.reset();
				mMediaPlayer.setDataSource(filePath);
				mMediaPlayer.prepare();
				mMediaPlayer.start();
				mMediaPlayer.setOnCompletionListener(new 
				OnCompletionListener()
				{
					public void onCompletion(MediaPlayer arg0) {
						// TODO Auto-generated method stub
						isPlaying = false;
						enableButtonsPlaying(isPlaying);
					}
					
				});
			}
			else{
				AppLog.logString("filepath == null");
			}
		}catch(IOException e){}
	}
	
}
