package com.pdftools.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.airbnb.lottie.LottieAnimationView;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;
import com.dd.morphingbutton.MorphingButton;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;
import com.pdftools.R;
import com.pdftools.adapters.MergeFilesAdapter;
import com.pdftools.adapters.MergeSelectedFilesAdapter;
import com.pdftools.utils.DialogUtils;
import com.pdftools.utils.FileUtils;
import com.pdftools.utils.MergePdf;
import com.pdftools.utils.MorphButtonUtility;
import com.pdftools.utils.RealPathUtil;
import com.pdftools.utils.StringUtils;
import com.pdftools.utils.ViewFilesDividerItemDecoration;
import com.pdftools.utils.interfaces.MergeFilesListener;

import java.util.ArrayList;
import java.util.Collections;

import static com.pdftools.utils.Constants.MASTER_PWD_STRING;
import static com.pdftools.utils.Constants.STORAGE_LOCATION;
import static com.pdftools.utils.Constants.appName;
import static com.pdftools.utils.StringUtils.FOLDER_LOCATION;

public class MergePdfActivity extends AppCompatActivity implements MergeFilesAdapter.OnClickListener, MergeFilesListener,
        MergeSelectedFilesAdapter.OnFileItemClickListener, AdapterView.OnItemClickListener, PickiTCallbacks {

    private TextView tvFolderLocation;
    MorphingButton mergeBtn;
    Button mSelectFiles;
    RecyclerView mSelectedFiles;

    private Activity mActivity;
    private String mCheckbtClickTag = "";
    private static final int INTENT_REQUEST_PICK_FILE_CODE = 10;
    private MorphButtonUtility mMorphButtonUtility;
    private ArrayList<String> mFilePaths;
    private FileUtils mFileUtils;
    private MergeSelectedFilesAdapter mMergeSelectedFilesAdapter;
    private MaterialDialog mMaterialDialog;
    private String mHomePath;
    private SharedPreferences mSharedPrefs;
    private boolean mPasswordProtected = false;
    private String mPassword = "";

    private PickiT pickiT;
    //Permissions
    private static final int SELECT_VIDEO_REQUEST = 777;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private ProgressDialog progressBar;
    private StringUtils stringUtils;
    private StorageChooser.Builder builder = new StorageChooser.Builder();
    private StorageChooser chooser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_pdf);

        mergeBtn = (MorphingButton) findViewById(R.id.mergebtn);
        mSelectFiles = (Button) findViewById(R.id.selectFiles);
        mSelectedFiles = (RecyclerView) findViewById(R.id.selected_files);

