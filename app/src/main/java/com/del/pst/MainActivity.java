package com.del.pst;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.del.pst.dao.AppDatabase;
import com.del.pst.dao.DBItem;
import com.del.pst.databinding.ActivityMainBinding;
import com.del.pst.session.ConnectionManager;
import com.del.pst.session.Session;
import com.del.pst.utils.Callback;
import com.del.pst.utils.ExportImportTask;
import com.del.pst.utils.PasswordGenerator;
import com.del.pst.utils.Utils;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.util.Strings;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private static final int RC_READ_FILE = 10;
    private static final int RC_HANDLE_PERM_WRITE = 2;
    private static final int RC_HANDLE_PERM_READ = 3;
    private static final String TAG = "PST Main";

    private ActivityResultLauncher<String> connectionLauncher;
    private ActivityResultLauncher<String> codeLauncher;
    private ConnectionManager connectionManager;
    private Session session;

    @Override
    protected void onResume() {
        if (session != null) {
            SharedPreferences settings = getSharedPreferences(SettingsActivity.S_NAME, MODE_PRIVATE);
            session.checkTimeout(settings.getLong(SettingsActivity.S_SESSION_TIMEOUT, 10));
            if (session.isActive()) connectionManager.ping(session);
        }
        super.onResume();
    }

    private void updateAndroidSecurityProvider() {
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        try {
            if (!Utils.init()) {
                binding.msgNoSecure.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e("INIT", e.getMessage(), e);
            finish();
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        connectionManager = new ConnectionManager(this);
//        TextView connectionInfo = findViewById(R.id.connectionInfo);
//        connectionInfo.setText(connectionManager.getHost());

        session = new ViewModelProvider(this).get(Session.class);
        Menu menu = binding.toolbar.getMenu();
        session.getKey().observe(this, (key) -> {
            if (menu != null && menu.size() > 0) {
                MenuItem menuItem = menu.findItem(R.id.action_connect);
                if (Strings.isEmptyOrWhitespace(key)) {
                    menuItem.setIcon(getResources().getDrawable(R.drawable.ic_connection_disabled));
                } else {
                    menuItem.setIcon(getResources().getDrawable(R.drawable.ic_connection));
                }
            }
        });
        session.getDigestOfPassword().observe(this, (code) -> {
            if (menu != null && menu.size() > 0) {
                menu.findItem(R.id.action_import).setEnabled(code != null && code.length > 0);
                menu.findItem(R.id.action_export).setEnabled(code != null && code.length > 0);
                menu.findItem(R.id.action_clear_all).setEnabled(code != null && code.length > 0);
            }
        });

        connectionLauncher = registerForActivityResult(new ActivityResultContract<String, String>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, String input) {
                Intent intent = new Intent(context, ConnectActivity.class);
                intent.putExtra("HOST", connectionManager.getHost());
                return intent;
            }

            @Override
            public String parseResult(int resultCode, @Nullable Intent i) {
                if (resultCode == CommonStatusCodes.SUCCESS) {
                    if (i != null) {
                        return i.getStringExtra(ConnectActivity.BarcodeObject);
                    }
                }
                return null;
            }
        }, result -> {
            try {
                if (Strings.isEmptyOrWhitespace(result)) return;
                if (session.isActive()) {
                    connectionManager.disconnect(session, null, null);
                }
                JSONObject json = new JSONObject(result);
                session.start(json.getString("cid"));
                updateAndroidSecurityProvider();
                connectionManager.start(
                        session,
                        json.getString("pub"),
                        r -> Snackbar.make(getWindow().getDecorView().getRootView(), "Полключение установлено", 5000).show(),
                        e -> {
                            Log.e(TAG, "Connection error", e);
                            Snackbar.make(getWindow().getDecorView().getRootView(), "Подключение не удалось", 5000).show();
                        }
                );
            } catch (Exception e) {
                Log.e(TAG, "Send message error", e);
                Snackbar.make(getWindow().getDecorView().getRootView(),
                        "Подключение не удалось",
                        5000).show();
            }
        });
        codeLauncher = registerForActivityResult(new ActivityResultContract<String, String>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, String input) {
                return new Intent(context, CodeActivity.class);
            }

            @Override
            public String parseResult(int resultCode, @Nullable Intent i) {
                if (resultCode == CommonStatusCodes.SUCCESS) {
                    if (i != null) {
                        return i.getStringExtra(CodeActivity.CodeObject);
                    }
                }
                return null;
            }
        }, code -> {
            if (!Strings.isEmptyOrWhitespace(code)) {
                try {
                    byte[] digestOfPassword = Utils.getDigestOfPassword(code, false);
                    session.getDigestOfPassword().setValue(digestOfPassword);

                } catch (Exception e) {
                    Log.e("MAIN", e.getMessage(), e);
                }
            }
        });
        SharedPreferences settings = getSharedPreferences(SettingsActivity.S_NAME, MODE_PRIVATE);
        if (!settings.contains(SettingsActivity.S_APP_NAME)) {
            settings.edit().putString(SettingsActivity.S_APP_NAME, SettingsActivity.getDeviceName()).apply();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_header)
                    .setMessage(R.string.exit_question)
                    .setPositiveButton(R.string.YES, (dialogInterface, i) -> finishAffinity())
                    .setNegativeButton(R.string.NO, (dialogInterface, i) -> {
                    }).show();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }
        if (id == R.id.action_connect) {
            if (session.isActive()) {
                connectionManager.disconnect(session, null, null);
            } else {
                connectionLauncher.launch(null);
            }
            return true;
        }
        if (id == R.id.action_info) {
            Intent viewIntent = new Intent("android.intent.action.VIEW",
                    Uri.parse(connectionManager.getHost() + "/help"));
            startActivity(viewIntent);
            return true;
        }
        if (id == R.id.action_code) {
            codeLauncher.launch(null);
            return true;
        }
        if (id == R.id.action_clear_buffer) {
            clearBuffer();
            return true;
        }
        if (id == R.id.action_import) {
            int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (rc != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission(Manifest.permission.READ_EXTERNAL_STORAGE, RC_HANDLE_PERM_READ);
            } else {
                importDB();
            }
            return true;
        }
        if (id == R.id.action_export) {
            int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (rc != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, RC_HANDLE_PERM_WRITE);
            } else {
                exportDB();
            }
            return true;
        }
        if (id == R.id.action_clear_all) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_header)
                    .setMessage(R.string.question_clear_all)
                    .setPositiveButton(R.string.YES, (dialogInterface, i) -> clearAll())
                    .setNegativeButton(R.string.NO, (dialogInterface, i) -> {
                    }).show();
            return true;
        }
        if (id == R.id.action_test) {
            try {
                PasswordGenerator pg = new PasswordGenerator();
                pg.setUseLower(true);
                pg.setUseUpper(true);
                pg.setUseDigits(true);
                pg.setUsePunctuation(false);
                pg.setLength(10);
                Utils.printMac("MUZSzXth3V");
            } catch (Exception e) {
                Log.e("MAIN", e.getMessage(), e);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearAll() {
        Handler h = new Handler();
        h.post(() -> {
            AppDatabase db = AppContext.getInstance().getDatabase();
            byte[] digestOfPassword = session.getDigestOfPasswordValue();
            int count = 0;
            for (DBItem item : db.itemDao().getAll()) {
                if (Utils.valid(item, digestOfPassword)) {
                    db.itemValuesDao().delete(item);
                    count++;
                }
            }
            updateList();
            Snackbar.make(getWindow().getDecorView().getRootView(),
                    "Удалено: " + count,
                    5000).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_PERM_WRITE && requestCode != RC_HANDLE_PERM_READ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case RC_HANDLE_PERM_WRITE:
                    exportDB();
                    break;
                case RC_HANDLE_PERM_READ:
                    importDB();
                    break;
            }
        } else {
            DialogInterface.OnClickListener listener = (dialog, id) -> finish();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_header)
                    .setMessage(R.string.no_any_permission)
                    .setPositiveButton(R.string.ok, listener);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        clearBuffer();
        if (session.isActive()) {
            connectionManager.disconnect(session, null, null);
        }
        session.getDigestOfPassword().setValue(null);
        super.onDestroy();
    }

    private void clearBuffer() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip();
        } else {
            ClipData clip = ClipData.newPlainText("QRTransfer", "");
            clipboard.setPrimaryClip(clip);
        }
        Snackbar.make(getWindow().getDecorView().getRootView(),
                "Буфер очищен",
                5000).show();
    }

    private void requestStoragePermission(String externalStorage, int requestCode) {
        final String[] permissions = new String[]{externalStorage};
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                externalStorage)) {
            ActivityCompat.requestPermissions(this, permissions, requestCode);
            return;
        }
        final Activity thisActivity = this;
        View.OnClickListener listener = view ->
                ActivityCompat.requestPermissions(thisActivity, permissions, requestCode);

        findViewById(R.id.mainTopLayout).setOnClickListener(listener);
        Snackbar.make(getWindow().getDecorView().getRootView(),
                        R.string.permission_storage_rationale,
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    private void importDB() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).
                setType("*/*").
                addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Укажите импортируемый файл"), RC_READ_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_READ_FILE && resultCode == RESULT_OK && data != null) {
            passwordRequest(password -> {
                byte[] digestOfPassword = session.getDigestOfPassword().getValue();
                if (!Strings.isEmptyOrWhitespace(password)) {
                    try {
                        byte[] bytes = Utils.readFile(this, data.getData());
                        byte[] decode = Utils.decodeData(bytes, password, true);
                        String json = new String(decode, StandardCharsets.UTF_8);
                        new ExportImportTask(digestOfPassword, ExportImportTask.Mode.IMPORT, s -> {
                            if (s != null) {
                                updateList();
                                Snackbar.make(binding.getRoot(), s, 5000).show();
                            } else {
                                Snackbar.make(binding.getRoot(), "Импорт был прерван", 5000).show();
                            }
                        }).execute(password, json);
                    } catch (Exception e) {
                        Utils.showError(binding.getRoot(), e.getMessage(), e);
                    }
                } else {
                    Utils.showError(getWindow().getDecorView().getRootView(), "Вы не указали пароль!", null);
                }
            });
        }
    }

    private void updateList() {
        byte[] digestOfPassword = session.getDigestOfPasswordValue();
        session.getDigestOfPassword().setValue(digestOfPassword);
    }

    private void exportDB() {
        passwordRequest(password -> {
            byte[] digestOfPassword = session.getDigestOfPassword().getValue();
            if (!Strings.isEmptyOrWhitespace(password)) {
                new ExportImportTask(digestOfPassword, ExportImportTask.Mode.EXPORT, s -> {
                    if (s != null) {
                        Snackbar.make(binding.getRoot(), s, 5000).show();
                    } else {
                        Snackbar.make(binding.getRoot(), "Экспорт был прерван", 5000).show();
                    }
                }).execute(password);
            } else {
                Utils.showError(getWindow().getDecorView().getRootView(), "Вы не указали пароль!", null);
            }
        });
    }

    private void passwordRequest(Callback<String> c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Пароль");
        final EditText input = new EditText(this);
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton(R.string.OK, (dialog, which) -> {
            String password = input.getText().toString();
            c.call(password);
        });
        builder.setNegativeButton(R.string.btn_exit, (dialog, which) -> dialog.cancel());
        builder.show();
    }

}