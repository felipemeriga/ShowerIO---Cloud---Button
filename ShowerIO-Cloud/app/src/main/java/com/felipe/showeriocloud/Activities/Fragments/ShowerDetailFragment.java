package com.felipe.showeriocloud.Activities.Fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.felipe.showeriocloud.Activities.Authentication.LoginActivity;
import com.felipe.showeriocloud.Activities.Home.SplashScreen;
import com.felipe.showeriocloud.Activities.ShowerIO.ShowerNavigationDrawer;
import com.felipe.showeriocloud.Activities.SmartConfig.SearchForDevices;
import com.felipe.showeriocloud.Adapter.SpinnerHandler;
import com.felipe.showeriocloud.Aws.AwsIotCoreManager;
import com.felipe.showeriocloud.Aws.CognitoSyncClientManager;
import com.felipe.showeriocloud.Model.DeviceDO;
import com.felipe.showeriocloud.Model.DevicePersistance;
import com.felipe.showeriocloud.R;
import com.felipe.showeriocloud.Utils.ConvertIntegerToOption;
import com.felipe.showeriocloud.Utils.ServerCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ShowerDetailFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ShowerDetailFragment} factory method to
 * create an instance of this fragment.
 */
public class ShowerDetailFragment extends Fragment {

    private static final String TAG = "ShowerDetailFragment";

    private boolean connectionStatus;
    private ProgressDialog mProgressDialog;
    private String oldName;
    private ProgressDialog mqttProgressDialog;
    private ProgressDialog publishProgressDialog;
    private AlertDialog disconnectedDialog;
    private AlertDialog timeDialog;
    private AlertDialog nameDialog;
    private AlertDialog deleteDialog;
    private AlertDialog resetDialog;

    @BindView(R.id.mainGrid)
    public GridLayout mainGrid;

    @BindView(R.id.textGrid)
    public TextView deviceTitle;

    private DeviceDO device;
    public static String selectedShower;
    private SharedPreferences sharedPreferences;
    private final String SHOWERIO = "ShowerIO";

    @BindView(R.id.cardViewPlay)
    public CardView cardViewPlay;

    @BindView(R.id.cardViewName)
    public CardView cardViewName;

    @BindView(R.id.cardViewReset)
    public CardView cardViewReset;

    @BindView(R.id.cardViewExit)
    public CardView cardViewExit;

    @BindView(R.id.cardStatistics)
    public CardView cardStatistics;

    @BindView(R.id.cardInfo)
    public CardView cardInfo;

    private Boolean nameFlag;

    @BindView(R.id.scrollViewDetail)
    public ScrollView scrollView;

    private ColorStateList defaultColor;

    private OnFragmentInteractionListener mListener;

    private AwsIotCoreManager awsIotCoreManager;

    public ShowerDetailFragment() {
        // Required empty public constructor
        awsIotCoreManager = new AwsIotCoreManager();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        connectionStatus = false;
        View view = inflater.inflate(R.layout.fragment_shower_detail, container, false);

        ButterKnife.bind(this, view);

        nameFlag = false;
        defaultColor = cardViewPlay.getCardBackgroundColor();
        setSingleEvent(mainGrid);

        device = DevicePersistance.selectedDevice;
        if (device.getName().isEmpty() || device.getName().equals("UNAMED")) {
            deviceTitle.setText(R.string.noName);
            nameFlag = true;
            helpUserSetName();
        } else {
            deviceTitle.setText(device.getName());

        }
        enableAllFeatures();
        connectToMQTTclient();
        return view;


    }

    void enableAllFeatures() {
        if (nameFlag) {
            cardViewName.setCardBackgroundColor(Color.WHITE);
        } else {
            cardViewPlay.setCardBackgroundColor(Color.WHITE);
            cardViewName.setCardBackgroundColor(Color.WHITE);
            cardViewReset.setCardBackgroundColor(Color.WHITE);
            cardViewExit.setCardBackgroundColor(Color.WHITE);
            cardStatistics.setCardBackgroundColor(Color.WHITE);
            cardInfo.setCardBackgroundColor(Color.WHITE);
        }
    }

