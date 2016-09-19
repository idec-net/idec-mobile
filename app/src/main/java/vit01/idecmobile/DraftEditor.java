package vit01.idecmobile;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

public class DraftEditor extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draft_editor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Написать");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_compose, menu);

        Context context = getApplicationContext();

        menu.findItem(R.id.action_compose_save).setIcon(new IconicsDrawable
                (context, GoogleMaterial.Icon.gmd_save).actionBar().color(Color.WHITE));
        menu.findItem(R.id.action_compose_delete).setIcon(new IconicsDrawable
                (context, GoogleMaterial.Icon.gmd_delete).actionBar().color(Color.WHITE));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_compose_save) {
            Toast.makeText(DraftEditor.this, "Пробуем сохранить сообщение", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.action_compose_delete) {
            Toast.makeText(DraftEditor.this, "Удаляем черновик", Toast.LENGTH_SHORT).show();
            // TODO: сделать надо!
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // TODO: здесь сохраняем всё и/или спрашиваем
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
