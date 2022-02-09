package kjsdev.pdftools;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.pdftools.activities.CompressPdfActivity;
import com.pdftools.activities.ExcelToPdfActivity;
import com.pdftools.activities.InvertPdfActivity;
import com.pdftools.activities.MergePdfActivity;
import com.pdftools.activities.PdfToJpegActivity;
import com.pdftools.activities.SplitPdfActivity;
import com.pdftools.utils.StringUtils;

import java.io.File;

import static com.pdftools.utils.Constants.BUNDLE_DATA;
import static com.pdftools.utils.Constants.COMPRESS_PDF;
import static com.pdftools.utils.Constants.PDF_TO_IMAGES;
import static com.pdftools.utils.Constants.pdfDirectory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, 1);
        }

        String root = Environment.getExternalStorageDirectory().toString();
        File directory = new File(root + pdfDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public void openPdfToJpg(View view) {
        startActivity(new Intent(this, PdfToJpegActivity.class).putExtra(BUNDLE_DATA, PDF_TO_IMAGES));
    }

    public void openExcelToPdf(View view) {
        startActivity(new Intent(this, ExcelToPdfActivity.class));
    }

    public void openMergePdf(View view) {
        startActivity(new Intent(this, MergePdfActivity.class));
    }

    public void openInvertPdf(View view) {
        startActivity(new Intent(this, InvertPdfActivity.class));
    }

    public void openSplitPdf(View view) {
        startActivity(new Intent(this, SplitPdfActivity.class));
    }

    public void openCompressPdf(View view) {
        startActivity(new Intent(this, CompressPdfActivity.class).putExtra(BUNDLE_DATA, COMPRESS_PDF));
    }
}