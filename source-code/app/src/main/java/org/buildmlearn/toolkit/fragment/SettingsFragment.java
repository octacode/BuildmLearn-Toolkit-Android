package org.buildmlearn.toolkit.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.buildmlearn.toolkit.R;
import org.buildmlearn.toolkit.ToolkitApplication;
import org.buildmlearn.toolkit.activity.DeepLinkerActivity;
import org.buildmlearn.toolkit.utilities.RestoreThread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by abhishek on 21/06/15 at 9:51 PM.
 */
public class SettingsFragment extends PreferenceFragment {

    private static final int REQUEST_PICK_APK = 9985;
    private Preference prefUsername;
    public static float deleteDirectory(File file, float size) {
        if (file.exists()) {
            File[] listFiles = file.listFiles();
            if (listFiles == null) return 0;

            for (int i = 0; i < listFiles.length; i++) {
                if (listFiles[i].isDirectory()) {
                    size += deleteDirectory(listFiles[i], 0);
                } else {
                    size += listFiles[i].length();
                    listFiles[i].delete();
                }
            }
        }
        file.delete();
        return (size);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_settings);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Preference deleteTempFiles = findPreference(getString(R.string.key_delete_temporary_files));
        deleteTempFiles.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                String path = ToolkitApplication.getUnZipDir();
                float size = deleteDirectory(new File(path), 0);
                size = (float) ((float) Math.round((size / 1048576) * 100d) / 100d);
                if (size != 0) {
                    Toast.makeText(SettingsFragment.this.getActivity(), "Deleted " + size + " MB.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsFragment.this.getActivity(), "No Temp Files Found!", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        Preference restoreProject = findPreference(getString(R.string.key_restore_project));
        restoreProject.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                initRestoreProjectDialog();
                return true;
            }
        });

        prefUsername = findPreference(getString(R.string.key_user_name));
        prefUsername.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                prefUsername.setSummary((String) newValue);
                return true;
            }
        });
        prefUsername.setSummary(preferences.getString(getString(R.string.key_user_name), ""));
    }

    public void initRestoreProjectDialog() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/*");
        startActivityForResult(intent, REQUEST_PICK_APK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_PICK_APK:

                if (resultCode == Activity.RESULT_OK) {

                    try {
                        final ProgressDialog processDialog = new ProgressDialog(getActivity());
                                processDialog.setTitle(R.string.restore_progress_dialog);
                                processDialog.setMessage("Restoring Project from Apk");
                                processDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                processDialog.setIndeterminate(true);
                                processDialog.show();


                        InputStream inputStream = getActivity().getContentResolver().openInputStream(data.getData());
                        RestoreThread restore = new RestoreThread(getActivity(), inputStream,processDialog);
                        restore.setRestoreListener(new RestoreThread.OnRestoreComplete() {
                            @Override
                            public void onSuccess(File assetFile) {
                                processDialog.dismiss();
                                Intent intentProject = new Intent(getActivity(), DeepLinkerActivity.class);
                                intentProject.setData(Uri.fromFile(assetFile));
                                getActivity().startActivity(intentProject);
                            }
                            @Override
                            public void onFail() {
                                processDialog.dismiss();
                                final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.dialog_restore_title)
                                        .setMessage(R.string.dialog_restore_failed)
                                        .setPositiveButton(R.string.info_template_ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .create();
                                dialog.show();
                            }

                            @Override
                            public void onFail(Exception e) {
                                processDialog.dismiss();
                                final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.dialog_restore_title)
                                        .setMessage(R.string.dialog_restore_failed)
                                        .setPositiveButton(R.string.info_template_ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .create();
                                dialog.show();
                            }
                        });

                        restore.start();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();

                        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                .setTitle(R.string.dialog_restore_title)
                                .setMessage(R.string.dialog_restore_fileerror)
                                .setPositiveButton(R.string.info_template_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create();
                        dialog.show();
                    }


                }

                break;
            default: //do nothing
                break;
        }

    }
}
