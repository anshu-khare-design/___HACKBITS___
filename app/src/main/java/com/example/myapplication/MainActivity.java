package com.example.myapplication;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import com.example.myapplication.GraphicUtils.GraphicOverlay;
import com.example.myapplication.GraphicUtils.TextGraphic;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.ImageView;
import org.tensorflow.lite.Interpreter;
import android.os.SystemClock;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.camView)
    CameraView mCameraView;
    @BindView(R.id.cameraBtn)
    Button mCameraButton;
    @BindView(R.id.send)
    Button sendButton;
    @BindView(R.id.graphic_overlay)
    GraphicOverlay mGraphicOverlay;
    @BindView(R.id.classify)
     Button classify;
    @BindView(R.id.textView2)
    TextView textView;
    String t1 = "";
    String str = "";

    int inputSize = 227;
    int PIXEL_SIZE =3;
    public static String postUrl = "http://192.168.43.219/php_lessons/testy.php";
    public static String postUrl1 = "http://192.168.43.219/pan/testy1.php";
    public Interpreter tflite ;
    public ByteBuffer byteBuffer;
    public float result[][] = {{0,0}};
    Bitmap bitmap;
    int BATCH_SIZE = 1;

    float IMAGE_MEAN = 0f;
    float IMAGE_STD = 225f ;
    int a = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new
                    StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        ButterKnife.bind(this);
        mCameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                 bitmap = cameraKitImage.getBitmap();

                bitmap = Bitmap.createScaledBitmap(bitmap, mCameraView.getWidth(), mCameraView.getHeight(), false);
                mCameraView.stop();
                t1 = new String("");
               runTextRecognition(bitmap);
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGraphicOverlay.clear();
                mCameraView.start();
                mCameraView.captureImage();


            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTaskPayment().execute();
                mGraphicOverlay.clear();
                textView.setText("");

            }
        });

        classify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                bitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false);
                byteBuffer = convertBitmapToByteBuffer(bitmap);
                Log.d("this" , "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println(byteBuffer.remaining());
                setTflite();
                long startTime = SystemClock.uptimeMillis();
                tflite.run(byteBuffer , result);
                long endTime = SystemClock.uptimeMillis();
                Log.d("result" , String.valueOf( result[0][0]));
                Log.d("result" , String.valueOf( result[0][1]));

            }
        });

    }

    public void setValueofa() {

        str = t1;
        if( str.contains("INCOME TAX") || str.contains("INCOMETAX"))
        {
            textView.setText("Card submitted is PAN CARD");
            a=2;
        }


    else if( str.contains("GOVERNMENT OF INDIA")  ||  str.contains("Government of India")){
        textView.setText("Card submitted is AADHAR card");
         a =1;
        }
    else{
            textView.setText("Alert!Card submitted is not valid.");
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        mCameraView.start();
    }

    @Override
    public void onPause() {
        mCameraView.stop();
        super.onPause();
    }


    private void runTextRecognition(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextDetector detector = FirebaseVision.getInstance()
                .getVisionTextDetector();
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                processTextRecognitionResult(texts);

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) {
        List<FirebaseVisionText.Block> blocks = texts.getBlocks();
        if (blocks.size() == 0) {
            Log.d("TAG", "No text found");
            return;
        }
        mGraphicOverlay.clear();
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {
                    GraphicOverlay.Graphic textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
                    mGraphicOverlay.add(textGraphic);
                    Log.d("text_recocinzed", (elements.get(k)).getText());
                    t1 = t1 + " " + elements.get(k).getText();


                }
            }
        }

        setValueofa();

    }

    public class AsyncTaskPayment extends AsyncTask<String, Void, Void> {

        private final HttpClient Client = new DefaultHttpClient();
        private String Content;


        private String Error = null;
        private final String TAG = null;

        private ProgressDialog Dialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            Dialog.setMessage("Wait..");
            Dialog.show();
            Log.d(TAG, "------------------------------------- Here");
        }

        protected Void doInBackground(String... urls) {

            if ( a ==1){

                try {
                    HttpPost httppost = new HttpPost(postUrl);


                    JSONObject jObject = new JSONObject();
                    jObject.put("text_detected", t1);

                    MultipartEntity se = new MultipartEntity(
                            HttpMultipartMode.BROWSER_COMPATIBLE);
                    se.addPart("request", new StringBody(jObject.toString()));
                    httppost.setEntity(se);


                    HttpResponse resp = Client.execute(httppost);

                    Content = EntityUtils.toString(resp.getEntity());


                    Log.e("Response", "******" + Content);


                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            else if (a==2){

                try {
                    HttpPost httppost = new HttpPost(postUrl1);


                    JSONObject jObject = new JSONObject();
                    jObject.put("text_detected", t1);

                    MultipartEntity se = new MultipartEntity(
                            HttpMultipartMode.BROWSER_COMPATIBLE);
                    se.addPart("request", new StringBody(jObject.toString()));
                    httppost.setEntity(se);


                    HttpResponse resp = Client.execute(httppost);

                    Content = EntityUtils.toString(resp.getEntity());


                    Log.e("Response", "******" + Content);


                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }

            else {

                return null;
            }

            }


        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            Dialog.dismiss();
            Log.e("ERROR", "--------------------------------" + Content);
            Log.d("exit", "*******exit*********");
        }

    }

    public static MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void setTflite() {
        try {
            tflite = new Interpreter(loadModelFile(MainActivity.this ));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        byteBuffer = ByteBuffer.allocateDirect(
                4 * BATCH_SIZE * inputSize* inputSize * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        long startTime = SystemClock.uptimeMillis();
        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
               byteBuffer.putFloat(
                        ((((val >> 16) & 0xFF)- IMAGE_MEAN)/IMAGE_STD));
                byteBuffer.putFloat(
                         ((((val >> 8) & 0xFF)- IMAGE_MEAN)/IMAGE_STD));
                byteBuffer.putFloat(
                        ((((val) & 0xFF)- IMAGE_MEAN)/IMAGE_STD));
            }
        }
        long endTime = SystemClock.uptimeMillis();
        byteBuffer.rewind();
        return byteBuffer;
    }

}


