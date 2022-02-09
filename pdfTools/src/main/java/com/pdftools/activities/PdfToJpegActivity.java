package com.pdftools.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;
import com.pdftools.R;
import com.pdftools.adapters.ExtractImagesAdapter;
import com.pdftools.utils.CommonCodeUtils;
import com.pdftools.utils.DialogUtils;
import com.pdftools.utils.FileUtils;
import com.pdftools.utils.PDFUtils;
import com.pdftools.utils.PdfToImages;
import com.pdftools.utils.RealPathUtils;
import com.pdftools.utils.StringUtils;
import com.pdftools.utils.interfaces.ExtractImagesListener;

import java.util.ArrayList;

import static com.pdftools.utils.Constants.BUNDLE_DATA;
import static com.pdftools.utils.Constants.PDF_TO_IMAGES;
import static com.pdftools.utils.StringUtils.FOLDER_LOCATION;


public class PdfToJpegActivity extends AppCompatActivity implements ExtractImagesListener, ExtractImagesAdapter.OnFileItemClickedListener {

    private TextView tvPath, tvMessage, tvCancel, tvFolderLocation;
    private AppCompatButton btnChoose, btnConvert;
    private RecyclerView rvFiles;

    public static Uri uri;
    public static String path = "";
    private PDFUtils mPDFUtils;
    private FileUtils mFileUtils;
    private String[] mInputPassword;
    private String mOperation;
    private ArrayList<String> mOutputFilePaths;
    private MaterialDialog mMaterialDialog;
    private StorageChooser.Builder builder = new StorageChooser.Builder();
    private StorageChooser chooser;
    private StringUtils stringUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_to_jpeg);

        Bundle intent = getIntent().getExtras();
        mOperation = intent.getString(BUNDLE_DATA);

        tvPath = (TextView) findViewById(R.id.tv_pdf_to_jpg_path);
        btnChoose = (AppCompatButton) findViewById(R.id.btn_pdf_to_jpg_select);
        btnConvert = (AppCompatButton) findViewById(R.id.btn_pdf_to_jpg_convert);
        tvMessage = (TextView) findViewById(R.id.pdfToImagesText);
        rvFiles = (RecyclerView) findViewById(R.id.rv_pdf_to_jpg_data);
        tvCancel = (TextView) findViewById(R.id.tv_pdf_to_jpg_cancel);
        tvFolderLocation = (TextView) findViewById(R.id.tv_pdf_to_jpg_folder);

        stringUtils = new StringUtils(this);
        if (stringUtils.GetPreferences(FOLDER_LOCATION).equals("")) {
            stringUtils.SavePreferences(FOLDER_LOCATION, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        }
        tvFolderLocation.setText(StringUtils.getInstance().getDefaultStorageLocation(this));

        mFileUtils = new FileUtils(this);
        mPDFUtils = new PDFUtils(this);

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

        findViewById(R.id.btn_pdf_to_jpg_choose).setOnClickListener(new View.OnClickListener() {
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

        btnChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String folderPath = Environment.getExternalStorageDirectory() + "/";
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                Uri myUri = Uri.parse(folderPath);
                intent.setDataAndType(myUri, "application/pdf");
                startActivityForResult(Intent.createChooser(intent, "Select PDF file"), 101);
            }
        });

        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (mPDFUtils.isPDFEncrypted(path)) {
//                    mInputPassword = new String[1];
//                    new MaterialDialog.Builder(PdfToJpegActivity.this)
//                            .title("Enter Password")
//                            .content(R.string.decrypt_protected_file)
//                            .inputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
//                            .input(null, null, (dialog, input) -> {
//                                if (StringUtils.getInstance().isEmpty(input)) {
//                                    StringUtils.getInstance().showSnackbar(PdfToJpegActivity.this, "Name cannot be blank");
//                                } else {
//                                    final String inputName = input.toString();
//                                    mInputPassword[0] = inputName;
//                                    pdfToImage(mInputPassword);
//                                }
//                            })
//                            .show();
//                } else {
                    pdfToImage(mInputPassword);
//                }
            }
        });

        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private StorageChooser.Theme getScTheme(boolean isChecked) {
        StorageChooser.Theme theme = new StorageChooser.Theme(getApplicationContext());
        theme.setScheme((isChecked) ? getResources().getIntArray(R.array.paranoid_theme) : theme.getDefaultScheme());
        return theme;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            uri = data.getData();
            path = RealPathUtils.getInstance().getRealPath(this, uri);

            btnConvert.setEnabled(true);
            btnConvert.setClickable(true);
            btnConvert.setBackgroundDrawable(getDrawable(R.drawable.btn_back_rounded));
            tvPath.setText(path);
        }
    }

    private void pdfToImage(String[] mInputPassword) {
        if (mOperation.equals(PDF_TO_IMAGES)) {
            new PdfToImages(this, mInputPassword, path, uri, this).execute();
        }
//        else
//            new ExtractImages(path, this).execute();
    }

    /**
     * initializes interactive views
     */
    @Override
    public void resetView() {
        path = null;
//        mMorphButtonUtility.initializeButton(mSelectFileButton, mCreateImagesButton);
    }

    /**
     * displays progress indicator
     */
    @Override
    public void extractionStarted() {
        mMaterialDialog = DialogUtils.getInstance().createAnimationDialog(this);
        mMaterialDialog.show();
    }

    /**
     * updates recycler view list items based with the generated images
     *
     * @param imageCount      number of generated images
     * @param outputFilePaths path for each generated image
     */
    @Override
    public void updateView(int imageCount, ArrayList<String> outputFilePaths) {

        mMaterialDialog.dismiss();
        resetView();
        mOutputFilePaths = outputFilePaths;

        CommonCodeUtils.getInstance().updateView(this, imageCount, outputFilePaths,
                tvMessage, rvFiles, this);
    }

    @Override
    public void onFileItemClick(String path) {
        mFileUtils.openImage(path);
    }
}