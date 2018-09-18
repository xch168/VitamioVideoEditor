package com.m4399.videoeditor.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.m4399.videoeditor.R;
import com.m4399.videoeditor.entity.Video;
import com.m4399.videoeditor.ui.ImportVideoActivity;
import com.m4399.videoeditor.ui.ImportVideoSelectActivity;
import com.yixia.weibo.sdk.util.DateUtil;
import com.yixia.weibo.sdk.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ImportVideoSelectionAdapter extends BaseAdapter
{

    private ArrayList<ImportVideoSelectActivity.VideoRow> selectionVideoList;
    private Context mContext;
    /**
     * 一行显示个数
     */
    private static final int ROW_ITEM_COUNT = 3;
    private int mItemHeight;

    public ImportVideoSelectionAdapter(Context context, int itemWidth)
    {
        this.mContext = context;
        this.mItemHeight = itemWidth;
        selectionVideoList = new ArrayList<>();
    }

    public void updateVideoData(ArrayList<ImportVideoSelectActivity.VideoRow> result)
    {
        selectionVideoList = result;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return selectionVideoList.size();
    }

    @Override
    public Object getItem(int position)
    {
        return selectionVideoList.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        final ViewHolder holder;
        if (convertView == null)
        {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_import_video, null);
            holder = new ViewHolder(convertView);

            OnClickListener mCheckedOnClickListener = new OnClickListener()
            {

                @Override
                public void onClick(View v)
                {
                    Video video = (Video) v.getTag();
                    // 去剪切页面
                    if (video != null)
                    {
                        Intent intent = new Intent(mContext, ImportVideoActivity.class);
                        Bundle bundle = ((Activity) mContext).getIntent().getExtras();
                        if (bundle == null)
                        {
                            bundle = new Bundle();
                        }
                        bundle.putString("source", video.url);
                        if (StringUtils.isNotEmpty(video.url) && video.url.endsWith(".gif"))
                        {
                            bundle.putBoolean("gif", true);
                        }

                        bundle.putInt("orientation", video.orientation);
                        intent.putExtras(bundle);
                        mContext.startActivity(intent);
                    }
                }
            };

            for (int i = 0; i < ROW_ITEM_COUNT; i++)
            {
                ViewGroup.LayoutParams lp = holder.layout[i].getLayoutParams();
                lp.width = mItemHeight;
                lp.height = mItemHeight;
                holder.icon[i].setOnClickListener(mCheckedOnClickListener);
            }

            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }

        final ImportVideoSelectActivity.VideoRow item = (ImportVideoSelectActivity.VideoRow) getItem(position);

        final List<Video> videos = item.images;
        for (int i = 0, j = videos.size(); i < ROW_ITEM_COUNT; i++)
        {
            ImageView icon = holder.icon[i];
            TextView size = holder.size[i];
            RelativeLayout layout = holder.layout[i];
            if (i < j)
            {
                layout.setVisibility(View.VISIBLE);
                final Video image = videos.get(i);
                if (image.faild)
                {
                    icon.setImageResource(R.drawable.import_image_default);
                }
                else
                {
                    icon.setImageBitmap(getVideoThumbnail(image.url));
                }
                icon.setTag(image);
                size.setText(DateUtil.getTimeLengthString(image.duration / 1000));
            }
            else
            {
                layout.setVisibility(View.INVISIBLE);
            }

        }
        return convertView;
    }

    public static class ViewHolder
    {
        public ImageView[] icon = new ImageView[ROW_ITEM_COUNT];
        public TextView[] size = new TextView[ROW_ITEM_COUNT];
        public RelativeLayout[] layout = new RelativeLayout[ROW_ITEM_COUNT];

        public ViewHolder(View convertView)
        {
            icon[0] = convertView.findViewById(R.id.icon1);

            size[0] = convertView.findViewById(R.id.size1);
            layout[0] = convertView.findViewById(R.id.layout1);

            icon[1] = convertView.findViewById(R.id.icon2);

            size[1] = convertView.findViewById(R.id.size2);
            layout[1] = convertView.findViewById(R.id.layout2);

            icon[2] = convertView.findViewById(R.id.icon3);

            size[2] = convertView.findViewById(R.id.size3);
            layout[2] = convertView.findViewById(R.id.layout3);
        }
    }

    public Bitmap getVideoThumbnail(String filePath)
    {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try
        {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                retriever.release();
            }
            catch (RuntimeException e)
            {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

}
