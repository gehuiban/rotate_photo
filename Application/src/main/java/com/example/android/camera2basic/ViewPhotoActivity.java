package com.example.android.camera2basic;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ViewPhotoActivity extends Activity {

    private ImageView photoView;
    private Button cancelButton;
    private Button saveButton;
    private ImageButton rotateButton;
    public static final String PHOTO_TAKEN = "PHOTO_TAKEN";
    public static final String PREFS_FILE = "prefs_file";
    public static final String PHOTO_ORIENTATION = "PHOTO_ORIENTATION";
    public static final int WRITE_PERMISSION = 0x01;

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);

    private Bitmap rotatedImg;

    String filePath;
    int width, height, deviceOrientation;
    float angle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filePath = getIntent().getStringExtra(PHOTO_TAKEN);
        setContentView(R.layout.activity_view_photo);
        photoView = (ImageView) findViewById(R.id.image_view);
        cancelButton = (Button) findViewById(R.id.cancel);
        saveButton = (Button) findViewById(R.id.save);
        rotateButton = (ImageButton) findViewById(R.id.rotate_image);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        this.height = metrics.heightPixels;
        this.width = metrics.widthPixels;

        this.deviceOrientation = getResources().getConfiguration().orientation;
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(ViewPhotoActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestWritePermission();
                    return;
                }
                saveToFile(ViewPhotoActivity.this, rotatedImg);
                onBackPressed();
            }
        });
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                angle += 90;
                rotatedImg = decodeSampledBitmapFromFile(filePath, width, height, angle);
                photoView.setImageBitmap(rotatedImg);
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(PHOTO_ORIENTATION, angle);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        angle = prefs.getFloat(PHOTO_ORIENTATION, 0f);
        photoView.setImageBitmap(decodeSampledBitmapFromFile(filePath, width, height, angle));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }

    public static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight, float rotation) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        final int height = options.outHeight;
        final int width = options.outWidth;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        int inSampleSize = 1;

        if (height > reqHeight) {
            inSampleSize = Math.round((float) height / (float) reqHeight);
        }
        int expectedWidth = width / inSampleSize;
        if (expectedWidth > reqWidth) {
            inSampleSize = Math.round((float) width / (float) reqWidth);
        }
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;

        Bitmap bm = BitmapFactory.decodeFile(path, options);

        Matrix matrix = new Matrix();
        matrix.setRotate(rotation, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        return rotatedBitmap;
    }

    public static void saveToFile(Activity activity, Bitmap rotatedImg){
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/CameraTest");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        String dateTime = simpleDateFormat.format(GregorianCalendar.getInstance().getTime());
        String fileName = "image_" + dateTime + ".jpg";
        File file = new File(myDir, fileName);

        Log.i("SAVE_IMG", "" + file);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            rotatedImg.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Camera2BasicFragment.showToast(activity, "File saved:" + file.getAbsolutePath());
    }

    private void requestWritePermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveToFile(this, rotatedImg);
                    onBackPressed();
                } else {
                    Camera2BasicFragment.showToast(this, "Permission required to be able to store image.");
                }
                return;
            }
        }
    }
}
