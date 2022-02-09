package com.pdftools.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.pdftools.R;
import com.pdftools.utils.FileUtils;

public class MergeSelectedFilesAdapter extends RecyclerView.Adapter<MergeSelectedFilesAdapter.MergeSelectedFilesHolder> {

    private final ArrayList<String> mFilePaths;
    private final Activity mContext;
    private final OnFileItemClickListener mOnClickListener;

    public MergeSelectedFilesAdapter(Activity mContext, ArrayList<String> mFilePaths,
                                     OnFileItemClickListener mOnClickListener) {
        this.mContext = mContext;
        this.mFilePaths = mFilePaths;
        this.mOnClickListener = mOnClickListener;
    }

    @NonNull
    @Override
    public MergeSelectedFilesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_merge_selected_files, parent, false);
        return new MergeSelectedFilesAdapter.MergeSelectedFilesHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MergeSelectedFilesHolder holder, int position) {
        holder.mFileName.setText(FileUtils.getFileName(mFilePaths.get(position)));
    }

    @Override
    public int getItemCount() {
        return mFilePaths == null ? 0 : mFilePaths.size();
    }

    public class MergeSelectedFilesHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView mFileName;
        ImageView mViewFile;
        ImageView mRemove;
        ImageView mUp;
        ImageView mDown;

        MergeSelectedFilesHolder(View itemView) {
            super(itemView);
            mFileName = (TextView) itemView.findViewById(R.id.fileName);
            mViewFile = (ImageView) itemView.findViewById(R.id.view_file);
            mRemove = (ImageView) itemView.findViewById(R.id.remove);
            mUp = (ImageView) itemView.findViewById(R.id.up_file);
            mDown = (ImageView) itemView.findViewById(R.id.down_file);

            mViewFile.setOnClickListener(this);
            mRemove.setOnClickListener(this);
            mUp.setOnClickListener(this);
            mDown.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.view_file) {
                mOnClickListener.viewFile(mFilePaths.get(getAdapterPosition()));
            } else if (view.getId() == R.id.up_file) {
                if (getAdapterPosition() != 0) {
                    mOnClickListener.moveUp(getAdapterPosition());
                }
            } else if (view.getId() == R.id.down_file) {
                if (mFilePaths.size() != getAdapterPosition() + 1) {
                    mOnClickListener.moveDown(getAdapterPosition());
                }
            } else {
                mOnClickListener.removeFile(mFilePaths.get(getAdapterPosition()));
            }
        }
    }

    public interface OnFileItemClickListener {
        void viewFile(String path);
        void removeFile(String path);
        void moveUp(int position);
        void moveDown(int position);
    }
}
