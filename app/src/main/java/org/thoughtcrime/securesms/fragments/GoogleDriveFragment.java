package org.thoughtcrime.securesms.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.util.DriveServiceHelper;

import java.util.Collections;

public class GoogleDriveFragment extends Fragment {
    private final String TAG = GoogleDriveFragment.class.toString();

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private Pair<String, String> lastReadFilePair = null;
//    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

    private DriveServiceHelper driveServiceHelper;

    private void setLastReadFilePair(Pair<String, String> pair) {
        lastReadFilePair = pair;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.google_drive_fragment, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.sign_in).setOnClickListener(unused -> {
            Log.i(TAG, "Requesting Google Account Sign in...");
            requestSignIn();
        });

        view.findViewById(R.id.create_file).setOnClickListener(unused -> {
            driveServiceHelper.createFile()
                    .addOnSuccessListener(fileId -> {
                        Log.d(TAG, "File successfully created with ID: " + fileId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    });
        });
        view. findViewById(R.id.read_file).setOnClickListener(unused -> {
            Log.i(TAG, "Read File");
            driveServiceHelper.queryFiles()
                    .addOnSuccessListener(fileList -> {
                        if (fileList.getFiles().size() == 0) {
                            return;
                        }
                        String fileId = fileList.getFiles().get(0).getId();
                        driveServiceHelper.readFile(fileId)
                                .addOnSuccessListener(pair -> {
                                    setLastReadFilePair(pair);
                                    Log.d(TAG, "File successfully read with ID: " + pair.first);
                                    Log.d(TAG, "File Contents: " + pair.second);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, e.getMessage());
                                    e.printStackTrace();
                                });
                        Log.d(TAG, "File successfully created with ID: " + fileId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    });
        });
        view.findViewById(R.id.update_file).setOnClickListener(unused -> {
            Log.i(TAG, "Update File");
            if (lastReadFilePair == null) {
                return;
            }
            // saving an example text file with content: 'Hello world!' :-) as it should be
            driveServiceHelper.saveFile(lastReadFilePair.first, "Hello_world.txt", "Hello world!")
                    .addOnSuccessListener(unusedParam -> {
                        Log.d(TAG, "File successfully updated with ID: " + lastReadFilePair.first);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, e.getMessage());
                        e.printStackTrace();
                    });

        });

//        requestSignIn();
//        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
//            }
//        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultData != null) {
                    handleSignInResult(resultData);
                }
                break;
            default:
                Log.e(TAG, "Unable to Sign In... Please contact the developer.");
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }

    /**
     * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
     */
    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(getActivity(), signInOptions);

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private void setViewVisibility(View view, int tagId, int visibility) {
        view.findViewById(tagId).setVisibility(visibility);
    }

    private void setButtonsVisibility() {
        View view = getView();
        setViewVisibility(view, R.id.create_file, View.VISIBLE);
        setViewVisibility(view, R.id.read_file, View.VISIBLE);
        setViewVisibility(view, R.id.update_file, View.VISIBLE);
        setViewVisibility(view, R.id.sign_in, View.GONE);

//        ConstraintLayout constraintLayout = view.findViewById(R.id.example_parent);
//        ConstraintSet constraintSet = new ConstraintSet();
//        constraintSet.clone(constraintLayout);
//        constraintSet.connect(R.id.example_title, ConstraintSet.BOTTOM, R.id.create_file, 0, 0);
//        view.findViewById(R.id.example_title);
    }

    /**
     * Handles the {@code result} of a completed sign-in activity initiated from {@link
     * #requestSignIn()}.
     */
    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in successfully as: " + googleAccount.getEmail());

                    setButtonsVisibility();
                    HttpTransport transport = AndroidHttp.newCompatibleTransport();

                    // Use the authenticated account to sign in to the Drive service.
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    getActivity(), Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    Drive googleDriveService =
                            new Drive.Builder(
                                    transport,
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Signal")
                                    .build();

                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.
                    driveServiceHelper = new DriveServiceHelper(googleDriveService);
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }
}
