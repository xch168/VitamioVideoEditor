package com.m4399.videoeditor.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.m4399.videoeditor.App;
import com.m4399.videoeditor.R;
import com.m4399.videoeditor.os.ThreadTask;
import com.m4399.videoeditor.views.TextureVideoView;
import com.m4399.videoeditor.views.VideoSelectionView;
import com.m4399.videoeditor.views.VideoSelectionView.OnBackgroundColorListener;
import com.m4399.videoeditor.views.VideoSelectionView.OnSeekBarChangeListener;
import com.m4399.videoeditor.views.VideoSelectionView.OnSwich60sListener;
import com.m4399.videoeditor.views.VideoSelectionView.OnVideoChangeScaleTypeListener;
import com.m4399.videoeditor.views.VideoViewTouch;
import com.m4399.videoeditor.util.Constant;
import com.yixia.camera.demo.log.Logger;
import com.yixia.camera.demo.ui.record.helper.RecorderHelper;
import com.yixia.videoeditor.adapter.UtilityAdapter;
import com.yixia.weibo.sdk.FFMpegUtils;
import com.yixia.weibo.sdk.VCamera;
import com.yixia.weibo.sdk.model.MediaObject;
import com.yixia.weibo.sdk.util.ConvertToUtils;
import com.yixia.weibo.sdk.util.DeviceUtils;
import com.yixia.weibo.sdk.util.FileUtils;
import com.yixia.weibo.sdk.util.StringUtils;
import com.yixia.weibo.sdk.util.ToastUtils;

import java.io.File;

public class ImportVideoActivity extends BaseActivity implements OnPreparedListener, TextureVideoView.OnPlayStateListener, OnInfoListener, OnVideoSizeChangedListener, OnErrorListener, OnSeekCompleteListener, OnClickListener, OnSeekBarChangeListener, OnSwich60sListener, OnBackgroundColorListener, OnVideoChangeScaleTypeListener
{
    /**
     * 显示正在加载
     */
    private View mVideoLoading;
    /**
     * 播放控件
     */
    private VideoViewTouch mVideoView;
    /**
     * 显示播放
     */
    private ImageView mPlayController;
    /**
     * 操作提示
     */
    private ImageView mTipsMove;
    /**
     * 视频区域选择
     */
    private VideoSelectionView mVideoSelection;
    /**
     * 操作提示文字
     */
    private View mTipMoveText;
    /**
     * 首次进入页面提示文字
     */
    private TextView mTipsSelect;
    private LinearLayout mPreviewLinearLayout;
    protected TextView titleText, titleRightTextView;
    protected TextView titleLeft, titleRight;
    /**
     * 屏幕的宽度
     */
    private int mWindowWidth;
    /**
     * 播放路径
     */
    private String mSourcePath;

