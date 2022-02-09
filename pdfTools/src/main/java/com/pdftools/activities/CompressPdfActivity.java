package com.pdftools.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;
import com.dd.morphingbutton.MorphingButton;
import com.hbisoft.pickit.PickiT;
import com.hbisoft.pickit.PickiTCallbacks;
import com.pdftools.R;
import com.pdftools.utils.DialogUtils;
import com.pdftools.utils.FileUtils;
import com.pdftools.utils.MorphButtonUtility;
import com.pdftools.utils.PDFUtils;
import com.pdftools.utils.StringUtils;
import com.pdftools.utils.interfaces.OnPDFCompressedInterface;

import java.io.File;
import java.util.ArrayList;

import static com.pdftools.utils.Constants.BUNDLE_DATA;
import static com.pdftools.utils.Constants.COMPRESS_PDF;
import static com.pdftools.utils.FileUtils.getFormattedSize;
import static com.pdftools.utils.StringUtils.FOLDER_LOCATION;

public class CompressPdfActivity extends AppCompatActivity implements PickiTCallbacks, OnPDFCompressedInterface {

    MorphingButton selectFileButton;
    MorphingButton createPdf;
    TextView mInfoText;
    EditText pagesInput;
    TextView mCompressionInfoText, tvFolderLocation;
    Button mViewPdf;

    private Activity mActivity;
    private String mPath;
    private MorphButtonUtility mMorphButtonUtility;
    private FileUtils mFileUtils;
    private PDFUtils mPDFUtils;
    private static final int INTENT_REQUEST_PICKFILE_CODE = 10;
    private static final int INTENT_REQUEST_REARRANGE_PDF = 11;
    private String mOperation;
    private MaterialDialog mMaterialDialog;
    private PickiT pickiT;
    private ProgressDialog progressBar;
    private Uri mUri;
    private StorageChooser.Builder builder = new StorageChooser.Builder();
    private StorageChooser chooser;
    private StringUtils stringUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compress_pdf);

        selectFileButton = (MorphingButton) findViewById(R.id.selectFile);
        createPdf = (MorphingButton) findViewById(R.id.pdfCreate);
        mInfoText = (TextView) findViewById(R.id.infoText);
        pagesInput = (EditText) findViewById(R.id.pages);
        mCompressionInfoText = (TextView) findViewById(R.id.compressionInfoText);
        mViewPdf = (Button) findViewById(R.id.view_pdf);
        tvFolderLocation = (TextView) findViewById(R.id.tv_compress_folder);

        stringUtils = new StringUtils(this);
        if (stringUtils.GetPreferences(FOLDER_LOCATION).equals("")) {
            stringUtils.SavePreferences(FOLDER_LOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        }
        tvFolderLocation.setText(StringUtils.getInstance().getDefaultStorageLocation(this));

        Bundle intent = getIntent().getExtras();
        mOperation = intent.getString(BUNDLE_DATA);

        mActivity = (Activity) CompressPdfActivity.this;
        mMorphButtonUtility = new MorphButtonUtility(mActivity);
        mFileUtils = new FileUtils(mActivity);
        mPDFUtils = new PDFUtils(mActivity);
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

        findViewById(R.id.btn_compress_choose).setOnClickListener(new View.OnClickListener() {
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

        createPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringUtils.getInstance().hideKeyboard(mActivity);
                if (mOperation.equals(COMPRESS_PDF)) {
                    compressPDF();
                }
            }
        });
    }

    private StorageChooser.Theme getScTheme(boolean isChecked) {
        StorageChooser.Theme theme = new StorageChooser.Theme(getApplicationContext());
        theme.setScheme((isChecked) ? getResources().getIntArray(R.array.paranoid_theme) : theme.getDefaultScheme());
        return theme;
    }

    private void resetValues() {
        mPath = null;
        pagesInput.setText(null);
        mMorphButtonUtility.initializeButton(selectFileButton, createPdf);
        switch (mOperation) {
//            case REORDER_PAGES:
//            case REMOVE_PAGES:
//            case ADD_PWD:
//            case REMOVE_PWd:
//                mInfoText.setVisibility(View.GONE);
//                pagesInput.setVisibility(View.GONE);
//                break;
            case COMPRESS_PDF:
                mInfoText.setText(R.string.compress_pdf_prompt);
                break;
        }
    }

    private void compressPDF() {
        String input = pagesInput.getText().toString();
        int check;
        try {
            check = Integer.parseInt(input);
            if (check > 100 || check <= 0 || mPath == null) {
                StringUtils.getInstance().showSnackbar(mActivity, "Invalid Entry");
            } else {
//                String outputPath = mPath.replace(mActivity.getString(R.string.pdf_ext),
//                        "_edited" + check + mActivity.getString(R.string.pdf_ext));
                String outputPath = stringUtils.GetPreferences(FOLDER_LOCATION) + "/_edited" + check + ".pdf";
                mPDFUtils.compressPDF(mPath, outputPath, 100 - check, CompressPdfActivity.this);
            }
        } catch (NumberFormatException e) {
            StringUtils.getInstance().showSnackbar(mActivity, "Invalid Entry");
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_REQUEST_PICKFILE_CODE) {
            if (resultCode == RESULT_OK) {
                mUri = data.getData();
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

    @Override
    public void onBackPressed() {
        pickiT.deleteTemporaryFile(this);
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            pickiT.deleteTemporaryFile(this);
        }
    }

    private void setTextAndActivateButtons(String path) {
        if (path == null) {
            StringUtils.getInstance().showSnackbar(mActivity, R.string.file_access_error);
            resetValues();
            return;
        }
        mPath = path;
        mCompressionInfoText.setVisibility(View.GONE);
        mViewPdf.setVisibility(View.GONE);
        mMorphButtonUtility.setTextAndActivateButtons(path, selectFileButton, createPdf);
    }

    @Override
    public void pdfCompressionStarted() {
        mMaterialDialog = DialogUtils.getInstance().createAnimationDialog(mActivity);
        mMaterialDialog.show();
    }

    @Override
    public void pdfCompressionEnded(String path, Boolean success) {
        mMaterialDialog.dismiss();
        if (success && path != null && mPath != null) {
            StringUtils.getInstance().getSnackbarwithAction(mActivity, R.string.snackbar_pdfCreated)
                    .setAction(R.string.snackbar_viewAction,
                            v -> mFileUtils.openFile(path, FileUtils.FileType.e_PDF)).show();
            File input = new File(mPath);
            File output = new File(path);
            viewPdfButton(path);
            mCompressionInfoText.setVisibility(View.VISIBLE);
            mCompressionInfoText.setText(String.format(mActivity.getString(R.string.compress_info),
                    getFormattedSize(input),
                    getFormattedSize(output)));
        } else {
            StringUtils.getInstance().showSnackbar(mActivity, "PDF is password protected.");
        }
        resetValues();
    }

    private void viewPdfButton(String path) {
        mViewPdf.setVisibility(View.VISIBLE);
        mViewPdf.setOnClickListener(v -> mFileUtils.openFile(path, FileUtils.FileType.e_PDF));
    }
}