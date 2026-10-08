package com.licensegate.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static final String PREFS = "license_gate_v1";
    private static final String KEY_STATUS = "status";
    private static final String KEY_EXPIRES = "expiresAt";
    private static final String TRIAL_CODE = "1234";
    private static final String PERMANENT_CODE = "2234";
    private static final long TRIAL_MS = 30L * 24 * 60 * 60 * 1000;

    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = findViewById(R.id.statusText);
        if (needsGate()) {
            showGate();
        } else {
            render();
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private boolean needsGate() {
        String status = prefs().getString(KEY_STATUS, "");
        if ("permanent".equals(status)) return false;
        if ("trial".equals(status)) {
            return System.currentTimeMillis() >= prefs().getLong(KEY_EXPIRES, 0);
        }
        return true;
    }

    private boolean trialExpired() {
        return "trial".equals(prefs().getString(KEY_STATUS, ""))
                && System.currentTimeMillis() >= prefs().getLong(KEY_EXPIRES, 0);
    }

    private void showGate() {
        boolean renew = trialExpired();
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(renew ? "请输入 2234" : "请输入 1234");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("输入验证码开始使用")
                .setMessage(renew
                        ? "试用已到期。输入 2234 后永久解锁，之后不再弹窗。"
                        : "输入 1234 可使用 30 天。到期后需要 2234 永久解锁。")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("确认", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String code = input.getText().toString().trim();
            if (!renew && TRIAL_CODE.equals(code)) {
                prefs().edit()
                        .putString(KEY_STATUS, "trial")
                        .putLong(KEY_EXPIRES, System.currentTimeMillis() + TRIAL_MS)
                        .apply();
                dialog.dismiss();
                render();
                return;
            }
            if (renew && PERMANENT_CODE.equals(code)) {
                prefs().edit().putString(KEY_STATUS, "permanent").remove(KEY_EXPIRES).apply();
                dialog.dismiss();
                render();
                return;
            }
            input.setError("验证码不正确");
        }));
        dialog.show();
    }

    private void render() {
        String status = prefs().getString(KEY_STATUS, "");
        if ("permanent".equals(status)) {
            statusText.setText("已永久解锁，后续无需再输入验证码。");
            return;
        }
        long expires = prefs().getLong(KEY_EXPIRES, 0);
        String when = DateFormat.getDateTimeInstance().format(new Date(expires));
        statusText.setText("试用中，到期时间：" + when + "\n到期后打开会再次要求输入 2234。");
    }
}