    private boolean mIsFitCenter, mIsWhiteBackground;
    protected MediaObject mMediaObject;
    /**
     * 视频旋转角度
     */
    private int mVideoRotation = 0;
    /**
     * 视频临时目录
     */
    private String mTargetPath;
    /**
     * 预先裁剪是否完成
     */
    private boolean mTempVideoTranscodeFinishd;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_import_video);
        initIntent();
        if (mMediaObject == null)
        {
            String dirName = System.currentTimeMillis() + "";
            String directory = VCamera.getVideoCachePath();
            if (StringUtils.isNotEmpty(mTargetPath))
            {
                File f = new File(mTargetPath);
                if (!f.exists())
                {
                    f.mkdirs();
                }
                dirName = f.getName();
                directory = f.getParent() + "/";
            }
            mTargetPath = directory + dirName;
            mMediaObject = new MediaObject(directory, dirName, RecorderHelper.getVideoBitrate(), MediaObject.MEDIA_PART_TYPE_IMPORT_VIDEO);
        }
        initView();
    }

    private void initIntent()
    {
        mVideoRotation = getIntent().getIntExtra("orientation", 0);
        /** 存储 */
        mTargetPath = getIntent().getStringExtra("target");
    }

    private void initView()
    {

        titleLeft = findViewById(R.id.titleLeft);
        titleText = findViewById(R.id.titleText);
        titleRightTextView = findViewById(R.id.titleRightTextView);

        mVideoLoading = findViewById(R.id.video_loading);
        mVideoView = findViewById(R.id.preview);
        mPreviewLinearLayout = (LinearLayout) mVideoView.getParent();
        mPlayController = findViewById(R.id.play_controller);
        mVideoSelection = findViewById(R.id.video_selection_view);
        mTipsMove = findViewById(R.id.tips_move);
        mTipMoveText = findViewById(R.id.tips_move_text);
        mTipsSelect = findViewById(R.id.tip_import_video_select);

        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnPlayStateListener(this);
        mVideoView.setOnTouchEventListener(mOnVideoTouchListener);
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnVideoSizeChangedListener(this);
        mVideoView.setOnErrorListener(this);
        mVideoView.setOnSeekCompleteListener(this);
        titleLeft.setOnClickListener(this);

        titleRightTextView.setOnClickListener(this);
        titleRightTextView.setText(R.string.nexttip);
        titleRightTextView.setCompoundDrawables(null, null, null, null);

        mVideoSelection.setOnSeekBarChangeListener(this);
        mVideoSelection.setOnSwich60sListener(this);
        mVideoSelection.setOnBackgroundColorListener(this);
        mVideoSelection.setOnVideoChangeScaleTypeListener(this);

        initSurfaceView();

        titleText.setText(R.string.record_camera_import_title6);

        parseIntentUrl(getIntent());
    }

    private void initSurfaceView()
    {
        int w = DeviceUtils.getScreenWidth(this);// 屏幕宽度

        // 宽高一致
        View preview_layout = findViewById(R.id.preview_layout);
        LinearLayout.LayoutParams mParams = (LinearLayout.LayoutParams) preview_layout.getLayoutParams();
        mParams.height = w;
        preview_layout.setVisibility(View.VISIBLE);

        View cropView = findViewById(R.id.cropView);
        int cropHeight = (int) (mWindowWidth * 1.0f * 9 / 16);
        int topMargin = ConvertToUtils.dipToPX(this, 49) + (mWindowWidth - cropHeight) / 2;
        RelativeLayout.LayoutParams cropViewParam = (RelativeLayout.LayoutParams) cropView.getLayoutParams();
        cropViewParam.width = mWindowWidth;
        cropViewParam.height = cropHeight;
        cropViewParam.topMargin = topMargin;
        cropView.setLayoutParams(cropViewParam);
    }

    /**
     * 解析url
     *
     * @param intent
     * @return -1 解析失败 0 正在解析 1解析成功
     */
    private void parseIntentUrl(Intent intent)
    {
        if (intent != null)
        {
            try
            {
                mSourcePath = intent.getStringExtra("source");
                if (StringUtils.isEmpty(mSourcePath))
                {
                    Uri uri = intent.getData();
                    if (uri == null)
                    {
                        Bundle b = intent.getExtras();
                        Object o = b.get(Intent.EXTRA_STREAM);
                        uri = Uri.parse(o.toString());
                    }
                    if (uri != null)
                    {
                        if (uri.getScheme().startsWith("file"))
                        {
                            mSourcePath = uri.toString();
                        }
                        else
                        {
                            ContentResolver contentResolver = getContentResolver();
                            Cursor cursor = contentResolver.query(uri, null, null, null, null);
                            cursor.moveToFirst();
                            if (cursor != null)
                            {
                                cursor.moveToFirst();
                                int index2 = cursor.getColumnIndex("mime_type");
                                String type = cursor.getString(index2);
                                if (type != null && type.indexOf("video") != -1)
                                {

                                }
                                else
                                {
                                    return;
                                }
                                int index = cursor.getColumnIndex("_data");
                                if (index > -1)
                                {
                                    if (cursor.getString(index) != null)
                                    {
                                        mSourcePath = cursor.getString(index);
                                    }
                                }
                                else
                                {

                                }
                                cursor.close();
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
            }
        }
        // 本地地址检测是否存在
        if (StringUtils.isEmpty(mSourcePath) || !new File(mSourcePath).exists())
        {
            ToastUtils.showToast(ImportVideoActivity.this, R.string.record_camera_import_video_exists);
            finish();
        }
        else
        {
            mVideoView.setVideoPath(mSourcePath);
        }
    }


    private VideoViewTouch.OnTouchEventListener mOnVideoTouchListener = new VideoViewTouch.OnTouchEventListener()
    {

        @Override
        public boolean onClick()
        {
            if (mVideoView.isPlaying())
            {
                mVideoView.pauseClearDelayed();
            }
            else
            {
                mVideoView.start();
                mHandler.sendEmptyMessage(HANDLE_PROGRESS);
            }
            return true;
        }

        @Override
        public void onVideoViewDown()
        {
        }

        @Override
        public void onVideoViewUp()
        {

        }
    };


    /**
     * 显示进度
     */
    private static final int HANDLE_PROGRESS = 1;
    /**
     * 选区延迟检测
     */
    private static final int HANDLE_SEEKTO = 2;

    /**
     * 重置时间前的开始时间（关键帧的问题，导致可能需要重新设置开始和结束时间）
     */
    private int mPreChangedStartTime;
    /**
     * 重置时间前的结束时间
     */
    private int mPreChangedEndTime;
    /**
     * 是否重置时间标记
     */
    private boolean mIsChangeTime;

    private long lastPosition = 0;

    /**
     * 更新进度线的位置
     */
    private void setLinePosition()
    {
        if (mVideoView != null)
        {

            int startTime = mVideoSelection.getStartTime();
            int endTime = mVideoSelection.getEndTime();

            long position = mVideoView.getCurrentPosition();
            if (lastPosition != 0 && Math.abs(position - lastPosition) > 500)
            {

                mPreChangedStartTime = startTime;
                mPreChangedEndTime = endTime;

                endTime = (int) position + endTime - startTime;
                startTime = (int) position;
                mVideoSelection.setStartTime(startTime);
                mVideoSelection.setEndTime(endTime);

                mVideoSelection.setStartTime(startTime);
                mVideoSelection.setEndTime(endTime);

                mIsChangeTime = true;
            }
            lastPosition = position;

            if (mVideoSelection != null)
            {
                if (mVideoSelection.mVideoSelection != null)
                {
                    mVideoSelection.mVideoSelection.setLinePosition(position, startTime, endTime);
                }
            }
        }
    }

    /**
     * 视频暂停时 把进度线也隐藏
     */
    private void clearLine()
    {
        if (mVideoSelection != null)
        {
            if (mVideoSelection.mVideoSelection != null)
            {
                mVideoSelection.mVideoSelection.clearLine();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case HANDLE_PROGRESS:
                    if (mVideoView.isPlaying())
                    {
                        // 播放到结束时间的位置 则暂停不要循环
                        long position = mVideoView.getCurrentPosition();

                        if ((position >= mVideoSelection.getEndTime() && (lastPosition != 0 && Math.abs(
                                position - lastPosition) < 500)) || position == mVideoView.getDuration())
                        {
                            Logger.e("simon", "step1");
                            if (mIsChangeTime)
                            {
                                Logger.e("simon", "当前重设的历史StartTime>>" + mPreChangedStartTime + ">>>当前记录的历史endTime>>>" + mPreChangedEndTime);
                                mVideoSelection.setStartTime(mPreChangedStartTime);
                                mVideoSelection.setEndTime(mPreChangedEndTime);
                                mIsChangeTime = false;
                            }
                            Logger.e("simon", "暂停了?position ::" + position + "endTime::" + mVideoSelection.getEndTime() + "view.getDuration::" + mVideoView.getDuration());
                            final int startTime = mVideoSelection.getStartTime();
                            mVideoView.pauseClearDelayed();
                            mVideoView.seekTo(startTime);
                        }
                        else
                        {
                            Logger.e("simon", "step2");
                            setLinePosition();
                            sendEmptyMessageDelayed(HANDLE_PROGRESS, 20);
                        }
                        setProgress();
                    }
                    else if (mVideoView.isPaused())
                    {
                        Logger.e("simon", "step3");
                        if (mIsChangeTime)
                        {
                            Logger.e("", "当前重设的历史StartTime>>" + mPreChangedStartTime + ">>>当前记录的历史endTime>>>" + mPreChangedEndTime);
                            mVideoSelection.setStartTime(mPreChangedStartTime);
                            mVideoSelection.setEndTime(mPreChangedEndTime);
                            mIsChangeTime = false;
                        }
                        final int startTime = mVideoSelection.getStartTime();
                        mVideoView.seekTo(startTime);
                        setProgress();
                    }
                    break;
                case HANDLE_SEEKTO:
                    if (!isFinishing())
                    {
                        final int startTime = mVideoSelection.getStartTime();
                        if (mVideoView.isPlaying())
                        {
                            mVideoView.loopDelayed(startTime, mVideoSelection.getEndTime());
                        }
                        else
                        {
                            mVideoView.seekTo(startTime);
                        }
                        setProgress();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private long setProgress()
    {
        return 0;
    }

    private int scale;

    @Override
    public void onChanged(int scale)
    {
        this.scale = scale;
        setVideoMode(scale);
    }

    @Override
    public void onChanged(boolean isWhiteBackground)
    {
        if (isWhiteBackground)
        {
            mPreviewLinearLayout.setBackgroundColor(getResources().getColor(R.color.white));
            mIsWhiteBackground = true;
        }
        else
        {
            mIsWhiteBackground = false;
            mPreviewLinearLayout.setBackgroundColor(getResources().getColor(R.color.black));
        }
    }

    @Override
    public void onChanged()
    {
        if (mVideoView != null)
        {
            mVideoView.pauseClearDelayed();
            mVideoView.seekTo(0);
        }
    }

    @Override
    public void onProgressChanged()
    {
        // 拖动手柄或者滚动缩略图片的时候把视频停止 并把指针移到起始位置
        if (mVideoView != null)
        {
            if (mVideoView.isPlaying())
            {
                mVideoView.pauseClearDelayed();
            }
            int startTime = mVideoSelection.getStartTime();

            // Log.e("simon","onProgressChanged::StartTime>>"+startTime+">>>endTime>>>"+mVideoSelection.getEndTime());

            mVideoView.seekTo(startTime);
            setProgress();
        }
    }

    @Override
    public void onProgressEnd()
    {
        if (mHandler.hasMessages(HANDLE_SEEKTO))
        {
            mHandler.removeMessages(HANDLE_SEEKTO);
        }
        mHandler.sendEmptyMessageDelayed(HANDLE_SEEKTO, 20);
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.titleLeft:
                onBackPressed();
                break;
            case R.id.titleRightTextView:
                startEncoding();
                break;
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mp)
    {
        Logger.e("simon", "[ImportVideoActivity]onSeekComplete...");
        // mVideoView.start();
        lastPosition = 0;
        mPreChangedStartTime = 0;
        mPreChangedEndTime = 0;
        mIsChangeTime = false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra)
    {
        return false;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height)
    {

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra)
    {
        return false;
    }

    @Override
    public void onStateChanged(boolean isPlaying)
    {
        if (isPlaying)
        {
            mHandler.removeMessages(HANDLE_PROGRESS);
            mHandler.sendEmptyMessage(HANDLE_PROGRESS);
            mPlayController.setVisibility(View.GONE);
        }
        else
        {
            clearLine();
            mHandler.removeMessages(HANDLE_PROGRESS);
            mPlayController.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 视频时长
     */
    private int mDuration = -1;

    @Override
    public void onPrepared(MediaPlayer mp)
    {
        // 检测
        mVideoLoading.setVisibility(View.GONE);
        mDuration = mVideoView.getDuration();

        if (mDuration < 3000)
        {
            ToastUtils.showToast(ImportVideoActivity.this, R.string.video_import_duration_too_short);
            finish();
            return;
        }

        setVideoMode(scale);
        mVideoSelection.init(FileUtils.getCacheDiskPath(this, "thumbs"), mSourcePath, mDuration,
                             RecorderHelper.getMaxDuration() > 10 * 1000 ? 60 * 1000 : 10 * 1000, 3 * 1000);


        mVideoView.start();
    }

    private void setVideoMode(int scale)
    {
        if (scale == VideoSelectionView.FIT_XY)
        {
            mVideoView.resize();
            if (mVideoView.getCropX() == 0 && mVideoView.getCropY() == 0)
            {
                mVideoView.centerXY();
            }
            mIsFitCenter = false;
            mPreviewLinearLayout.setGravity(Gravity.NO_GRAVITY);
        }
        else if (scale == VideoSelectionView.FIT_CENTER)
        {
            mVideoView.fitCenter();
            mPreviewLinearLayout.setGravity(Gravity.CENTER);
            mIsFitCenter = true;
        }
    }

    /**
     * 下一步转码
     */
    @SuppressLint("NewApi")
    private void startEncoding()
    {
        // 检测磁盘空间
        if (!App.isAvailableSpace())
        {
            // ToastUtils.showToastErrorTip(R.string.record_check_available_faild);
            return;
        }

        if (mVideoSelection != null)
        {
            mVideoSelection.killSnapImage();
        }
        // ffmpeg -i 1.mp4 -vcodec copy -acodec copy -vbsf h264_mp4toannexb 1.ts
        // 将视频转成ts
        if (mMediaObject != null)
        {
            // 生成片段信息
            com.yixia.weibo.sdk.model.MediaObject$MediaPart part = mMediaObject.getLastPart();
            if (part == null)
            {
                part = mMediaObject.buildMediaPart(-1, ".mp4");
            }

            // 暂停播放
            mVideoView.pauseClearDelayed();

            Logger.e("samuel", " mVideoSelection.getStartTime()" + mVideoSelection.getStartTime() + "<><>mPreStartTime::" + mPreChangedStartTime);

            final com.yixia.weibo.sdk.model.MediaObject$MediaPart mediaPart = part;
            final int videoWidth = mVideoView.getVideoWidth();
            final int videoHeight = mVideoView.getVideoHeight();
            final int cropX = mVideoView.getCropX();
            final int cropY = mVideoView.getCropY();
            final float scale = mVideoView.getScale();

            int startTimetmp = 0;
            int endTimetmp = 0;
            if (mIsChangeTime)
            {
                startTimetmp = mPreChangedStartTime;
                endTimetmp = mPreChangedEndTime;
            }
            else
            {
                startTimetmp = mVideoSelection.getStartTime();
                endTimetmp = mVideoSelection.getEndTime();
            }

            final int startTime = startTimetmp;
            final int endTime = endTimetmp;
            final String output = mediaPart.mediaPath;

            part.duration = endTime - startTime;
            mTempVideoTranscodeFinishd = false;

            Logger.e("startTime / 1000F, (endTime - startTime) / 1000F " + startTime / 1000F + "," + mVideoSelection.getVideoTime() / 1000F);

            new ThreadTask<Void, Void, Boolean>()
            {

                @Override
                protected void onPreExecute()
                {
                    super.onPreExecute();
                    showDialog();
                }

                @Override
                protected Boolean doInBackground(Void... params)
                {
                    // 检测是否正在截取缩略图，是的话要等切完了再转
                    while (mVideoSelection.isThumbLoading())
                    {
                        SystemClock.sleep(500);
                    }

                    if (mVideoRotation <= 0)
                    {
                        mVideoRotation = UtilityAdapter.VideoGetMetadataRotate(mSourcePath);
                    }

                    String cutpath = mediaPart.mediaPath;// + ".mp4";
                    if (StringUtils.isNotEmpty(cutpath))
                    {
                        File f = new File(cutpath);
                        if (!f.exists())
                        {
                            String parentPath = f.getParent();
                            File file = new File(parentPath);
                            if (!file.exists())
                            {
                                try
                                {
                                    //按照指定的路径创建文件夹
                                    file.mkdirs();
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                            File dir = new File(cutpath);
                            if (!dir.exists())
                            {
                                try
                                {
                                    //在指定的文件夹中创建文件
                                    dir.createNewFile();
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    // ========== 先切割 -vcodec copy -acodec copy -vbsf
                    // h264_mp4toannexb
                    @SuppressLint("DefaultLocale")
                    String cmd = String.format("ffmpeg %s -ss %.1f -i \"%s\" -t %.1f -vcodec copy -acodec copy  -f mp4 -movflags faststart \"%s\"",
                                               FFMpegUtils.getLogCommand(), startTime / 1000F, mSourcePath, mVideoSelection.getVideoTime() / 1000F,
                                               cutpath);

                    boolean result = UtilityAdapter.FFmpegRun("", cmd) == 0;
                    return result;
                }

                @Override
                protected void onPostExecute(Boolean result)
                {
                    super.onPostExecute(result);
                    hideDialog();
                    if (!isFinishing())
                    {
                        if (result)
                        {

                            String mCoverPath = mediaPart.mediaPath.replace(".mp4", ".jpg");
                            Log.i("asdf", "path:" + mediaPart.mediaPath);
                            startActivity(new Intent(ImportVideoActivity.this, VideoPlayerActivity.class).putExtra(Constant.RECORD_VIDEO_PATH, mediaPart.mediaPath).putExtra(Constant.RECORD_VIDEO_CAPTURE, mCoverPath));
                        }
                        else
                        {
                            ToastUtils.showToast(ImportVideoActivity.this, R.string.video_transcoding_faild);
                        }
                    }
                }
            }.execute();
        }
    }

    @Override
    public void onBackPressed()
    {
        hideDialog();
        // 删除临时文件
        if (mMediaObject != null)
        {
            mMediaObject.cancel();
        }
        finish();
    }

    ProgressDialog mEncodingProgressDialog;

    private void showDialog()
    {
        if (isFinishing())
        {
            return;
        }
        if (mEncodingProgressDialog == null)
        {
            mEncodingProgressDialog = showVideoProcessDialog(this, this.getResources().getString(R.string.dialog_encoding_text));
        }
        mEncodingProgressDialog.show();
    }

    private void hideDialog()
    {
        if (!isFinishing() && mEncodingProgressDialog != null && mEncodingProgressDialog.isShowing())
        {
            mEncodingProgressDialog.dismiss();
            mEncodingProgressDialog = null;
        }
    }

    public static ProgressDialog showVideoProcessDialog(Context mContext, String text)
    {
        ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setIndeterminate(true);
        dialog.show();
        View convertView = LayoutInflater.from(mContext).inflate(R.layout.dialog_encoding_novalue, null);
        dialog.setContentView(convertView);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        TextView textview = convertView.findViewById(R.id.text);
        if (StringUtils.isEmpty(text))
        {
            textview.setVisibility(View.GONE);
        }
        else
        {
            textview.setVisibility(View.VISIBLE);
            textview.setText(text);
        }
        return dialog;
    }

}
