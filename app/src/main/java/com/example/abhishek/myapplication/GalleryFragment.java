package com.example.abhishek.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static android.app.Activity.RESULT_OK;


public class GalleryFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int CAMERA_REQUEST = 2;
    private static final int RESULT_LOAD_IMG=1;
    String destination;
    ImageView imageView;
    Button btnProcess;
    TextView txtDetect;
    public static TextView txtResult;
    Bitmap bitmap = null;
    String strURl = "https://translation.googleapis.com/language/translate/v2?key=AIzaSyB_DfRaCGNZ4NjnoyrlH_XcGX5DiHknY_Y&q=Hello World&target=zh";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public GalleryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        imageView = (ImageView) view.findViewById(R.id.image_view);
        btnProcess = (Button) view.findViewById(R.id.button_process);
        txtResult = (TextView) view.findViewById(R.id.textView_result);
        txtDetect = (TextView) view.findViewById(R.id.textView_detect);
        Button galleryButton = (Button) view.findViewById(R.id.button_gallery);

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
            }
        });
        final Spinner spinner = (Spinner) view.findViewById(R.id.spinner);
        ArrayAdapter adapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_dropdown_item);
        adapter.add("Select a Destination Language");
        adapter.add("en");
        adapter.add("ne");
        adapter.add("es");
        adapter.add("zh-CN");
        adapter.add("zh-TW");
        adapter.add("sq");
        adapter.add("da");
        adapter.add("de");
        adapter.add("pt");
        adapter.add("pa");
        adapter.add("ne");
        adapter.add("hmn");
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
                destination= spinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                destination = "en";
            }
        });
        return view;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == RESULT_LOAD_IMG && resultCode==RESULT_OK){
            Uri imageUri = data.getData();

            CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(getContext(),this);


        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                final Uri resultUri = result.getUri();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(),resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageURI(resultUri);
                btnProcess.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view){
                        TextRecognizer textRecognizer = new TextRecognizer.Builder(getContext().getApplicationContext()).build();
                        if (!textRecognizer.isOperational()) {
                            Log.e("ERROR", "Detector dependencies are not yet available");
                        }
                        else{
                            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                            SparseArray<TextBlock> items = textRecognizer.detect(frame);
                            StringBuilder stringBuilder = new StringBuilder();
                            for (int i = 0; i < items.size(); ++i) {
                                TextBlock item = items.valueAt(i);
                                stringBuilder.append(item.getValue());
                                stringBuilder.append("\n");
                                String result = new String();
                                result = stringBuilder.toString();
                                txtDetect.setMovementMethod(new ScrollingMovementMethod());
                                txtDetect.setText("Detected: "+result);
                                String sourceText = result;
                                strURl = "https://translation.googleapis.com/language/translate/v2?key=AIzaSyB_DfRaCGNZ4NjnoyrlH_XcGX5DiHknY_Y&q="+sourceText+"&target="+destination;
                                new Task().execute();
                                Toast.makeText(getContext(), "Image Processed",Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }


    public class Task extends AsyncTask<String, String, String> {

        String data;
        String dataParsed;

        @Override
        protected String doInBackground(String... strings) {
            try {
                URL url = new URL(strURl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();

                BufferedReader bf = new BufferedReader(new InputStreamReader(con.getInputStream()));

                StringBuilder sb = new StringBuilder();
                StringBuffer output = new StringBuffer();
                String line ="";

                while ((line = bf.readLine())!=null) {
                    output.append(line);
                }

                data = output.toString();
                JSONObject result = new JSONObject(data).getJSONObject("data");
                dataParsed = result.getJSONArray("translations").getJSONObject(0).getString("translatedText");


            }
            catch (MalformedURLException e){
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            txtResult.setMovementMethod(new ScrollingMovementMethod());
            GalleryFragment.txtResult.setText("Your result: "+ dataParsed);

        }
    }
    public  Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getActivity().getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            Toast.makeText(context, "Gallery Action", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
