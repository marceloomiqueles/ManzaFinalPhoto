package com.example.marcelomiqueles.manzafinalphoto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;

public class MainActivity extends Activity {
    static final String FTP_HOST= "192.185.185.42";
    static final String FTP_USER = "beelab";
    static final String FTP_PASS  ="Fld)=tAGZaS~";
    private static final int CAMERA_REQUEST = 1888;
    private ImageView imageView;
    private File f;
    private File l;
    private String filename;
    private String imageFileName;

    public File getAlbumDir()
    {
        File storageDir = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                ),
                "/BAC/"
        );
        // Create directories if needed
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return storageDir;
    }

    private File createImageFile() throws IOException {
        filename = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss").format(new Date());
        // Create an image file name
        imageFileName = getAlbumDir().toString() +"/" + filename + ".jpg";
        File image = new File(imageFileName);
        return image;
    }

    public Bitmap loadFrame() {
        FileInputStream in;
        BufferedInputStream buf;
        Bitmap bMap;
        try {
            in = new FileInputStream("/storage/emulated/0/Download/frame.png");
            buf = new BufferedInputStream(in);
            bMap = BitmapFactory.decodeStream(buf);
            return bMap;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        this.imageView = (ImageView)this.findViewById(R.id.imageView1);
        imageView.setImageBitmap(loadFrame());
        Button photoButton = (Button) this.findViewById(R.id.button1);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            try {
                f = createImageFile();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Matrix matrix = new Matrix();
            Uri uri = Uri.fromFile(f);
            matrix.postRotate(exifToDegrees(uri));
            Bitmap photo = BitmapFactory.decodeFile(f.getAbsolutePath());
            Bitmap reSizedBitMap = Bitmap.createScaledBitmap(photo, 1422, 800, true);
            Bitmap cropped = Bitmap.createBitmap(reSizedBitMap, 311, 0, 800, 800, matrix, true);
            Bitmap merged = combineImages(loadFrame(), cropped);
            storeImage(merged);
            File r = new File("/storage/emulated/0/Pictures/BAC/" + filename + ".jpg");
            // Upload sdcard file
            upLoadFile(r);
            imageView.setImageBitmap(merged);

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
            i.putExtra(Intent.EXTRA_BCC, new String[] {"sonyselfipascuero@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "Mi Selfi Con Santa");
            i.putExtra(Intent.EXTRA_TEXT   , "Disfruta tu selfie con el Santa de Sony en http://www.miselficonsanta.cl ");


            i.putExtra(Intent.EXTRA_STREAM, uri);
            try {
                startActivity(Intent.createChooser(i, "Enviar Correo"));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void storeImage(Bitmap image) {
        File pictureFile = new File(getAlbumDir().toString() +"/" + filename + ".jpg");
        if (pictureFile == null) {
            //Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            //Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            //Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    /*private void storeImage(Bitmap image, String name) {
        File pictureFile = new File(getAlbumDir().toString() +"/" + name + ".jpg");
        if (pictureFile == null) {
            //Log.d(TAG, "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            //Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            //Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }*/

    public void upLoadFile(File fileName){
        FTPClient client = new FTPClient();
        try {
            client.connect(FTP_HOST,21);
            client.login(FTP_USER, FTP_PASS);
            client.setType(FTPClient.TYPE_BINARY);
            client.changeDirectory("/public_html/Sony/");
            client.upload(fileName, new MyTransferListener());
        } catch (Exception e) {
            e.printStackTrace();
            try {
                client.disconnect(true);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    public class MyTransferListener implements FTPDataTransferListener {
        public void started() {
            // btn.setVisibility(View.GONE);
            // Transfer started
            // Toast.makeText(getBaseContext(), " Upload Started ...", Toast.LENGTH_SHORT).show();
            System.out.println(" Upload Started ...");
        }

        public void transferred(int length) {
            // Yet other length bytes has been transferred since the last time this
            // method was called
            // Toast.makeText(getBaseContext(), " transferred ..." + length, Toast.LENGTH_SHORT).show();
            System.out.println(" transferred ..." + length);
        }

        public void completed() {
            // btn.setVisibility(View.VISIBLE);
            // Transfer completed
            Toast.makeText(getBaseContext(), " completed ...", Toast.LENGTH_SHORT).show();
            //System.out.println(" completed ..." );
        }

        public void aborted() {
            //btn.setVisibility(View.VISIBLE);
            // Transfer aborted
            Toast.makeText(getBaseContext()," transfer aborted ,please try again...", Toast.LENGTH_SHORT).show();
            //System.out.println(" aborted ..." );
        }

        public void failed() {
            // btn.setVisibility(View.VISIBLE);
            // Transfer failed
            System.out.println(" failed ..." );
        }

    }

    public Bitmap combineImages(Bitmap frame, Bitmap image) {
        Bitmap cs = null;
        Bitmap rs = null;

        rs = Bitmap.createScaledBitmap(frame, image.getWidth() + 50,
                image.getHeight() + 50, true);

        cs = Bitmap.createBitmap(rs.getWidth(), rs.getHeight(),
                Bitmap.Config.RGB_565);

        Canvas comboImage = new Canvas(cs);

        comboImage.drawBitmap(image, 25, 25, null);
        comboImage.drawBitmap(rs, 0, 0, null);
        if (rs != null) {
            rs.recycle();
            rs = null;
        }
        Runtime.getRuntime().gc();
        return cs;
    }

    private static int exifToDegrees(Uri uri) {

        try {
            ExifInterface exif = new ExifInterface(uri.getPath());
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (rotation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
            else if (rotation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
            else if (rotation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
