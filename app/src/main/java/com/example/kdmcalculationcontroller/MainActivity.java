package com.example.kdmcalculationcontroller;

import android.app.Activity;
import android.content.Intent;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {
    private EditText usernameEditText;
    private String privateKey;
    private static final int REQUEST_PICK_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Fragment kodu odpowiedzialny za obsługę interfejsu użytkownika
        usernameEditText = findViewById(R.id.usernameEditText);
        Button saveButton = findViewById(R.id.saveButton);
        Button executeButton = findViewById(R.id.executeButton);
        Button selectKeyFileButton = findViewById(R.id.selectKeyFileButton);
        NestedScrollView nestedScrollView = findViewById(R.id.nestedScrollView);
        nestedScrollView.setFillViewport(true);

        //Wczytanie zapisanych danych
        String savedUsername = SharedPreferencesHelper.getUsername(this);
        if (!savedUsername.isEmpty()) {
            // Dane zostały już zapisane wcześniej, więc są wczytywane do pola edycyjnego
            usernameEditText.setText(savedUsername);
        }
        //Wybór pliku
        selectKeyFileButton.setOnClickListener(view -> {
            // Uruchom ekran wyboru pliku
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            //intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*"); // Dowolny typ pliku

            startActivityForResult(intent, REQUEST_PICK_FILE);
        });
        //Zapisanie danych
        saveButton.setOnClickListener(view -> {
            String username = usernameEditText.getText().toString();

            // Zapisz dane użytkownika
            SharedPreferencesHelper.saveUserInfo(MainActivity.this, username, privateKey);
        });
        //Złożenie i wykonanie komendy
        executeButton.setOnClickListener(view -> {
            String username = SharedPreferencesHelper.getUsername(this);
            String privateKey = SharedPreferencesHelper.getPrivateKey(this);

            //String command = "ssh trytonp 'squeue -o\"%j %t %S %M %e %V\" -u " + username + "'";
            String command = "ssh trytonp 'squeue -h -o\"%j %t %S %M %e %V\" -u " + username + "'";

            new ExecuteTask(this).execute(command, username, privateKey);
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_FILE && resultCode == Activity.RESULT_OK && data != null) {
            // Uzyskaj URI wybranego pliku
            Uri selectedFileUri = data.getData();
            // Uzyskaj pełną ścieżkę do pliku z URI
            String selectedFilePath = selectedFileUri.toString();

            Uri uri = Uri.parse(selectedFilePath);

            byte[] keyBytes = null;
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream != null) {
                    keyBytes = readBytes(inputStream);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            File file = new File(getFilesDir(), "id_rsa.pem");
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                if (keyBytes != null) {
                    fileOutputStream.write(keyBytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            privateKey = file.toURI().getPath();
        }
    }
    private byte[] readBytes(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }
    private static class ExecuteTask extends AsyncTask<String, Void, String> {
        private final MainActivity activity;
        public ExecuteTask(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        protected String doInBackground(String... params) {
            String command = params[0];
            String username = params[1];
            String privateKey = params[2];

            File privatekeyFile = new File(privateKey);
            SSHManager sshManager = new SSHManager("kdm.task.gda.pl", username, privatekeyFile);
            return sshManager.executeCommand(command);
        }

        protected void onPostExecute(String result) {
            // Aktualizuj widok tabeli po zakończeniu zadania
            activity.runOnUiThread(() -> activity.updateTable(result));
        }
    }
    private void updateTable(String result) {
        TableLayout tableLayout = findViewById(R.id.tableLayout);
        tableLayout.setBackgroundResource(R.drawable.table_border);

        // Wyczyść poprzednie dane w tabeli (zostaw nagłówki)
        clearTableData(tableLayout);

        // Dodaj nagłówki
        addTableHeader(tableLayout);

        // Podziel wynik na wiersze
        String[] rows = result.split("\n");

        for (String row : rows) {
            TableRow tableRow = new TableRow(this);

            // Podziel wiersz na kolumny
            String[] columns = row.split("\\s+");

            for (String column : columns) {
                TextView textView = new TextView(this);
                textView.setText(column);
                textView.setGravity(Gravity.CENTER);
                textView.setPadding(5, 5, 5, 5);
                textView.setBackgroundResource(R.drawable.row_cell_border);
                // Dodaj LayoutParams do TextView
                TableRow.LayoutParams params = new TableRow.LayoutParams(
                        TableRow.LayoutParams.WRAP_CONTENT,
                        TableRow.LayoutParams.MATCH_PARENT
                );
                textView.setLayoutParams(params);

                // Dodaj TextView do TableRow
                tableRow.addView(textView);
            }

            // Dodaj TableRow do TableLayout (pomijaj dodawanie nagłówków)
            tableLayout.addView(tableRow, new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private void clearTableData(TableLayout tableLayout) {
        int childCount = tableLayout.getChildCount();
        // Zacznij od 1, aby pozostawić nagłówki (pierwszy wiersz)
        for (int i = 1; i < childCount; i++) {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow) {
                tableLayout.removeViewAt(i);
                i--; // Zdekrementuj licznik, ponieważ usunięty został jeden wiersz
            }
        }
    }

    private void addTableHeader(TableLayout tableLayout) {
        // Tutaj dodaj wiersz nagłówkowy do TableLayout
        // W tym przykładzie zakładałem, że nagłówki są stałe, więc można je dodać ręcznie
        TableRow headerRow = new TableRow(this);

        // Dodaj wiersz nagłówkowy do TableLayout
        tableLayout.addView(headerRow, new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));
    }
}