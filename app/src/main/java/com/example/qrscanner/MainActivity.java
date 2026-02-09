package com.example.qrscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    // 权限请求码
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    // 扫码结果请求码
    private static final int SCAN_REQUEST_CODE = 101;

    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 绑定控件
        tvResult = findViewById(R.id.tv_result);
        Button btnScan = findViewById(R.id.btn_scan);

        // 点击Scan按钮触发逻辑
        btnScan.setOnClickListener(v -> {
            // 检查相机权限
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // 申请相机权限
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                // 权限已授予，跳转到扫码界面
                startActivityForResult(new Intent(MainActivity.this, ScanActivity.class), SCAN_REQUEST_CODE);
            }
        });
    }

    // 处理权限申请结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限通过，跳转到扫码界面
                startActivityForResult(new Intent(this, ScanActivity.class), SCAN_REQUEST_CODE);
            } else {
                // 权限拒绝，提示用户
                tvResult.setText(R.string.permission_tip);
            }
        }
    }

    // 接收扫码结果并显示
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("qr_result")) {
                String qrContent = data.getStringExtra("qr_result");
                tvResult.setText("扫码结果：" + qrContent);
            }
        }
    }
}