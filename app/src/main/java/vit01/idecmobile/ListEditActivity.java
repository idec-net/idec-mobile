package vit01.idecmobile;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.util.ArrayList;

import vit01.idecmobile.Core.Config;

public class ListEditActivity extends AppCompatActivity {
    ArrayList<String> contents;
    ArrayAdapter<String> contents_adapter;
    ListView listView, action;
    EditText echoEdit;
    AlertDialog editEchoarea;
    int echoPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_edit);
        getSupportActionBar().setTitle("Правка списка эх");

        Intent in = getIntent();
        String listType = in.getStringExtra("type");

        switch (listType) {
            case "fromstation":
                int stationIndex = in.getIntExtra("index", 0);
                contents = Config.values.stations.get(stationIndex).echoareas;
                break;
            case "offline":
                contents = Config.values.offlineEchoareas;
                break;
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_ev);
        IconicsDrawable add_icon = new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_add).color(Color.WHITE).sizeDp(24);
        fab.setImageDrawable(add_icon);

        contents_adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, contents);

        listView = (ListView) findViewById(R.id.contents);
        listView.setAdapter(contents_adapter);

        final View alertView = getLayoutInflater().inflate(R.layout.alert_echo_menu, null);

        echoEdit = (EditText) alertView.findViewById(R.id.edit_echoarea);
        action = (ListView) alertView.findViewById(R.id.echo_menu_options);

        fab.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {
                                       String gotText = "echoarea.new";
                                       contents.add(gotText);
                                       contents_adapter.notifyDataSetChanged();
                                       echoPosition = contents.size() - 1;

                                       echoEdit.setText(gotText);
                                       editEchoarea.show();
                                   }
                               }
        );

        editEchoarea = new AlertDialog.Builder(ListEditActivity.this)
                .setTitle("Правка эхоконференции")
                .setView(alertView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String gotText = echoEdit.getText().toString();
                        if (!gotText.equals("")) {
                            contents.set(echoPosition, gotText);
                            contents_adapter.notifyDataSetChanged();
                        }
                    }
                }).create();

        action.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String element;

                switch (position) {
                    case 0:
                        element = contents.remove(echoPosition);
                        contents.add(0, element);
                        echoPosition = 0;
                        break;
                    case 1:
                        if (echoPosition > 0) {
                            element = contents.remove(echoPosition);
                            echoPosition--;
                            contents.add(echoPosition, element);
                        }
                        break;
                    case 2:
                        if (echoPosition < contents.size() - 1) {
                            element = contents.remove(echoPosition);
                            echoPosition++;
                            contents.add(echoPosition, element);
                        }
                        break;
                    case 3:
                        element = contents.remove(echoPosition);
                        contents.add(element);
                        echoPosition = contents.size() - 1;
                        break;
                    case 4:
                        if (contents.size() > 1) {
                            contents.remove(echoPosition);
                            editEchoarea.cancel();
                        } else {
                            Toast.makeText(ListEditActivity.this,
                                    "В списке должна быть хотя бы одна эхоконференция!",
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
                contents_adapter.notifyDataSetChanged();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                echoPosition = position;
                echoEdit.setText(((TextView) view).getText());
                editEchoarea.show();
                return true;
            }
        });
    }

    @Override
    protected void onStop() {
        Config.writeConfig(this);
        super.onStop();
    }
}