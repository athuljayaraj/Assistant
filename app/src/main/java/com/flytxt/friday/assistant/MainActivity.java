package com.flytxt.friday.assistant;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;

import java.util.List;
import java.util.Locale;

import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private String TAG = "speech";
    WebView myWebView;

    @Override
    public void onInit(int status) {
        Log.d(TAG,"Init complete");

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 123 && grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Speech.init(this);
            doRecording();
        } else {
            Toast.makeText(this, "Permission Required inorder to record audio", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myWebView = findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.loadUrl("http://www.example.com");


        Log.i("SpeechInitialized", "Initialised");

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 123);
            return;
        }
        Speech.init(this);
        doRecording();
    }

    private void doRecording() {
        try {
            // you must have android.permission.RECORD_AUDIO granted at this point
            Speech.getInstance().startListening(new SpeechDelegate() {
                @Override
                public void onStartOfSpeech() {
                    Log.i(TAG, "speech recognition is now active");
                }

                @Override
                public void onSpeechRmsChanged(float value) {
                    Log.d(TAG, "rms is now: " + value);

                }

                @Override
                public void onSpeechPartialResults(List<String> results) {

                    StringBuilder str = new StringBuilder();
                    for (String res : results) {
                        str.append(res).append(" ");
                    }
                    Log.i(TAG, "partial result: " + str.toString().trim());
                }

                @Override
                public void onSpeechResult(String result) {
                    Log.i(TAG, "result: " + result);
                    parse(result);
                }
            });
        } catch (GoogleVoiceTypingDisabledException | SpeechRecognitionNotAvailable e) {
            e.printStackTrace();
        }
    }

    private void parse(String text) {

        final TextToSpeech tts = new TextToSpeech(MainActivity.this, MainActivity.this);
        tts.setLanguage(Locale.US);

        AIConfiguration config = new AIConfiguration("95693ca526604801ba01875feee22c6c", ai.api.AIConfiguration.SupportedLanguages.English, AIConfiguration.RecognitionEngine.System);

        final AIDataService aiDataService = new AIDataService(MainActivity.this, config);
        final AIRequest aiRequest = new AIRequest();
        aiRequest.setQuery(text);
        new AsyncTask<AIRequest, Void, AIResponse>() {
            @Override
            protected AIResponse doInBackground(AIRequest... requests) {
                final AIRequest request = requests[0];
                try {
                    final AIResponse response = aiDataService.request(aiRequest);
                    return response;
                } catch (AIServiceException e) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(AIResponse aiResponse) {
                if (aiResponse != null) {
                    final Result result = aiResponse.getResult();
                    Log.i("Response", result.getParameters().toString());

                    tts.speak((result.getFulfillment().getSpeech()), TextToSpeech.QUEUE_FLUSH, null);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            myWebView.loadUrl("https://arunsoman.github.io/polymer-d3/");
                            Toast.makeText(MainActivity.this, " Context: " + result.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }.execute(aiRequest);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("System", "Destroy");
        Speech.getInstance().stopTextToSpeech();
    }
}