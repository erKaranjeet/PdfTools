package com.pdftools.activities;

import androidx.annotation.NonNull;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;
import com.dd.morphingbutton.MorphingButton;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;
import com.pdftools.R;
import com.pdftools.utils.Constants;
import com.pdftools.utils.DialogUtils;
import com.pdftools.utils.ExcelToPDFAsync;
import com.pdftools.utils.FileUtils;
import com.pdftools.utils.MorphButtonUtility;
import com.pdftools.utils.PermissionsUtils;
import com.pdftools.utils.RealPathUtil;
import com.pdftools.utils.StringUtils;
import com.pdftools.utils.database.DatabaseHelper;
import com.pdftools.utils.interfaces.OnPDFCreatedInterface;

import java.util.ArrayList;

import static com.pdftools.utils.Constants.READ_WRITE_PERMISSIONS;
import static com.pdftools.utils.Constants.STORAGE_LOCATION;
import static com.pdftools.utils.StringUtils.FOLDER_LOCATION;


public class ExcelToPdfActivity extends AppCompatActivity implements OnPDFCreatedInterface, PickiTCallbacks {

    TextView mTextView, tvPath, tvFolderLocation;
    MorphingButton mOpenPdf;
    MorphingButton mCreateExcelPdf, mSelectExcel;

    private Activity mActivity;
    private FileUtils mFileUtils;
    private Uri mExcelFileUri;
    private String mRealPath;
    private String mPath;

    private SharedPreferences mSharedPreferences;
    private MorphButtonUtility mMorphButtonUtility;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT = 1;
    private boolean mPermissionGranted = false;
    private boolean mButtonClicked = false;
    private final int mFileSelectCode = 0;
    private MaterialDialog mMaterialDialog;
    private boolean mPasswordProtected = false;
    private String mPassword;

