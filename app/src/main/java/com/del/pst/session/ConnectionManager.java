package com.del.pst.session;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.del.pst.R;
import com.del.pst.SettingsActivity;
import com.google.android.gms.common.util.Base64Utils;
import com.google.android.gms.common.util.Strings;
import com.google.android.gms.security.ProviderInstaller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;

import javax.crypto.Cipher;

public class ConnectionManager {

    private Context context;
    private String host;
    private String devName;

    public ConnectionManager(Context context) {
        this.context = context;
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.S_NAME, MODE_PRIVATE);
        this.host = String.format("%s://%s",
                context.getString(R.string.WS_PROTOCOL),
                context.getString(R.string.WS_HOST)
        );
        this.devName = settings.getString(SettingsActivity.S_APP_NAME, "noname");
    }

    public String getHost() {
        return host;
    }

    private String getEncodeCid(Session session) {
        try {
            return URLEncoder.encode(session.getCid(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return session.getCid();
        }
    }

    private String getEncodeDevName() {
        try {
            return URLEncoder.encode(devName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return devName;
        }
    }

    public void start(Session session,
                      String key,
                      Response.Listener<String> success,
                      Response.ErrorListener error) {
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                String.format("%s/notify/%s/%s",
                        host,
                        getEncodeCid(session),
                        getEncodeDevName()),
                r -> {
                    if (r != null && r.equalsIgnoreCase("OK")) {
                        session.activate(key);
                        if (success != null) success.onResponse(r);
                    } else {
                        session.inactivate();
                        if (error != null) error.onErrorResponse(new VolleyError(r));
                    }
                },
                e -> {
                    session.inactivate();
                    if (error != null) error.onErrorResponse(e);
                }
        );
        RequestQueue volleyQueue = Volley.newRequestQueue(context);
        volleyQueue.add(stringRequest);
    }

    public void ping(Session session) {
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                String.format("%s/notify/%s/%s",
                        host,
                        getEncodeCid(session),
                        getEncodeDevName()),
                r -> {
                    if (r == null || !r.equalsIgnoreCase("OK")) {
                        session.inactivate();
                    }
                },
                e -> {
                    session.inactivate();
                }
        );
        RequestQueue volleyQueue = Volley.newRequestQueue(context);
        volleyQueue.add(stringRequest);
    }

    public void send(Session session, String message, Response.Listener<String> success, Response.ErrorListener error) throws Exception {
        if (Strings.isEmptyOrWhitespace(message)) return;
        if (session.isActive()) {
            StringBuilder key = new StringBuilder();
            Scanner scanner = new Scanner(session.getKeyValue());
            while (scanner.hasNext()) {
                String s = scanner.nextLine();
                if (s != null && !s.startsWith("--")) key.append(s);
            }

            Cipher encryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            KeyFactory factory = KeyFactory.getInstance("RSA");
            byte[] keyBase64 = Base64Utils.decode(key.toString());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBase64);
            PublicKey publicKey = factory.generatePublic(spec);
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedMessageBytes = encryptCipher.doFinal(message.getBytes());
            String encryptedMessage = Base64Utils.encode(encryptedMessageBytes);

            StringRequest stringRequest = new StringRequest(
                    Request.Method.GET,
                    String.format("%s/send/%s/%s",
                            host,
                            getEncodeCid(session),
                            URLEncoder.encode(encryptedMessage, "UTF-8")),
                    success,
                    e -> {
                        session.processError();
                        if (error != null) error.onErrorResponse(e);
                    }
            );

            RequestQueue volleyQueue = Volley.newRequestQueue(context);
            volleyQueue.add(stringRequest);
        } else {
            if (error != null) error.onErrorResponse(new VolleyError("Session is not active"));
        }
    }

    public void disconnect(Session session,
                           Response.Listener<String> success,
                           Response.ErrorListener error) {
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET,
                String.format("%s/close/%s",
                        host,
                        getEncodeCid(session)
                ),
                r -> {
                    session.inactivate();
                    if (success != null) success.onResponse(r);
                },
                e -> {
                    session.inactivate();
                    if (error != null) error.onErrorResponse(e);
                }
        );
        RequestQueue volleyQueue = Volley.newRequestQueue(context);
        volleyQueue.add(stringRequest);
    }


}
