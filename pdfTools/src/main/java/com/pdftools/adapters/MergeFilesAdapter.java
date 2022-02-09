package com.pdftools.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftools.R;
import com.pdftools.utils.FileUtils;
import com.pdftools.utils.PDFUtils;

import java.util.ArrayList;


public class MergeFilesAdapter extends RecyclerView.Adapter<MergeFilesAdapter.ViewMergeFilesHolder> {

    private final ArrayList<String> mFilePaths;
    private final Activity mContext;
    private final OnClickListener mOnClickListener;
    private final PDFUtils mPDFUtils;
    private final boolean mIsMergeFragment;

    public MergeFilesAdapter(Activity mContext, ArrayList<String> mFilePaths,
                             boolean mIsMergeFragment, OnClickListener mOnClickListener) {
        this.mContext = mContext;
        this.mFilePaths = mFilePaths;
        this.mOnClickListener = mOnClickListener;
        mPDFUtils = new PDFUtils(mContext);
        this.mIsMergeFragment = mIsMergeFragment;
    }

    @NonNull
    @Override
    public ViewMergeFilesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_merge_files, parent, false);
        return new MergeFilesAdapter.ViewMergeFilesHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewMergeFilesHolder holder, int position) {
        boolean isEncrypted = mPDFUtils.isPDFEncrypted(mFilePaths.get(position));
        holder.mFileName.setText(FileUtils.getFileName(mFilePaths.get(position)));
    }

    @Override
    public int getItemCount() {
        return mFilePaths == null ? 0 : mFilePaths.size();
    }

    public class ViewMergeFilesHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView mFileName;
        AppCompatCheckBox mCheckbox;

        ViewMergeFilesHolder(View itemView) {
            super(itemView);
            mFileName = (TextView) itemView.findViewById(R.id.fileName);
            mCheckbox = (AppCompatCheckBox) itemView.findViewById(R.id.itemMerge_checkbox);

            mFileName.setOnClickListener(this);
            if (mIsMergeFragment) mCheckbox.setVisibility(View.VISIBLE);
            else mCheckbox.setVisibility(View.GONE);

            mCheckbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCheckboxClick();
                }
            });

        }

        @Override
        public void onClick(View view) {

            if (getAdapterPosition() >= mFilePaths.size())
                return;

            if (mIsMergeFragment) mCheckbox.toggle();
            mOnClickListener.onItemClick(mFilePaths.get(getAdapterPosition()));
        }

        public void onCheckboxClick() {
            mOnClickListener.onItemClick(mFilePaths.get(getAdapterPosition()));
        }
    }

    public interface OnClickListener {
        void onItemClick(String path);
    }
}