//        mCheckbtClickTag = savedInstanceState.getString("savText");
        tvFolderLocation = (TextView) findViewById(R.id.tv_merge_folder);

        stringUtils = new StringUtils(this);
        if (stringUtils.GetPreferences(FOLDER_LOCATION).equals("")) {
            stringUtils.SavePreferences(FOLDER_LOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        }
        tvFolderLocation.setText(StringUtils.getInstance().getDefaultStorageLocation(this));
        mActivity = (Activity) MergePdfActivity.this;
        mFileUtils = new FileUtils(mActivity);
        pickiT = new PickiT(this, this, mActivity);

        mFilePaths = new ArrayList<>();
        mMergeSelectedFilesAdapter = new MergeSelectedFilesAdapter(mActivity, mFilePaths, this);
        mMorphButtonUtility = new MorphButtonUtility(mActivity);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
//        mHomePath = mSharedPrefs.getString(STORAGE_LOCATION, StringUtils.getInstance().getDefaultStorageLocation(this));
        mHomePath = stringUtils.GetPreferences(FOLDER_LOCATION) + "/";

        mSelectedFiles.setAdapter(mMergeSelectedFilesAdapter);
        mSelectedFiles.addItemDecoration(new ViewFilesDividerItemDecoration(mActivity));
        setMorphingButtonState(false);

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

        findViewById(R.id.btn_merge_choose).setOnClickListener(new View.OnClickListener() {
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
                        mHomePath = stringUtils.GetPreferences(FOLDER_LOCATION) + "/";
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

        mSelectFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAddingPDF();
            }
        });

        mergeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mergeFiles();
            }
        });
    }

    private StorageChooser.Theme getScTheme(boolean isChecked) {
        StorageChooser.Theme theme = new StorageChooser.Theme(getApplicationContext());
        theme.setScheme((isChecked) ? getResources().getIntArray(R.array.paranoid_theme) : theme.getDefaultScheme());
        return theme;
    }

    void startAddingPDF() {
//        startActivityForResult(mFileUtils.getFileChooser(), INTENT_REQUEST_PICK_FILE_CODE);

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
            startActivityForResult(intent, SELECT_VIDEO_REQUEST);
        }
    }

    void mergeFiles() {
        String[] pdfpaths = mFilePaths.toArray(new String[0]);
        String masterpwd = mSharedPrefs.getString(MASTER_PWD_STRING, appName);
        new MaterialDialog.Builder(mActivity)
                .title("Creating PDF")
                .content("Enter file name")
                .input("Example : abc", null, (dialog, input) -> {
                    if (StringUtils.getInstance().isEmpty(input)) {
                        StringUtils.getInstance().showSnackbar(mActivity, "Name cannot be blank");
                    } else {
                        if (!mFileUtils.isFileExist(input + getString(R.string.pdf_ext))) {
                            new MergePdf(input.toString(), mHomePath, mPasswordProtected,
                                    mPassword, this, masterpwd).execute(pdfpaths);
                        } else {
                            MaterialDialog.Builder builder = DialogUtils.getInstance().createOverwriteDialog(mActivity);
                            builder.onPositive((dialog12, which) -> new MergePdf(input.toString(),
                                    mHomePath, mPasswordProtected, mPassword,
                                    this, masterpwd).execute(pdfpaths))
                                    .onNegative((dialog1, which) -> mergeFiles()).show();
                        }
                    }
                })
                .show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null || resultCode != RESULT_OK || data.getData() == null)
            return;
        if (requestCode == INTENT_REQUEST_PICK_FILE_CODE) {
            //Getting Absolute Path
            String path = RealPathUtil.getInstance().getRealPath(this, data.getData());
            mFilePaths.add(path);
            mMergeSelectedFilesAdapter.notifyDataSetChanged();
            StringUtils.getInstance().showSnackbar(mActivity, "PDF has been added to the list.");
            if (mFilePaths.size() > 1 && !mergeBtn.isEnabled())
                setMorphingButtonState(true);
        } else if (requestCode == SELECT_VIDEO_REQUEST) {
            if (resultCode == RESULT_OK) {
                pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
            }
        }
    }

    //  Check if permissions was granted
    private boolean checkSelfPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    //  Handle permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //  Permissions was granted, open the gallery
            }
            //  Permissions was not granted
            else {
                StringUtils.getInstance().showSnackbar(this, "No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("savText", mCheckbtClickTag);
    }

    @Override
    public void onItemClick(String path) {
        if (mFilePaths.contains(path)) {
            mFilePaths.remove(path);
            StringUtils.getInstance().showSnackbar(mActivity, "PDF has been removed from the list.");
        } else {
            mFilePaths.add(path);
            StringUtils.getInstance().showSnackbar(mActivity, "PDF has been added to the list.");
        }

        mMergeSelectedFilesAdapter.notifyDataSetChanged();
        if (mFilePaths.size() > 1) {
            if (!mergeBtn.isEnabled()) setMorphingButtonState(true);
        } else {
            if (mergeBtn.isEnabled()) setMorphingButtonState(false);
        }
    }

    @Override
    public void resetValues(boolean isPDFMerged, String path) {
        mMaterialDialog.dismiss();

        if (isPDFMerged) {
            StringUtils.getInstance().getSnackbarwithAction(mActivity, R.string.pdf_merged)
                    .setAction(R.string.snackbar_viewAction,
                            v -> mFileUtils.openFile(path, FileUtils.FileType.e_PDF)).show();
//            new DatabaseHelper(mActivity).insertRecord(path,
//                    mActivity.getString(R.string.created));
        } else
            StringUtils.getInstance().showSnackbar(mActivity, R.string.file_access_error);

        setMorphingButtonState(false);
        mFilePaths.clear();
        mMergeSelectedFilesAdapter.notifyDataSetChanged();
        mPasswordProtected = false;
//        showEnhancementOptions();
    }

    @Override
    public void mergeStarted() {
        mMaterialDialog = DialogUtils.getInstance().createAnimationDialog(mActivity);
        mMaterialDialog.show();
    }

    @Override
    public void viewFile(String path) {
        mFileUtils.openFile(path, FileUtils.FileType.e_PDF);
    }

    @Override
    public void removeFile(String path) {
        mFilePaths.remove(path);
        mMergeSelectedFilesAdapter.notifyDataSetChanged();
        StringUtils.getInstance().showSnackbar(mActivity, "PDF has been removed from the list.");
        if (mFilePaths.size() < 2 && mergeBtn.isEnabled())
            setMorphingButtonState(false);
    }

    @Override
    public void moveUp(int position) {
        Collections.swap(mFilePaths, position, position - 1);
        mMergeSelectedFilesAdapter.notifyDataSetChanged();
    }

    @Override
    public void moveDown(int position) {
        Collections.swap(mFilePaths, position, position + 1);
        mMergeSelectedFilesAdapter.notifyDataSetChanged();
    }

    private void setMorphingButtonState(Boolean enabled) {
        if (enabled.equals(true))
            mMorphButtonUtility.morphToSquare(mergeBtn, mMorphButtonUtility.integer());
        else
            mMorphButtonUtility.morphToGrey(mergeBtn, mMorphButtonUtility.integer());

        mergeBtn.setEnabled(enabled);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


    }

    @Override
    public void PickiTonUriReturned() {
        //Show Progress Dialog
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
//  Check if it was a Drive/local/unknown provider file and display a Toast
        if (wasDriveFile) {
            StringUtils.getInstance().showSnackbar(this, "Drive file was selected");
        } else if (wasUnknownProvider) {
            StringUtils.getInstance().showSnackbar(this, "File was selected from unknown provider");
        } else {
            StringUtils.getInstance().showSnackbar(this, "Local file was selected");
        }

        //  Chick if it was successful
        if (wasSuccessful) {
            mFilePaths.add(path);
            mMergeSelectedFilesAdapter.notifyDataSetChanged();
            StringUtils.getInstance().showSnackbar(mActivity, "PDF has been added to the list.");
            if (mFilePaths.size() > 1 && !mergeBtn.isEnabled())
                setMorphingButtonState(true);
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
}