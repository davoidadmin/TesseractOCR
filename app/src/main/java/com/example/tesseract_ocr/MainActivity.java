package com.example.tesseract_ocr;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private Bitmap mImageBitmap;
    private TessBaseAPI mTess;
    private TextToSpeech mTTS;

    private Button mCaptureButton;
    private Button mOCRButton;
    private Button mReadButton;
    private ImageView mImageView;
    private TesseractOCR tess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCaptureButton = findViewById(R.id.capture_button);
        mOCRButton = findViewById(R.id.ocr_button);
        mReadButton = findViewById(R.id.read_button);
        mImageView = findViewById(R.id.image_view);

        AssetManager assetManager = getAssets();
        tess = new TesseractOCR(assetManager);

        mTTS = new TextToSpeech(this, this);

        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
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

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            assert data != null;
            Bundle extras = data.getExtras();
            mImageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(mImageBitmap);
        }
    }

    private void doOCR() {
        String OCRresult = tess.getResults(mImageBitmap);;
        Log.d(TAG, "doOCR: " + OCRresult);
    }

    private void readText() {
        String OCRresult = tess.getUTF8Text();
        mTTS.speak(OCRresult, TextToSpeech.QUEUE_FLUSH, null);
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
    }
}
