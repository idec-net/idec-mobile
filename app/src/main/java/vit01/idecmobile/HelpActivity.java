package vit01.idecmobile;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import vit01.idecmobile.Core.Config;
import vit01.idecmobile.Core.SimpleFunctions;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Config.appTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        InputStream licenseFile = getResources().openRawResource(R.raw.gpl);
        Spanned GPLHtml = null;

        try {
            GPLHtml = Html.fromHtml(SimpleFunctions.readIt(licenseFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        TextView licenceTextView = (TextView) findViewById(R.id.text_GPL);
        licenceTextView.setText(GPLHtml);
    }
}