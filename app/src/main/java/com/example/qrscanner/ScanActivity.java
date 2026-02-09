package com.example.qrscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Size;

import com.google.common.util.concurrent.ListenableFuture;
//import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {
    private PreviewView previewView;
    // 相机线程池（避免主线程阻塞）
    private ExecutorService cameraExecutor;
    // ML Kit扫码器
    private BarcodeScanner barcodeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // 初始化控件和线程池
        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 配置ML Kit：只识别二维码（可扩展为识别条形码）
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // 启动相机预览和扫码
        startCamera();
    }

    // 初始化相机并绑定预览/分析器
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // 相机提供者初始化完成后执行
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. 配置相机预览
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 2. 配置图像分析（核心：实时识别二维码）
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720)) // 分辨率越高识别越准
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 只处理最新帧
                        .build();

                // 绑定图像分析器：每一帧都进行二维码识别
                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                // 3. 选择后置摄像头（扫码默认用后置）
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 绑定相机生命周期（和Activity绑定，自动管理）
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // 处理相机帧，识别二维码
    private void processImageProxy(@NonNull ImageProxy imageProxy) {
        // 将相机帧转换为ML Kit可识别的InputImage
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        // 扫描二维码
        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    // 识别成功且有结果
                    if (!barcodes.isEmpty()) {
                        Barcode barcode = barcodes.get(0);
                        String qrContent = barcode.getRawValue();

                        // 将结果返回给主界面
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("qr_result", qrContent);
                        setResult(RESULT_OK, resultIntent);

                        // 关闭扫码界面
                        finish();
                    }
                    // 必须关闭ImageProxy，否则相机会卡死
                    imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    // 识别失败（无需处理，继续扫描）
                    imageProxy.close();
                });
    }

    // 销毁时释放资源（避免内存泄漏）
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }
}