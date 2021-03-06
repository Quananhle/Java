package com.example.multi_notepad.Main;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.JsonWriter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.multi_notepad.About.AboutActivity;
import com.example.multi_notepad.Edit.EditActivity;
import com.example.multi_notepad.Edit.Notes;
import com.example.multi_notepad.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.*;
import java.util.*;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "MainActivity";
    private static final int ADD_REQUEST_CODE = 1;
    private static final int EDIT_REQUEST_CODE = 2;
    private RecyclerView recyclerView;
    private final List<Notes> notesList = new ArrayList<>();
    private NoteAdapter noteAdapter;
    private Notes note;
    private int currentNote = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler);
        //Data to recyclerview adapter
        noteAdapter = new NoteAdapter(notesList, this);
        recyclerView.setAdapter(noteAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.readFile();
        if (notesList.size() > 0) {
            getSupportActionBar().setTitle(getString(R.string.file_name)
                    + " (" + notesList.size() + ")");
        }
        noteAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        //Tap on a note to edit
        currentNote = recyclerView.getChildLayoutPosition(v);
        Notes note = notesList.get(currentNote);
        Intent editIntent = new Intent(MainActivity.this, EditActivity.class);
        editIntent.putExtra("UPDATED_TITLE", note.getName());
        editIntent.putExtra("UPDATED_DESCRIPTION", note.getBody());
        startActivityForResult(editIntent, EDIT_REQUEST_CODE);
        noteAdapter.notifyDataSetChanged();
    }

    // From OnLongClickListener
    @Override
    public boolean onLongClick(View v) {
        //Long press a note trigger a Delete dialog
        int pos = recyclerView.getChildLayoutPosition(v);
        note = notesList.get(pos);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("DELETE NOTE \'" + note.getName() + "\'?");
        //if user selected 'Yes'
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                notesList.remove(note);
                //set note to null
                note = null;
                noteAdapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "NOTE REMOVED", Toast.LENGTH_SHORT).show();
                if (notesList.size() >= 0) {
                    getSupportActionBar().setTitle(getString(R.string.file_name)
                            + " (" + (notesList.size()) + ")");
                }
            }
        });
        //if user selected 'No', do nothing
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ;
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return false;
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "HAVE A GOOD DAY!", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }

    @Override
    public void onPause() {
        this.writeFile();
        super.onPause();
    }

    @Override
    public void onStop() {
        this.writeFile();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.createButton:
                Toast.makeText(this, "NEW", Toast.LENGTH_SHORT).show();
                Intent intentEdit = new Intent(MainActivity.this, EditActivity.class);
                startActivityForResult(intentEdit, ADD_REQUEST_CODE);
                return true;
            case R.id.aboutButton:
                Toast.makeText(this, "INFO", Toast.LENGTH_SHORT).show();
                Intent intentAbout = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intentAbout);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent receivedData) {
        super.onActivityResult(requestCode, resultCode, receivedData);

        switch (requestCode) {
            //create a new note
            case ADD_REQUEST_CODE:
                if (resultCode == RESULT_CANCELED) {
                    String newTitle = receivedData.getStringExtra("NEW_TITLE");
                    String newDescription = receivedData.getStringExtra("NEW_DESCRIPTION");
                    notesList.add(0, new Notes(newTitle, newDescription, getCurrentTime()));
                    getSupportActionBar().setTitle(getString(R.string.app_name) + " (" + notesList.size() + ")");
                    noteAdapter.notifyDataSetChanged();
                }
                break;
            //edit an existing note
            case EDIT_REQUEST_CODE:
                if (resultCode == RESULT_CANCELED) {
                    String updatedTitle = receivedData.getStringExtra("UPDATED_TITLE");
                    String updatedDescription = receivedData.getStringExtra("UPDATED_DESCRIPTION");
                    notesList.remove(currentNote);
                    notesList.add(0, new Notes(updatedTitle, updatedDescription, getCurrentTime()));
                    getSupportActionBar().setTitle(getString(R.string.app_name) + "(" + notesList.size() + ")");
                    noteAdapter.notifyDataSetChanged();
                }
                //otherwise, no changes have been made
                else if (resultCode == RESULT_OK) {
                    currentNote = RESULT_OK;
                }
                break;
        }
    }
    //====================== *** HELPER•METHODS *** ======================//
    private void readFile() {
        try {
            InputStream inputStream = getApplicationContext().openFileInput(getString(R.string.notes_file));
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(inputStream, getString(R.string.encoding)));
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            JSONArray jsonArray = new JSONArray(stringBuilder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject noteObject = jsonArray.getJSONObject(i);
                String noteTitle = noteObject.getString("noteTitle");
                String noteDescription = noteObject.getString("noteDescription");
                String noteLastModified = noteObject.getString("noteLastModified");
                notesList.add(new Notes(noteTitle, noteDescription, noteLastModified));
            }
            noteAdapter.notifyDataSetChanged();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (JSONException jsone) {
            jsone.printStackTrace();
        }
    }

    private void writeFile() {
        try {
            FileOutputStream outputStream = getApplicationContext().openFileOutput(
                    getString(R.string.notes_file), Context.MODE_PRIVATE);
            JsonWriter jsonWriter = new JsonWriter(new OutputStreamWriter(
                    outputStream, getString(R.string.encoding)));
            jsonWriter.setIndent(" ");
            jsonWriter.beginArray();
            for (int i = 0; i < notesList.size(); i++) {
                jsonWriter.beginObject();
                jsonWriter.name("noteTitle").value(notesList.get(i).getName());
                jsonWriter.name("noteDescription").value(notesList.get(i).getBody());
                jsonWriter.name("noteLastModified").value(notesList.get(i).getTime());
                jsonWriter.endObject();
            }
            jsonWriter.endArray();
            jsonWriter.close();
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    public static String getCurrentTime(){
        Date D = Calendar.getInstance().getTime();
        SimpleDateFormat SDF = new SimpleDateFormat("EEE MMM d, h:mm a");
        String lastSaveDate = SDF.format(D).toString();
        return lastSaveDate;
    }
}
