import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.tesseract_ocr.TesseractOCR;
import com.example.tesseract_ocr.R;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.widget.ImageView;
import androidx.camera.view.PreviewView;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;

    private Bitmap mImageBitmap;
    private TessBaseAPI mTess;
    private TextToSpeech mTTS;
    private ExecutorService cameraExecutor;

    private Button mCaptureButton;
    private Button mOCRButton;
    private Button mReadButton;
    private PreviewView mPreviewView;
    private TesseractOCR tess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCaptureButton = findViewById(R.id.capture_button);
        mOCRButton = findViewById(R.id.ocr_button);
        mReadButton = findViewById(R.id.read_button);
        mPreviewView = findViewById(R.id.preview_view);

        AssetManager assetManager = getAssets();
        tess = new TesseractOCR(assetManager);

        mTTS = new TextToSpeech(this, this);

        cameraExecutor = Executors.newSingleThreadExecutor();

        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                }
            }
        });

        mOCRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doOCR();
            }
        });

        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readText();
            }
        });
    }

    private void openCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Unable to open camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Unable to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void doOCR() {
        if (mImageBitmap != null) {
            String OCRresult = tess.getResults(mImageBitmap);
            Log.d(TAG, "doOCR: " + OCRresult);
        } else {
            Toast.makeText(MainActivity.this, "Capture an image first", Toast.LENGTH_SHORT).show();
        }
    }

    private void readText() {
        if (mImageBitmap != null) {
            String OCRresult = tess.getUTF8Text();
            mTTS.speak(OCRresult, TextToSpeech.QUEUE_FLUSH, null);
        } else {
            Toast.makeText(MainActivity.this, "Capture an image first", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = mTTS.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "onInit: This Language is not supported");
            }
        } else {
            Log.e(TAG, "onInit: Initialization Failed!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tess.onDestroy();
        mTTS.shutdown();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

