package com.m4399.videoeditor.views;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.widget.ImageView;

import com.yixia.camera.demo.log.Logger;
import com.yixia.weibo.sdk.util.FileUtils;
import com.yixia.weibo.sdk.util.StringUtils;

import java.io.File;

/**
 * 视频截图
 *
 * @author tangjun
 */
public class VideoThumbImageView extends ImageView
{

    /**
     * 截图存放路径
     */
    private String mThumbPath;
    /**
     * 屏幕宽度
     */
    private int mWindowWidth;
    /**
     * 索引
     */
    private int mIndex;
    /**
     * 当前时间
     */
    private int mPosition;
    /**
     * 是否已经加载了图片
     */
    private boolean mNeedLoad;

    public VideoThumbImageView(Context context, String thumbPath, int mWindowWidth, int index, int position)
    {
        super(context);
        this.mWindowWidth = mWindowWidth;
        this.mThumbPath = thumbPath;
        this.mNeedLoad = true;
        this.mIndex = index;
        this.mPosition = position;
    }


    /**
     * 获取截图对应的时间戳
     */
    public int getThumbPosition()
    {
        return mPosition;
    }

    public int getThumbIndex()
    {
        return mIndex;
    }

    public String getThumbPath()
    {
        return mThumbPath;
    }

    public void log()
    {
        Rect rect = new Rect();
        getGlobalVisibleRect(rect);
        Logger.d("[VideoThumbImageView]mNeedLoad:" + mNeedLoad + " checkVisible:" + checkVisible() + ":getLeft:" + getLeft() + ":getRight:" + getRight() + " checkThumb:" + checkThumb() + " " + new File(
                        mThumbPath).getName());
    }

    /**
     * 检测是否需要显示
     */
    public boolean checkVisible()
    {
        if (StringUtils.isNotEmpty(mThumbPath))
        {
            Rect rect = new Rect();
            getGlobalVisibleRect(rect);

            if (rect.right <= mWindowWidth && rect.right > 0)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测是否需要截图
     */
    public boolean checkThumb()
    {
        File f = new File(mThumbPath);
        if (f.exists() && f.canRead() && f.length() > 0)
        {
            return true;
        }
        return false;
    }

    /**
     * 检查是否需要加载
     */
    public boolean needLoad()
    {
        return mNeedLoad;
    }

    public static Uri getFileUri(String path)
    {

        Logger.e("simon", "getFile Uri>>>" + path);

        return Uri.parse("file:///" + path);
    }

    /**
     * 开始截图
     */
    public void loadImage()
    {
        if (FileUtils.checkFile(mThumbPath))
        {
            setImageURI(getFileUri(mThumbPath));
            mNeedLoad = false;
        }
    }

}
