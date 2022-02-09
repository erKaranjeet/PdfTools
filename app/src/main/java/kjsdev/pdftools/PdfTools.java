package kjsdev.pdftools;

import android.content.Context;
import android.os.Environment;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.google.firebase.FirebaseApp;

import static kjsdev.pdftools.BuildConfig.DEBUG;

public class PdfTools extends MultiDexApplication {

    public PdfTools() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}