    void connectToMQTTclient() {
        mqttProgressDialog = new ProgressDialog(getActivity());
        mqttProgressDialog.setMessage("Carregando...");
        mqttProgressDialog.setCanceledOnTouchOutside(false);
        mqttProgressDialog.show();
        awsIotCoreManager.initializeIotCore(device.getUserId(), device.getIotCoreEndPoint(), getActivity(), new ServerCallback() {
            @Override
            public void onServerCallback(final boolean status, String response) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (status) {
                            mqttProgressDialog.dismiss();
                            checkConnection();
                        } else {
                            mqttProgressDialog.dismiss();
                            //Toast toast = new Toast(getContext());
                            Toast.makeText(getContext(),R.string.mqtt_conection_failed, Toast.LENGTH_LONG).show();
                            //toast.show();
                            Intent listOfDevices = new Intent(getContext(), ShowerNavigationDrawer.class);
                            startActivity(listOfDevices);
                            getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                            getActivity().finish();
                        }

                    }
                });
            }
        });
    }

    void checkConnection() {

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!connectionStatus) {
                    if (timeDialog != null) {
                        timeDialog.dismiss();
                    }
                    if (nameDialog != null) {
                        nameDialog.dismiss();
                    }
                    if (resetDialog != null) {
                        resetDialog.dismiss();
                    }
                    if (deleteDialog != null) {
                        deleteDialog.dismiss();
                    }

                    disconnectedDialog = new AlertDialog.Builder(getContext())
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //device.setStatus("OFFLINE");
                                    //DevicePersistance.fastUpdateDevice(device);
                                    mListener.onFragmentInteraction("ShowerListFragment");
                                }

                            }).create();
                    disconnectedDialog.setMessage("Tentamos contatar seu ShowerIO e parece que este esta desconectado," +
                            " verifique se o LED indicativo vermelho esta piscado, caso positivo, efetue uma nova busca para reconectar o mesmo");

                    disconnectedDialog.setCanceledOnTouchOutside(false);
                    disconnectedDialog.show();
                }
            }
        }, 2000);

        //Run on a new thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                awsIotCoreManager.publishConnectionCheck(device, new

                        ServerCallback() {
                            @Override
                            public void onServerCallback(boolean status, String response) {
                                if (status) {
                                    connectionStatus = true;
                                }
                            }
                        });

            }

        }).start();
    }


    private void setSingleEvent(GridLayout mainGrid) {
        //Loop all child item of Main Grid
        for (int i = 0; i < mainGrid.getChildCount(); i++) {
            //You can see , all child item is CardView , so we just cast object to CardView
            CardView cardView = (CardView) mainGrid.getChildAt(i);
            final int finalI = i;

            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (finalI) {
                        case 0:
                            if (!nameFlag) {
                                Log.i("ShowerDetailFragment", "case 0, opening dialog to set bath parms");
                                onStartPressed();
                            }
                            break;
                        case 1:
                            Log.i("ShowerDetailFragment", "case 1, opening form to change name");
                            onSetNamePressed();
                            break;
                        case 2:
                            Log.i("ShowerDetailFragment", "case 2, opening confirmation dialog to reset");
                            onResetPressed();
                            break;
                        case 3:
                            Log.i("ShowerDetailFragment", "case 3, going back to ShowerNavigationDrawer");
                            mListener.onFragmentInteraction("ShowerListFragment");
                            break;
                        case 4:
                            Log.i("ShowerDetailFragment", "case 4, opening statistics fragment");
                            mListener.onFragmentInteraction("StatisicsDetailFragment");
                            break;
                        case 5:
                            Log.i("ShowerDetailFragment", "case 5, opening confirmation dialog to delete");
                            onDeletePressed();
                            break;
                    }
                }
            });
        }
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String fragmentName);

    }


    private void helpUserSetName() {
        // showing toast to help user to use the application
        Toast.makeText(getContext(),
                R.string.dialog_name_hint,
                Toast.LENGTH_SHORT).show();
    }

    private void onStartPressed() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this.getContext());
        View mView = getLayoutInflater().inflate(R.layout.dialog_control_device, null);
        final Spinner mSpinnerBathTime = (Spinner) mView.findViewById(R.id.spinnerBathTime);
        final Spinner mSpinnerBathPosTime = (Spinner) mView.findViewById(R.id.spinnerBathPosTime);
        Button mSetTimes = (Button) mView.findViewById(R.id.btnApplyTimes);

        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.times));
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinnerBathTime.setAdapter(mAdapter);
        mSpinnerBathPosTime.setAdapter(mAdapter);

        mSpinnerBathTime.setPrompt("B1");
        mSpinnerBathPosTime.setPrompt("B2");
        mSpinnerBathTime.setSelection(returnHardCoddedPosition(device.getBathTime()));
        mSpinnerBathPosTime.setSelection(returnHardCoddedPosition(device.getWaitingTime()));

        mBuilder.setView(mView);
        timeDialog = mBuilder.create();
        timeDialog.show();
        mSetTimes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeDialog.dismiss();
                publishProgressDialog = new ProgressDialog(getActivity());
                publishProgressDialog.setMessage("Aplicando...");
                publishProgressDialog.setCanceledOnTouchOutside(false);
                publishProgressDialog.show();

                awsIotCoreManager.publishBathParams(returnHardCoddedMinutes(mSpinnerBathTime.getSelectedItemPosition())
                        , returnHardCoddedMinutes(mSpinnerBathPosTime.getSelectedItemPosition())
                        , device
                        , new ServerCallback() {
                            @Override
                            public void onServerCallback(boolean status, String response) {
                                publishProgressDialog.dismiss();
                                Toast.makeText(getContext(),
                                        R.string.dialog_set_time_success,
                                        Toast.LENGTH_SHORT).show();
                                if (status) {
                                    DevicePersistance.fastUpdateDevice(device);
                                } else {
                                    Toast.makeText(getContext(),
                                            R.string.dialog_set_time_fail,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

    }

    private void onResetPressed() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.reset_title);
        builder.setMessage(R.string.reset_text);
        builder.setCancelable(false);
        builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                awsIotCoreManager.publishReset(device, new ServerCallback() {
                    @Override
                    public void onServerCallback(boolean status, String response) {
                        if (status) {
                            DevicePersistance.fastUpdateDevice(device);
                            mListener.onFragmentInteraction("ShowerListFragment");
                        } else {
                            Toast.makeText(getContext(), R.string.reset_fail, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        builder.setNegativeButton("Não", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        resetDialog = builder.create();
        resetDialog.show();

    }

    private void onDeletePressed() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.delete_title);
        builder.setMessage(R.string.delete_text);
        builder.setCancelable(false);
        builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                awsIotCoreManager.publishDelete(new ServerCallback() {
                    @Override
                    public void onServerCallback(boolean status, String response) {
                        if (status) {
                            DevicePersistance.deleteDevice(device);
                            // TODO - Delete all Statistics
                            mListener.onFragmentInteraction("ShowerListFragment");
                        } else {
                            Toast.makeText(getContext(), R.string.delete_fail, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        builder.setNegativeButton("Não", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        deleteDialog = builder.create();
        deleteDialog.show();
    }

    private void onSetNamePressed() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this.getContext());
        View mView = getLayoutInflater().inflate(R.layout.dialog_name_form, null);
        final EditText mName = (EditText) mView.findViewById(R.id.etName);
        Button mSetName = (Button) mView.findViewById(R.id.btnSetName);
        mBuilder.setView(mView);
        final Context fragmentContext = this.getContext();
        nameDialog = mBuilder.create();
        nameDialog.setCanceledOnTouchOutside(true);
        nameDialog.show();
        mSetName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mName.getText().toString().isEmpty()) {
                    nameDialog.dismiss();
                    setNewNameCall(mName.getText().toString());
                } else {
                    Toast.makeText(fragmentContext,
                            R.string.dialog_set_name_fail,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void setNewNameCall(final String name) {
        oldName = device.getName();
        mProgressDialog = new ProgressDialog(this.getContext());
        mProgressDialog.setMessage("Mudando nome do dispositivo...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
        device.setName(name);
        final Context fragmentContext = this.getContext();

        DevicePersistance.updateDevice(device, new ServerCallback() {
            @Override
            public void onServerCallback(final boolean status, String response) {

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (status) {
                            mProgressDialog.dismiss();
                            deviceTitle.setText(name);
                            nameFlag = false;
                        } else {
                            mProgressDialog.dismiss();
                            Toast.makeText(fragmentContext,
                                    R.string.dialog_set_name_fail,
                                    Toast.LENGTH_SHORT).show();
                            deviceTitle.setText(oldName);
                            device.setName(oldName);
                        }
                        enableAllFeatures();
                    }
                });
            }
        });
    }

    // FUTURE - That is a hard codded array of values in slider, change this for dynamic values from database
    int returnHardCoddedPosition(int minutes) {
        if (minutes == 25) {
            return 21;
        } else if (minutes > 25) {
            return 22;
        } else {
            return minutes;
        }
    }

    // FUTURE - That is a hard codded array of values in slider, change this for dynamic values from database
    int returnHardCoddedMinutes(int position) {
        if (position == 21) {
            return 25;
        } else if (position == 22) {
            return 99;
        } else {
            return position;
        }
    }

}



