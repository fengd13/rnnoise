package com.fryant.denoise;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import java.util.Timer;
import java.util.TimerTask;
//import android.widget.TextView;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
public class MainActivity extends AppCompatActivity
        implements View.OnClickListener {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("denoise");
    }
    public native int rnnoise(String infile,String outfile);


    private final int REQ_PERMISSION_AUDIO = 0x01;
    private Button mRecord, mPlay,mTran,mPlay2,mRealtimeTran;
    private String path=Environment.getExternalStorageDirectory().getAbsolutePath()+ "/pauseRecordDemo/";
    private File mAudioFile = new File(path+"before.pcm");
    private File mAudioFile2 = new File(path+"after.pcm");

    //used to real time tran.
    private File mAudioBuffer_before1 = new File(path+"bb_1.pcm");
    private File mAudioBuffer_before2 = new File(path+"bb_2.pcm");
    private File mAudioBuffer_after1 = new File(path+"ba_1.pcm");
    private File mAudioBuffer_after2 = new File(path+"ba_2.pcm");

    private Handler handler=null;
    private Thread mCaptureThread = null;
    private boolean mIsRecording,mIsPlaying,mIsRealTimeTraning;
    private int mFrequence = 48000;
    private int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int mPlayChannelConfig = AudioFormat.CHANNEL_IN_DEFAULT;
    private int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    private PlayTask mPlay_task;
    private RecordTask mRecord_task;
    private TranTask mTran_task;
    private RealTimeTranTask mRealTimeTran_task; //一个类

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler=new Handler();
        mRecord = (Button) findViewById(R.id.audio_Record);
        mPlay = (Button) findViewById(R.id.audio_paly);
        mTran=(Button) findViewById(R.id.audio_tran);
        mPlay2=(Button) findViewById(R.id.audio_play2);
        mRealtimeTran=(Button) findViewById(R.id.rt_audio_play);

        mPlay.setEnabled(false);
        mPlay2.setEnabled(false);
        mTran.setEnabled(false);

        mRecord.setOnClickListener(this);
        mPlay.setOnClickListener(this);
        mTran.setOnClickListener(this);
        mPlay2.setOnClickListener(this);
        mRealtimeTran.setOnClickListener(this);


    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.audio_Record:
                if (mRecord.getTag() == null) {
                    startAudioRecord();
                } else {
                    stopAudioRecord();
                }
                break;
            case R.id.audio_paly:
                if (mPlay.getTag() == null) {
                    startAudioPlay(mAudioFile);
                } else {
                    stopAudioPlay(mAudioFile);
                }
                break;
            case R.id.audio_play2:
                if (mPlay2.getTag() == null) {
                    startAudioPlay2(mAudioFile2);
                } else {
                    stopAudioPlay2(mAudioFile2);
                }
                break;
            case R.id.audio_tran:
                if (mTran.getTag() == null) {
                    startAudioTran();
                }
                break;
            case R.id.rt_audio_play:
                if (mRealtimeTran.getTag() == null) {
                    startRealTimeAudioTran();
                }
                else
                {
                    stopRealTimeTran();
                }
                break;

        }
    }

    private void startAudioRecord() {
        if (checkPermission()) {
            PackageManager packageManager = this.getPackageManager();
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                showToast("This device doesn't have a mic!");
                return;
            }
            mRecord.setTag(this);
            mRecord.setText("stop");
            mPlay.setEnabled(false);

            File fpath = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/pauseRecordDemo");
            if (!fpath.exists()) {
                fpath.mkdirs();
            }
            mRecord_task = new RecordTask();
            mRecord_task.execute();

            showToast("录音开始");

        } else {
            requestPermission();
        }
    }

    private void stopAudioRecord() {

        mIsRecording = false;

        mRecord.setTag(null);
        mRecord.setText("录制");
        mPlay.setEnabled(true);
        mTran.setEnabled(true);
        showToast("录音完毕");
    }
    private void stopRealTimeTran() {

        mIsRealTimeTraning = false;
        mRealtimeTran.setTag(null);
        mRealtimeTran.setText("实时转换");
        mRealtimeTran.setEnabled(true);
        showToast("录音完毕");
    }

    private void startAudioPlay(File A) {
        mPlay.setTag(this);
        mPlay.setText("停止");

        mPlay_task = new PlayTask(A);
        mPlay_task.execute();

        showToast("开始播放录音音频");
    }
    private void startAudioPlay2(File A) {
        mPlay2.setTag(this);
        mPlay2.setText("停止");

        mPlay_task = new PlayTask(A);
        mPlay_task.execute();

        showToast("开始播放处理后音频");
    }
    private void startAudioTran() {
        mTran.setTag(this);
        mTran.setText("转换中");
        mTran.setEnabled(false);

        mTran_task= new TranTask();
        mTran_task.execute();

        showToast("降噪开始");
    }

    private void startRealTimeAudioTran() {
        mRealtimeTran.setTag(this);
        mRealtimeTran.setText("停止实时转换");

        mRealTimeTran_task= new RealTimeTranTask();
        mRealTimeTran_task.execute();

        showToast("实时处理开始");
    }

    private void stopAudioPlay(File A) {

        mIsPlaying = false;

        mPlay.setTag(null);
        mPlay.setText("播放录音音频");

    }
    private void stopAudioPlay2(File A) {

        mIsPlaying = false;

        mPlay2.setTag(null);
        mPlay2.setText("播放处理后音频");

    }


    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, REQ_PERMISSION_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQ_PERMISSION_AUDIO:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        showToast("Permission Granted");
                    } else {
                        showToast("Permission  Denied");
                    }
                }
                break;
        }
    }

    public void showToast(String ms) {
        final Toast toast=Toast.makeText(this, ms, Toast.LENGTH_LONG);
        int cnt=500;
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                toast.show();
            }
        }, 0, 3000);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                toast.cancel();
                timer.cancel();
            }
        }, cnt );
    }

    public static void shortToByte_LH(short shortVal, byte[] b) {
        b[0] = (byte) (shortVal & 0xff);
        b[1] = (byte) (shortVal >> 8 & 0xff);
    }
    public static short byteToShort_HL(byte[] b)
    {
        short result;
        result = (short)((((b[0]) << 8) & 0xff00 | b[1] & 0x00ff));
        return result;
    }


    class RecordTask extends AsyncTask<Void,Integer,Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            mIsRecording = true;
            try {
                // 开通输出流到指定的文件
                DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(mAudioFile)));
                // 根据定义好的几个配置，来获取合适的缓冲大小
                int bufferSize = AudioRecord.getMinBufferSize(mFrequence,
                        mChannelConfig, mAudioEncoding);

                // 实例化AudioRecord
                AudioRecord Record = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, mFrequence,
                        mChannelConfig, mAudioEncoding, bufferSize);
                // 定义缓冲
                short[] buffer = new short[bufferSize];


                // 开始录制
                Record.startRecording();


                int r = 0; // 存储录制进度
                byte[] b=new byte[2];
                // 定义循环，根据isRecording的值来判断是否继续录制
                while (mIsRecording) {
                    // 从bufferSize中读取字节，返回读取的short个数
                    int bufferReadResult = Record
                            .read(buffer, 0, buffer.length);
                    // 循环将buffer中的音频数据写入到OutputStream中
                    for (int i = 0; i < bufferReadResult; i++) {
                        shortToByte_LH(buffer[i], b);

                        dos.writeShort(byteToShort_HL(b));
                    }
                    publishProgress(new Integer(r)); // 向UI线程报告当前进度
                    r++; // 自增进度值
                }
                // 录制结束
                Record.stop();
                Log.i("slack", "::" + mAudioFile.length());
                dos.close();
            } catch (Exception e) {
                // TODO: handle exception
                Log.e("slack", "::" + e.getMessage());
            }
            return null;
        }


        // 当在上面方法中调用publishProgress时，该方法触发,该方法在UI线程中被执行
        protected void onProgressUpdate(Integer... progress) {
            //
        }


        protected void onPostExecute(Void result) {

        }

    }
    class PlayTask extends AsyncTask<Void,Void,Void> {
        File AudioFile;

        PlayTask(File A) {
            AudioFile = A;
        } //初始化

        @Override

        protected Void doInBackground(Void... arg0) {
            mIsPlaying = true;
            int bufferSize = AudioRecord.getMinBufferSize(mFrequence,
                    mChannelConfig, mAudioEncoding);
            Log.v("noisemain", "currentX=" + bufferSize);
            short[] buffer = new short[bufferSize];
            try {
                // 定义输入流，将音频写入到AudioTrack类中，实现播放
                DataInputStream dis = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(AudioFile)));
                // 实例AudioTrack
                AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC,
                        mFrequence,
                        mPlayChannelConfig, mAudioEncoding, bufferSize,
                        AudioTrack.MODE_STREAM);
                // 开始播放
                track.play();
                byte [] b=new byte[2];
                // 由于AudioTrack播放的是流，所以，我们需要一边播放一边读取
                while (mIsPlaying && dis.available() > 0) {
                    int i = 0;
                    while (dis.available() > 0 && i < buffer.length) {
                        //buffer[i] = dis.readShort();
                        shortToByte_LH(dis.readShort(),b);
                        buffer[i] =byteToShort_HL(b);
                        i++;
                    }
                    // 然后将数据写入到AudioTrack中
                    track.write(buffer, 0, buffer.length);
                }


                // 播放结束
                track.stop();
                dis.close();
                mIsPlaying = false;
                mPlay.setTag(null);
                mPlay.setText("播放录音音频");
                mPlay2.setTag(null);
                mPlay2.setText("播放处理后音频");
            } catch (Exception e) {
                // TODO: handle exception
                Log.e("slack", "error:" + e.getMessage());
            }
            return null;
        }


        protected void onPostExecute(Void result) {

        }


        protected void onPreExecute() {

        }
    }
    class TranTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                new Thread(){
                    public void run(){
                        rnnoise(Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/pauseRecordDemo/"+mAudioFile.getName(),Environment.getExternalStorageDirectory()
                                .getAbsolutePath() + "/pauseRecordDemo/"+mAudioFile2.getName());
                        handler.post(runnableUi);
                    }
                }.start();



            } catch (Exception e) {
                // TODO: handle exception
                Log.e("slack", "error:" + e.getMessage());
            }
            return null;
        }


        Runnable   runnableUi=new  Runnable(){
            @Override
            public void run() {
                //更新界面
                mTran.setTag(null);
                mTran.setEnabled(true);
                mTran.setText("降噪处理");
                mPlay2.setEnabled(true);
                showToast("降噪完毕");
            }

        };
        protected void onPostExecute(Void result) {

        }


        protected void onPreExecute() {

        }
    }

    class RealTimeTranTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                // 根据定义好的几个配置，来获取合适的缓冲大小 缓冲越大 延时越长 效果越好
                int bufferSize = AudioRecord.getMinBufferSize(mFrequence,
                        mChannelConfig, mAudioEncoding)*10;//可以改为*10
                //bufferSize=480000;
                AudioRecord Record = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, mFrequence,
                        mChannelConfig, mAudioEncoding, bufferSize);
                AudioTrack Track = new AudioTrack(AudioManager.STREAM_MUSIC,
                        mFrequence,
                        mPlayChannelConfig, mAudioEncoding, bufferSize,
                        AudioTrack.MODE_STREAM);
                Record.startRecording();//开始录制
                Track.play();//开始播放


                // 定义缓冲
                short[] buffer = new short[bufferSize];
                short[] tmpBuf = new short[bufferSize];
                short[] reBuf = new short[bufferSize];
                mIsRealTimeTraning=true;




                // 定义循环，根据isRecording的值来判断是否继续录制
                while (mIsRealTimeTraning) {
                    // 从bufferSize中读取字节，返回读取的short个数
                    int bufferReadResult = Record
                            .read(buffer, 0, buffer.length);
                    //System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult); 直接复制
                    //大端转小端处理
                    DataOutputStream dos = new DataOutputStream(
                            new BufferedOutputStream(
                                    new FileOutputStream(mAudioBuffer_before1)));
                    byte[] b=new byte[2];
                    for (int i = 0; i < bufferReadResult; i++) {
                        shortToByte_LH(buffer[i], b);
                        tmpBuf[i]=(byteToShort_HL(b));
                        dos.writeShort(byteToShort_HL(b));
                    }
                    dos.close();
                    //对buffer进行处理
                    rnnoise(path+mAudioBuffer_before1.getName(),path+mAudioBuffer_after1.getName());

                    //小端转大端
                    try {
                        DataInputStream dis= new DataInputStream(
                                new BufferedInputStream(new FileInputStream(mAudioBuffer_after1)));

                        /*for (int i = 0; i < bufferReadResult; i++) {
                            shortToByte_LH(tmpBuf[i], b);
                            reBuf[i]=(byteToShort_HL(b));
                        }*/

                        int i = 0;
                        //延时 应该没啥用
                        try
                        {
                            Thread.currentThread().sleep(10);//毫秒
                        }
                        catch(Exception e){}
                        while (dis.available() > 0 && i < buffer.length) {
                            shortToByte_LH(dis.readShort(),b);
                            reBuf[i] =byteToShort_HL(b);
                            i++;
                        }
                        dis.close();


                    }
                    catch (Exception e){}



                   Track.write(reBuf, 0, tmpBuf.length);

                }
                // 录制结束
                Record.stop();
                Track.stop();
            } catch (Exception e) {
                // TODO: handle exception
                Log.e("slack", "error:" + e.getMessage());
            }
            return null;
        }


        Runnable   runnableUi=new  Runnable(){
            @Override
            public void run() {
                //更新界面
                mTran.setTag(null);
                mTran.setEnabled(true);
                mTran.setText("降噪处理");
                mPlay2.setEnabled(true);
                showToast("降噪完毕");
            }

        };
        protected void onPostExecute(Void result) {

        }


        protected void onPreExecute() {

        }
    }


}
