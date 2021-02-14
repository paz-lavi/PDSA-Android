package com.paz.pdsa.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.paz.logger.EZLog;
import com.paz.pdsa.FileMimeException;
import com.paz.pdsa.R;
import com.paz.pdsa.databinding.ActivityMainBinding;
import com.paz.pdsa.dsa.ras.KeyPair;
import com.paz.pdsa.dsa.ras.RSA;
import com.paz.pdsa.dsa.sign.DSA;
import com.paz.pdsa.dsa.sign.SignatureData;
import com.paz.pdsa.dsa.sign.ValidateResult;
import com.paz.pdsa.utils.Constants;
import com.paz.pdsa.utils.Files;
import com.paz.pdsa.utils.User;
import com.paz.prefy_lib.Prefy;
import com.paz.taskrunnerlib.task_runner.BaseTask;
import com.paz.taskrunnerlib.task_runner.RunnerCallback;
import com.paz.taskrunnerlib.task_runner.TaskRunner;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import lombok.SneakyThrows;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    // Request code for selecting a PDF document.
    private static final int SIGN_FILE = 2265;
    private static final int SAVE_FILE = 2918;
    private Uri uri, resUri;
    private KeyPair keyPair;
    private RSA rsa;
    private String mimeType;
    private final String TAG = "pttt";
    private final String ERROR = "Failed to signed file";
    private static final String ACCESS_ERROR = "The app has no access to the file";
    private final EZLog ezLog = EZLog.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        uri = getIntent().getData();
        rsa = new RSA();
        setOnClick();
        validateKeys();
        shouldShowAndroid11Note();
        bindLabelsText();
       // FirebaseFirestore.getInstance().setFirestoreSettings(new .Builder().setHost().build());
    }

    private void bindLabelsText() {
        binding.mainLBLUri.setText(getString(R.string.filePath, uri == null ? "Not Selected" : uri.getPath()));
        binding.mainLBLResult.setVisibility(View.GONE);
    }

    private void validateKeys() {
        Prefy prefy = Prefy.getInstance();
        long e = prefy.getLong(Constants.PRIVATE_KEY, -1);
        long d = prefy.getLong(Constants.PUBLIC_KEY, -1);
        long n = prefy.getLong(Constants.KEY_LENGTH, -1);
        if (e != -1 && d != -1 && n != -1) {
            keyPair = new KeyPair(e, d, n);
        } else {
            keyPair = rsa.keysGenerator(64);
            prefy.putLong(Constants.KEY_LENGTH, keyPair.getKeyLength());
            prefy.putLong(Constants.PUBLIC_KEY, keyPair.getPublicKey());
            prefy.putLong(Constants.PRIVATE_KEY, keyPair.getPrivateKey());
        }

    }

    private void setOnClick() {
        binding.mainBTNSelectFile.setOnClickListener((v) -> openFile());
        binding.mainBTNSign.setOnClickListener((v) -> signFile(uri));
        binding.mainBTNValidate.setOnClickListener((v) -> validateFile(uri));
        binding.mainBTNAccount.setOnClickListener((v) -> startActivity(new Intent(MainActivity.this, UserActivity.class)));
        binding.mainBTNClose.setOnClickListener(v -> hideSignedDialog());
        binding.mainBTNSave.setOnClickListener(v -> saveSignedFile());
        binding.mainBTNShareFile.setOnClickListener(v -> sentToMail(resUri.getPath()));
    }


    private void validateFile(Uri uri) {
        if (uri == null) {
            shoWErrorDialog();
            return;
        }
        TaskRunner<ValidateResult> ts = new TaskRunner<>();
        //  binding.mainBARProgressIndicator.setVisibility(View.VISIBLE);
        ts.executeAsync(new RunnerCallback<ValidateResult>() {
            @Override
            public void onPreExecute() {
                runOnUiThread(() -> {
                    binding.mainLBLResult.setVisibility(View.GONE);
                    showProgressDialog("Validating File");

                });
            }

            @Override
            public ValidateResult call() throws Exception {

                // Perform operations on the document using its URI.
                try {

                    return new DSA(MainActivity.this).validateSignedFile(uri);
                } catch (ArrayIndexOutOfBoundsException e) {
                    ezLog.logException(e.getMessage(), e);

                    return new ValidateResult(false, null);
                } catch (SecurityException | FileMimeException e) {
                    ezLog.logException(e.getMessage(), e);
                    showAccessErrorDialog();
                    return new ValidateResult(false, null);
                }
            }

            @Override
            public void onPostExecute(ValidateResult result) {
                hideProgressDialog();
                if (!result.isValid())
                    setValidateResult(getString(R.string.notValidFile));
                else {
                    setValidFileString(result.getData());
                }
            }
        });
    }

    private void setValidFileString(SignatureData data) {
        FirebaseFirestore.getInstance().collection("users").document(data.getUserId()).get().addOnSuccessListener(documentSnapshot -> {
            User user = documentSnapshot.toObject(User.class);
//            ezLog.debug( "setValidFileString: user" + user.toString());
            String res = "✅✅✅ Valid File! ✅✅✅\nSigned by " + user.getName() + " at " +
                    new SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.US).format(data.getTimestamp());
            setValidateResult(res);
        });
    }

    private void setValidateResult(String res) {
        binding.mainLBLResult.setText(res);
        binding.mainLBLResult.setVisibility(View.VISIBLE);
        binding.mainBTNSelectFile.setEnabled(true);
    }

    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // intent.setType("application/pdf");

        //intent.setType("text/plain");


        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, Constants.MIME_TYPES);

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, SIGN_FILE);
    }


    @SneakyThrows
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK) {

            switch (requestCode) {
                case SIGN_FILE:
                    if (resultData != null) {
                        uri = resultData.getData();
                        binding.mainLBLUri.setText(getString(R.string.filePath, uri.getPath()));
                        binding.mainLBLResult.setVisibility(View.GONE);
                        mimeType = getContentResolver().getType(uri);

                    }
                    break;
                case SAVE_FILE:
                    saveFileToPath(resultData.getData());
                    break;

            }


        }
    }

    private void saveFileToPath(Uri data) {
        TaskRunner<Boolean> taskRunner = new TaskRunner<>();
        taskRunner.executeAsync(new BaseTask<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return Files.copyFile(MainActivity.this, resUri, data, mimeType);
            }

            @Override
            public void onPostExecute(Boolean result) {
                binding.mainLBLSaveResult.setText(getString(result ? R.string.savedSuccessfully : R.string.notSaved));
            }
        });


    }

    private void signFile(final Uri data) {
        if (uri == null) {
            shoWErrorDialog();
            return;
        }
        String fileName = binding.mainEDTOutputName.getEditText().getText().toString();
        boolean use = binding.mainCBOutputName.isChecked();
        TaskRunner<String> ts = new TaskRunner<>();
        //  binding.mainBARProgressIndicator.setVisibility(View.VISIBLE);
        ts.executeAsync(new RunnerCallback<String>() {

            @Override
            public void onPreExecute() {
                runOnUiThread(() -> {
                    binding.mainLBLResult.setVisibility(View.GONE);
                    showProgressDialog("Signing File");
                });

            }

            @Override
            public String call() throws Exception {

                try {

                    // Perform operations on the document using its URI.
                    return new DSA(MainActivity.this).signFile(data, use ? fileName : "");
                } catch (FileMimeException e) {
                    ezLog.logException(e.getMessage(), e);
                    return e.getMessage();
                } catch (java.io.EOFException e) {
                    ezLog.logException(e.getMessage(), e);
                    return ERROR;
                } catch (java.lang.SecurityException e) {
                    ezLog.logException(e.getMessage(), e);
                    showAccessErrorDialog();
                    return ACCESS_ERROR;
                }
            }

            @Override
            public void onPostExecute(String result) {
                hideProgressDialog();
                if (!result.equals(ERROR) && !result.equals(ACCESS_ERROR)) {
                    resUri = Uri.fromFile(new File(result));
                    binding.mainLBLUri.setText(getString(R.string.fileSigned));
                    //   binding.mainBARProgressIndicator.setVisibility(View.GONE);
                    uri = null;
                    showSignedDialog();
                } else
                    binding.mainLBLUri.setText(result);


            }
        });
    }

    private void showSignedDialog() {
        binding.mainLAYSignedDialog.setVisibility(View.VISIBLE);
        setButtonsEnabled(false);
    }

    private void hideSignedDialog() {
        binding.mainLAYSignedDialog.setVisibility(View.GONE);
        setButtonsEnabled(true);
    }

    private void sentToMail(String filePath) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(filePath));
        this.grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        final Intent emailIntent1 = new Intent(Intent.ACTION_SEND);
        emailIntent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent1.putExtra(Intent.EXTRA_STREAM, uri);
        // emailIntent1.putExtra(Intent.EXTRA_EMAIL, new String[]{"pazlavi29@gmail.com"});
        emailIntent1.putExtra(Intent.EXTRA_SUBJECT, "[" + "PDSA" + "] - " + " Signed File");
        emailIntent1.putExtra(Intent.EXTRA_TEXT, "\n\n\nSent from PDSA");
        emailIntent1.putExtra(Intent.EXTRA_CC, new String[]{""});
        emailIntent1.setData(Uri.parse("mailto:")); // or just "mailto:" for blank
        emailIntent1.setType("text/html");
        Intent chooser = Intent.createChooser(emailIntent1, "Send email using");
        List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            this.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(chooser);
    }


    private void saveSignedFile() {
        File f = new File(resUri.getPath());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, f.getName());
        intent.setType(mimeType);
        startActivityForResult(intent, SAVE_FILE);

    }


    private void showProgressDialog(String str) {
        binding.mainLAYProgressDialog.setVisibility(View.VISIBLE);
        binding.mainLBLProgressText.setText(str);
        setButtonsEnabled(false);

    }

    private void hideProgressDialog() {
        binding.mainLAYProgressDialog.setVisibility(View.GONE);
        setButtonsEnabled(true);


    }

    private void setButtonsEnabled(boolean enabled) {
        binding.mainBTNSelectFile.setEnabled(enabled);
        binding.mainBTNAccount.setEnabled(enabled);
        binding.mainBTNSelectFile.setEnabled(enabled);
        binding.mainBTNValidate.setEnabled(enabled);
        binding.mainBTNSign.setEnabled(enabled);

    }

    private void shoWErrorDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign failed")
                .setMessage("no selected file to sign / validate")
                .setPositiveButton("Dismiss", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showAccessErrorDialog() {
        runOnUiThread(() -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("AccessError")
                    .setMessage(ACCESS_ERROR)
                    .setPositiveButton("Dismiss", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }


    private void shouldShowAndroid11Note() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            binding.mainLAYAndroid11.setVisibility(View.GONE);
        }
    }
}