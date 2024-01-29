package com.example.kdmcalculationcontroller;

import android.util.Log;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class SSHManager {

    private final String host;
    private final String username;
    private final File privateKey;

    public SSHManager(String host, String username, File privateKey) {
        this.host = host;
        this.username = username;
        this.privateKey = privateKey;
    }

    public String executeCommand(String command) {
        Connection connection = new Connection(host);
        try {
            // Nawiązanie połączenia
            connection.connect();

            //Log.d("DEBUG", "privateKey: " + privateKey);


            // Uwierzytelnianie kluczem prywatnym
            boolean isAuthenticated = connection.authenticateWithPublicKey(username, privateKey, null);

            //Uwierzytelnianie hasłem
            //boolean isAuthenticated = connection.authenticateWithPassword(username, password);

            // Utworzenie sesji SSH
            Session session = connection.openSession();

            // Wykonanie zdalnego polecenia (np. listowanie plików)
            session.execCommand(command);

            // Odczytanie wyniku
            return printInputStream(session.getStdout());

        } catch (IOException e) {
            e.printStackTrace();
            return "IOException occurred: " + e.getMessage();
        } finally {
            // Zamknięcie połączenia
            connection.close();
        }
    }
    private String printInputStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line).append("\n");
        }
        reader.close();
        return result.toString();
    }
}