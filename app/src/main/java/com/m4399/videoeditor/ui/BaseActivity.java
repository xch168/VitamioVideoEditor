package com.m4399.videoeditor.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.view.Window;

import com.yixia.weibo.sdk.util.StringUtils;


public class BaseActivity extends Activity
{

    protected ProgressDialog mProgressDialog;

    public ProgressDialog showProgress(String title, String message)
    {
        return showProgress(title, message, -1);
    }

    public ProgressDialog showProgress(String title, String message, int theme)
    {
        if (mProgressDialog == null)
        {
            if (theme > 0)
            {
                mProgressDialog = new ProgressDialog(this, theme);
            }
            else
            {
                mProgressDialog = new ProgressDialog(this);
            }
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mProgressDialog.setCanceledOnTouchOutside(false);// 不能取消
            mProgressDialog.setIndeterminate(true);// 设置进度条是否不明确
        }

        if (!StringUtils.isEmpty(title))
        {
            mProgressDialog.setTitle(title);
        }
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
        return mProgressDialog;
    }

    public void hideProgress()
    {
        if (mProgressDialog != null)
        {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        hideProgress();
        mProgressDialog = null;
    }
}
