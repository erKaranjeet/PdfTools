package com.pdftools.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;
import com.dd.morphingbutton.MorphingButton;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;
import com.pdftools.R;
import com.pdftools.adapters.FilesListAdapter;
import com.pdftools.utils.FileUtils;
import com.pdftools.utils.MorphButtonUtility;
import com.pdftools.utils.SplitPDFUtils;
import com.pdftools.utils.StringUtils;
import com.pdftools.utils.ViewFilesDividerItemDecoration;

import java.io.File;
import java.util.ArrayList;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static com.pdftools.utils.StringUtils.FOLDER_LOCATION;

public class SplitPdfActivity extends AppCompatActivity implements PickiTCallbacks, FilesListAdapter.OnFileItemClickedListener {

    MorphingButton selectFileButton;
    MorphingButton splitFilesButton;
    TextView splitFilesSuccessText, tvFolderLocation;
    EditText mSplitConfitEditText;
    RecyclerView mSplittedFiles;

    private Activity mActivity;
    private String mPath;
    private MorphButtonUtility mMorphButtonUtility;
    private FileUtils mFileUtils;
    private SplitPDFUtils mSplitPDFUtils;
    private static final int INTENT_REQUEST_PICKFILE_CODE = 10;
    private PickiT pickiT;
    private ProgressDialog progressBar;
    private StorageChooser.Builder builder = new StorageChooser.Builder();
    private StorageChooser chooser;
    private StringUtils stringUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_pdf);

        selectFileButton = (MorphingButton) findViewById(R.id.selectFile);
        splitFilesButton = (MorphingButton) findViewById(R.id.splitFiles);
        splitFilesSuccessText = (TextView) findViewById(R.id.splitfiles_text);
        mSplitConfitEditText = (EditText) findViewById(R.id.split_config);
        mSplittedFiles = (RecyclerView) findViewById(R.id.splitted_files);
        tvFolderLocation = (TextView) findViewById(R.id.tv_split_folder);

        stringUtils = new StringUtils(this);
        if (stringUtils.GetPreferences(FOLDER_LOCATION).equals("")) {
            stringUtils.SavePreferences(FOLDER_LOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        }
        tvFolderLocation.setText(StringUtils.getInstance().getDefaultStorageLocation(this));

        mActivity = (Activity) SplitPdfActivity.this;
        mMorphButtonUtility = new MorphButtonUtility(mActivity);
        mFileUtils = new FileUtils(mActivity);
        mSplitPDFUtils = new SplitPDFUtils(mActivity);
        pickiT = new PickiT(this, this, mActivity);
        resetValues();


        //Location chooser
        Content c = new Content();
        c.setCreateLabel("Create");
        c.setInternalStorageText("My Storage");
        c.setCancelLabel("Cancel");
        c.setSelectLabel("Choose Folder");
        c.setOverviewHeading("Choose Drive");

        builder.withActivity(this)
                .withFragmentManager(getFragmentManager())
                .setMemoryBarHeight(1.5f)
                .disableMultiSelect()
                .withContent(c);

        findViewById(R.id.btn_split_choose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                builder.skipOverview(true);
                builder.allowCustomPath(true);
                builder.setTheme(getScTheme(true));
                builder.setType(StorageChooser.DIRECTORY_CHOOSER);

                chooser = builder.build();

                chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                    @Override
                    public void onSelect(String path) {
//                        Toast.makeText(PdfToJpegActivity.this, path, Toast.LENGTH_SHORT).show();
                        tvFolderLocation.setText(path);
                        stringUtils.SavePreferences(FOLDER_LOCATION, path);
                    }
                });

                chooser.setOnCancelListener(new StorageChooser.OnCancelListener() {
                    @Override
                    public void onCancel() {

                    }
                });

                chooser.setOnMultipleSelectListener(new StorageChooser.OnMultipleSelectListener() {
                    @Override
                    public void onDone(ArrayList<String> selectedFilePaths) {
                        for(String s: selectedFilePaths) {
                            Log.e("TAG", s);
                        }
                    }
                });

                chooser.show();
            }
        });


        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  first check if permissions was granted
                if (checkSelfPermission()) {
                    Intent intent;
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                    } else {
                        intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.INTERNAL_CONTENT_URI);
                    }
                    //  In this example we will set the type to video
                    intent.setType("application/pdf");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.putExtra("return-data", true);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivityForResult(intent, INTENT_REQUEST_PICKFILE_CODE);
                }
            }
        });

        splitFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringUtils.getInstance().hideKeyboard(mActivity);

                ArrayList<String> outputFilePaths = mSplitPDFUtils.splitPDFByConfig(mPath,
                        mSplitConfitEditText.getText().toString());
                int numberOfPages = outputFilePaths.size();
                if (numberOfPages == 0) {
                    return;
                }
                String output = String.format(mActivity.getString(R.string.split_success), numberOfPages);
                StringUtils.getInstance().showSnackbar(mActivity, output);
                splitFilesSuccessText.setVisibility(View.VISIBLE);
                splitFilesSuccessText.setText(output);

                FilesListAdapter splitFilesAdapter = new FilesListAdapter(mActivity, outputFilePaths, SplitPdfActivity.this);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mActivity);
                mSplittedFiles.setVisibility(View.VISIBLE);
                mSplittedFiles.setLayoutManager(mLayoutManager);
                mSplittedFiles.setAdapter(splitFilesAdapter);
                mSplittedFiles.addItemDecoration(new ViewFilesDividerItemDecoration(mActivity));
                resetValues();
            }
        });
    }

    private StorageChooser.Theme getScTheme(boolean isChecked) {
        StorageChooser.Theme theme = new StorageChooser.Theme(getApplicationContext());
        theme.setScheme((isChecked) ? getResources().getIntArray(R.array.paranoid_theme) : theme.getDefaultScheme());
        return theme;
    }

    //  Check if permissions was granted
    private boolean checkSelfPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 23);
            return false;
        }
        return true;
    }

    //  Handle permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 23) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //  Permissions was granted, open the gallery
            }
            //  Permissions was not granted
            else {
                StringUtils.getInstance().showSnackbar(this, "No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void resetValues() {
        mPath = null;
        mMorphButtonUtility.initializeButton(selectFileButton, splitFilesButton);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_REQUEST_PICKFILE_CODE) {
            if (resultCode == RESULT_OK) {
                pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
            }
        }
    }

    @Override
    public void PickiTonUriReturned() {
        progressBar = new ProgressDialog(this);
        progressBar.setMessage("Waiting to receive file...");
        progressBar.setCancelable(false);
        progressBar.show();
    }

    @Override
    public void PickiTonStartListener() {
        if (progressBar.isShowing()) {
            progressBar.cancel();
        }
    }

    @Override
    public void PickiTonProgressUpdate(int progress) {

    }

    @Override
    public void PickiTonCompleteListener(String path, boolean wasDriveFile, boolean wasUnknownProvider, boolean wasSuccessful, String Reason) {
        if (wasDriveFile) {
            StringUtils.getInstance().showSnackbar(this, "Drive file was selected");
        } else if (wasUnknownProvider) {
            StringUtils.getInstance().showSnackbar(this, "File was selected from unknown provider");
        } else {
            StringUtils.getInstance().showSnackbar(this, "Local file was selected");
        }

        //  Chick if it was successful
        if (wasSuccessful) {
            setTextAndActivateButtons(path);
        } else {
            StringUtils.getInstance().showSnackbar(this, "Error, please see the log..");
        }
    }

    //  Deleting the temporary file if it exists
    @Override
    public void onBackPressed() {
        pickiT.deleteTemporaryFile(this);
        super.onBackPressed();
    }

    //  Deleting the temporary file if it exists
    //  As we know, this might not even be called if the system kills the application before onDestroy is called
    //  So, it is best to call pickiT.deleteTemporaryFile(); as soon as you are done with the file
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            pickiT.deleteTemporaryFile(this);
        }
    }

    private void setTextAndActivateButtons(String path) {
        mSplittedFiles.setVisibility(View.GONE);
        splitFilesSuccessText.setVisibility(View.GONE);
        mPath = path;
        String defaultSplitConfig = getDefaultSplitConfig(mPath);
        if (defaultSplitConfig != null) {
            mMorphButtonUtility.setTextAndActivateButtons(path, selectFileButton, splitFilesButton);
            mSplitConfitEditText.setVisibility(View.VISIBLE);
            mSplitConfitEditText.setText(defaultSplitConfig);
        } else
            resetValues();
    }

    /**
     * Gets the total number of pages in the
     * selected PDF and returns them in
     * default format for single page pdfs
     * (1,2,3,4,5,)
     */
    private String getDefaultSplitConfig(String mPath) {
        StringBuilder splitConfig = new StringBuilder();
        ParcelFileDescriptor fileDescriptor = null;
        try {
            if (mPath != null)
                fileDescriptor = ParcelFileDescriptor.open(new File(mPath), MODE_READ_ONLY);
            if (fileDescriptor != null) {
                PdfRenderer renderer = new PdfRenderer(fileDescriptor);
                final int pageCount = renderer.getPageCount();
                for (int i = 1; i <= pageCount; i++) {
                    splitConfig.append(i).append(",");
                }
            }
        } catch (Exception er) {
            er.printStackTrace();
            StringUtils.getInstance().showSnackbar(mActivity, "PDF is password protected.");
            return null;
        }
        return splitConfig.toString();
    }

    @Override
    public void onFileItemClick(String path) {
        mFileUtils.openFile(path, FileUtils.FileType.e_PDF);
    }
}