    private StorageChooser.Builder builder = new StorageChooser.Builder();
    private StorageChooser chooser;
    private PickiT pickiT;
    //Permissions
    private static final int SELECT_VIDEO_REQUEST = 777;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private ProgressDialog progressBar;
    private StringUtils stringUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_to_pdf);

        mTextView = (TextView) findViewById(R.id.tv_excel_file_name_bottom);
        mOpenPdf = (MorphingButton) findViewById(R.id.open_pdf);
        mCreateExcelPdf = (MorphingButton) findViewById(R.id.create_excel_to_pdf);
        mSelectExcel = (MorphingButton) findViewById(R.id.select_excel_file);
        tvPath = (TextView) findViewById(R.id.tv_exceltopdf_path);
        tvFolderLocation = (TextView) findViewById(R.id.tv_excel_folder);

        stringUtils = new StringUtils(this);
        if (stringUtils.GetPreferences(FOLDER_LOCATION).equals("")) {
            stringUtils.SavePreferences(FOLDER_LOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        }
        tvFolderLocation.setText(StringUtils.getInstance().getDefaultStorageLocation(this));

        mActivity = (Activity) ExcelToPdfActivity.this;
        mFileUtils = new FileUtils(mActivity);
        pickiT = new PickiT(this, this, mActivity);


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

        findViewById(R.id.btn_excel_choose).setOnClickListener(new View.OnClickListener() {
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


        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mPermissionGranted = PermissionsUtils.getInstance().checkRuntimePermissions(this, READ_WRITE_PERMISSIONS);
        mMorphButtonUtility = new MorphButtonUtility(mActivity);
        mMorphButtonUtility.morphToGrey(mCreateExcelPdf, mMorphButtonUtility.integer());
        mCreateExcelPdf.setEnabled(false);

        mSelectExcel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectExcelFile();
            }
        });

        mCreateExcelPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExcelToPdf();
            }
        });

        mOpenPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPdf();
            }
        });
    }

    private StorageChooser.Theme getScTheme(boolean isChecked) {
        StorageChooser.Theme theme = new StorageChooser.Theme(getApplicationContext());
        theme.setScheme((isChecked) ? getResources().getIntArray(R.array.paranoid_theme) : theme.getDefaultScheme());
        return theme;
    }

    public void selectExcelFile() {
        if (!mButtonClicked) {
//            Uri uri = Uri.parse(Environment.getRootDirectory() + "/");
//            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//            intent.setDataAndType(uri, "*/*");
//            intent.addCategory(Intent.CATEGORY_OPENABLE);
//            try {
//                startActivityForResult(Intent.createChooser(intent, "Select a file to upload"),
//                        mFileSelectCode);
//            } catch (android.content.ActivityNotFoundException ex) {
//                StringUtils.getInstance().showSnackbar(mActivity, "Please install a file manager");
//            }

            //  first check if permissions was granted
            if (checkSelfPermission()) {
                Intent intent;
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                } else {
                    intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.INTERNAL_CONTENT_URI);
                }
                //  In this example we will set the type to video
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra("return-data", true);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent, SELECT_VIDEO_REQUEST);
            }
            mButtonClicked = true;
        }
    }

    /**
     * This function opens a dialog to enter the file name of
     * the converted file.
     */
    public void openExcelToPdf() {
        if (!mPermissionGranted)
            getRuntimePermissions();

        new MaterialDialog.Builder(mActivity)
                .title("Creating PDF")
                .content("Enter file name")
                .input("Example : abc", null, (dialog, input) -> {
                    if (StringUtils.getInstance().isEmpty(input)) {
                        StringUtils.getInstance().showSnackbar(mActivity, "Name cannot be blank");
                    } else {
                        final String inputName = input.toString();
                        if (!mFileUtils.isFileExist(inputName + getString(R.string.pdf_ext))) {
                            convertToPdf(inputName);
                        } else {
                            MaterialDialog.Builder builder = DialogUtils.getInstance().createOverwriteDialog(mActivity);
                            builder.onPositive((dialog12, which) -> convertToPdf(inputName))
                                    .onNegative((dialog1, which) -> openExcelToPdf())
                                    .show();
                        }
                    }
                })
                .show();
    }

    public void openPdf() {
        mFileUtils.openFile(mPath, FileUtils.FileType.e_PDF);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mButtonClicked = false;
        if (requestCode == mFileSelectCode) {
            if (resultCode == RESULT_OK) {
                mExcelFileUri = data.getData();
                mRealPath = RealPathUtil.getInstance().getRealPath(this, mExcelFileUri);
                tvPath.setText(mRealPath);
                processUri();
            }
        } else if (requestCode == SELECT_VIDEO_REQUEST) {
            if (resultCode == RESULT_OK) {
                mExcelFileUri = data.getData();
                pickiT.getPath(data.getData(), Build.VERSION.SDK_INT);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (grantResults.length < 1)
//            return;
//        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT) {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                mPermissionGranted = true;
//                openExcelToPdf();
//                StringUtils.getInstance().showSnackbar(mActivity, "Permissions Given!");
//            } else
//                StringUtils.getInstance().showSnackbar(mActivity, "Insufficient Permissions!");
//        }
//    }

    private void getRuntimePermissions() {
        PermissionsUtils.getInstance().requestRuntimePermissions(this,
                READ_WRITE_PERMISSIONS,
                PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT);
    }

    private void convertToPdf(String mFilename) {
//        String mStorePath = mSharedPreferences.getString(STORAGE_LOCATION,
//                StringUtils.getInstance().getDefaultStorageLocation(this));
        String mStorePath = stringUtils.GetPreferences(FOLDER_LOCATION);
        mPath = mStorePath + "/" + mFilename + mActivity.getString(R.string.pdf_ext);
        new ExcelToPDFAsync(mRealPath, mPath, this, mPasswordProtected, mPassword).execute();
    }

    private void processUri() {
        StringUtils.getInstance().showSnackbar(mActivity, "Excel file selected");
        String fileName = mFileUtils.getFileName(mExcelFileUri);
        if (fileName != null && !fileName.endsWith(Constants.excelExtension) &&
                !fileName.endsWith(Constants.excelWorkbookExtension)) {
            StringUtils.getInstance().showSnackbar(mActivity, "Sorry! This file type is not supported currently.");
            return;
        }

        fileName = "Excel file selected " + fileName;
        mTextView.setText(fileName);
        mTextView.setVisibility(View.VISIBLE);
        mCreateExcelPdf.setEnabled(true);
        mCreateExcelPdf.unblockTouch();
        mMorphButtonUtility.morphToSquare(mCreateExcelPdf, mMorphButtonUtility.integer());
        mOpenPdf.setVisibility(View.GONE);
    }

    @Override
    public void onPDFCreationStarted() {
        mMaterialDialog = DialogUtils.getInstance().createAnimationDialog(mActivity);
        mMaterialDialog.show();
    }

    @Override
    public void onPDFCreated(boolean success, String path) {
        if (mMaterialDialog != null && mMaterialDialog.isShowing())
            mMaterialDialog.dismiss();
        if (!success) {
            StringUtils.getInstance().showSnackbar(mActivity, "Sorry! PDF was not created. Please try again");
            mTextView.setVisibility(View.GONE);
            mMorphButtonUtility.morphToGrey(mCreateExcelPdf, mMorphButtonUtility.integer());
            mCreateExcelPdf.setEnabled(false);
            mExcelFileUri = null;
            return;
        }
        StringUtils.getInstance().getSnackbarwithAction(mActivity, R.string.snackbar_pdfCreated)
                .setAction(R.string.snackbar_viewAction,
                        v -> mFileUtils.openFile(mPath, FileUtils.FileType.e_PDF))
                .show();
//        new DatabaseHelper(mActivity).insertRecord(mPath, mActivity.getString(R.string.created));
        mTextView.setVisibility(View.GONE);
        mOpenPdf.setVisibility(View.VISIBLE);
        mMorphButtonUtility.morphToSuccess(mCreateExcelPdf);
        mCreateExcelPdf.blockTouch();
        mMorphButtonUtility.morphToGrey(mCreateExcelPdf, mMorphButtonUtility.integer());
        mExcelFileUri = null;
        mPasswordProtected = false;
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
//            mExcelFileUri = data.getData();
//            mRealPath = RealPathUtil.getInstance().getRealPath(this, mExcelFileUri);
            mRealPath = path;
            tvPath.setText(mRealPath);
            processUri();
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