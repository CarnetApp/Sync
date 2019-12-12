package com.spisoft.sync.browsing;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.spisoft.sync.R;
import com.spisoft.sync.wrappers.FileItem;

import java.util.List;

/**
 * Created by alexandre on 16/03/17.
 */

public class FileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<FileItem> mItemList;
    private Context mContext;
    private Listener mListener;

    public interface Listener{
        void onFileClick(FileItem item);
        void onMenuClick(FileItem item);
    }

    public FileListAdapter(Context context){
        super();
        mContext = context;

    }

    public void setListener(Listener listener){
        mListener = listener;
    }

    public void setFileList(List<FileItem> itemList){
        mItemList = itemList;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.file_item_view, parent, false);

        return new FileViewHolder(v);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((FileViewHolder)holder).setFileItem(mItemList.get(position));
    }

    @Override
    public int getItemCount() {
        return mItemList!=null?mItemList.size():0;
    }

    public class FileViewHolder extends RecyclerView.ViewHolder {


        private final TextView mNameTextView;
        private final ImageView mIconImgView;
        private final ImageView mDotsMenuImgView;

        public FileViewHolder(View itemView) {
            super(itemView);
            mIconImgView = (ImageView)itemView.findViewById(R.id.icon);
            mNameTextView = (TextView)itemView.findViewById(R.id.filename);
            mDotsMenuImgView = (ImageView)itemView.findViewById(R.id.dots_menu);
        }

        public void setFileItem(final FileItem item){
            mNameTextView.setText(item.getName());
            if(item.isDirectory())
                mIconImgView.setImageResource(R.drawable.directory);
            else
                mIconImgView.setImageResource(R.drawable.file);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onFileClick(item);
                }
            });
            mDotsMenuImgView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onMenuClick(item);
                }
            });
        }
    }
}