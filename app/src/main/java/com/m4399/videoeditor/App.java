package com.m4399.videoeditor;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.yixia.weibo.sdk.VCamera;
import com.yixia.weibo.sdk.util.DeviceUtils;
import com.yixia.weibo.sdk.util.FileUtils;
import com.yixia.weibo.sdk.util.ToastUtils;

import java.io.File;

public class App extends Application
{

    private static App application;

    @Override
    public void onCreate()
    {
        super.onCreate();
        application = this;

        // 设置拍摄视频缓存路径
        File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        if (DeviceUtils.isZte())
        {
            if (dcim.exists())
            {
                VCamera.setVideoCachePath(dcim + "/Camera/VideoEditor/");
            }
            else
            {
                VCamera.setVideoCachePath(dcim.getPath().replace("/sdcard/", "/sdcard-ext/") + "/Camera/VideoEditor/");
            }
        }
        else
        {
            VCamera.setVideoCachePath(dcim + "/Camera/VideoEditor/");
        }
        // 开启log输出,ffmpeg输出到logcat
        VCamera.setDebugMode(true);
        // 初始化拍摄SDK，必须
        VCamera.initialize(this);
    }

    public static Context getContext()
    {
        return application;
    }


    public final static int AVAILABLE_SPACE = 200;//M

    /**
     * 检测用户手机是否剩余可用空间200M以上
     *
     * @return
     */
    public static boolean isAvailableSpace()
    {
        if (application == null)
        {
            return false;
        }
        //检测磁盘空间
        if (FileUtils.showFileAvailable(application) < AVAILABLE_SPACE)
        {
            ToastUtils.showToast(application, application.getString(R.string.record_check_available_faild, AVAILABLE_SPACE));
            return false;
        }

        return true;
    }

    /**
     * 视频截图目录
     */
    public static File getThumbCacheDirectory()
    {
        if (application != null)
        {
            return FileUtils.getCacheDiskPath(application, "thumbs");//vineApplication.getExternalCacheDir() + "/cache/thumbs/";
        }
        return null;
    }


}
