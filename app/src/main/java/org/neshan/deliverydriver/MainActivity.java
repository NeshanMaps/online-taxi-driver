package org.neshan.deliverydriver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.carto.core.ScreenBounds;
import com.carto.core.ScreenPos;
import com.carto.graphics.Color;
import com.carto.styles.AnimationStyle;
import com.carto.styles.AnimationStyleBuilder;
import com.carto.styles.AnimationType;
import com.carto.styles.LineStyle;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.MarkerStyle;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.utils.BitmapUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.neshan.common.model.LatLng;
import org.neshan.common.model.LatLngBounds;
import org.neshan.common.utils.PolylineEncoding;
import org.neshan.deliverydriver.model.Travel;
import org.neshan.deliverydriver.model.User;
import org.neshan.mapsdk.MapView;
import org.neshan.mapsdk.model.Marker;
import org.neshan.mapsdk.model.Polyline;
import org.neshan.servicessdk.direction.NeshanDirection;
import org.neshan.servicessdk.direction.model.DirectionStep;
import org.neshan.servicessdk.direction.model.NeshanDirectionResult;
import org.neshan.servicessdk.direction.model.Route;

import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final int CALL_PHONE_PERMISSION_REQUEST_CODE = 1001;
    private static final int NEAR_MOTORS_DISTANCE_KILOMETERS = 2;

    private MapView map;
    private ConstraintLayout layoutRequest;
    private AppCompatTextView lblPrice;
    private AppCompatTextView lblSourceAddress;
    private AppCompatTextView lblDestinationAddress;
    private SwitchCompat switchGoOnline;
    private Button btnAcceptTravel;
    private Button btnReachedToSource;
    private AppCompatTextView lblUserName;
    private LinearLayout layoutUserName;
    private LinearLayout layoutRouting;
    private LinearLayout layoutNavigation;
    private LinearLayout layoutSource;
    private LinearLayout layoutDestination;
    private LinearLayout layoutPrice;
    private ConstraintLayout layoutFinishTravel;
    private ImageView imgCall;
    private Button btnPassengerBoarded;
    private Button btnPassengerGotOff;
    private Button btnOk;
    private AppCompatTextView lblFinishText;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationCallback locationCallback;
    private Location myLocation;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private Marker userMarker;
    private Handler handler = new Handler();
    private Runnable runnable;
    private ArrayList<LatLng> decodedStepByStepPath;
    private Polyline mapPolyline;
    private Travel travel;
    private LatLng myLocationLatLng;
    private LatLng sourceLatLng;
    private LatLng destinationLatLng;
    private Marker sourceMarker;
    private Marker destinationMarker;
    private LatLng northEast;
    private LatLng southWest;
    private boolean destinationRouting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        initLayoutReferences();

        getMyFakeLocation();

        runnable = new Runnable() {
            @Override
            public void run() {
                travel = makeFakeRequest();
                showRequest(travel);
            }
        };

        imgCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callNumber(travel.getUser().getPhoneNumber());
            }
        });

        btnAcceptTravel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, getString(R.string.long_press_for_accept_travel), Toast.LENGTH_SHORT).show();
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeFinishTravelWindow();
            }
        });

        btnReachedToSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, getString(R.string.long_press_for_accept), Toast.LENGTH_SHORT).show();
            }
        });

        btnReachedToSource.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                destinationRouting = true;
                if (mapPolyline != null) {
                    map.removePolyline(mapPolyline);
                }
                if (sourceMarker != null) {
                    map.removeMarker(sourceMarker);
                }
                layoutRouting.setVisibility(View.VISIBLE);
                layoutNavigation.setVisibility(View.GONE);
                mapSetPosition(northEast, southWest);
                layoutSource.setVisibility(View.GONE);
                layoutPrice.setVisibility(View.VISIBLE);
                btnReachedToSource.setVisibility(View.GONE);
                btnPassengerBoarded.setVisibility(View.VISIBLE);
                return true;
            }
        });

        btnPassengerBoarded.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, getString(R.string.long_press_for_accept), Toast.LENGTH_SHORT).show();
            }
        });

        btnPassengerBoarded.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                view.setVisibility(View.GONE);
                btnPassengerGotOff.setVisibility(View.VISIBLE);
                return true;
            }
        });

        btnPassengerGotOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, getString(R.string.long_press_for_accept), Toast.LENGTH_SHORT).show();
            }
        });

        btnPassengerGotOff.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                resetRequestWindow();
                showFinishTravelWindow();
                if (mapPolyline != null) {
                    map.removePolyline(mapPolyline);
                }
                if (destinationMarker != null) {
                    map.removeMarker(destinationMarker);
                }
                map.moveCamera(myLocationLatLng, .5f);
                return true;
            }
        });

        btnAcceptTravel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                btnAcceptTravel.setVisibility(View.GONE);
                layoutUserName.setVisibility(View.VISIBLE);
                layoutRouting.setVisibility(View.VISIBLE);
                btnReachedToSource.setVisibility(View.VISIBLE);
                return true;
            }
        });

        layoutRouting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!destinationRouting) {
                    if (myLocationLatLng != null && sourceLatLng != null) {
                        neshanRoutingApi(myLocationLatLng, sourceLatLng);
                    }
                } else {
                    if (myLocationLatLng != null && destinationLatLng != null) {
                        neshanRoutingApi(myLocationLatLng, destinationLatLng);
                    }
                }
            }
        });

        layoutNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!destinationRouting) {
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("nshn:" + sourceLatLng.getLatitude() + "," + sourceLatLng.getLongitude()));
                    startActivity(intent);
                } else {
                    Intent intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("nshn:" + destinationLatLng.getLatitude() + "," + destinationLatLng.getLongitude()));
                    startActivity(intent);
                }
            }
        });

        switchGoOnline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    compoundButton.setText(getString(R.string.on));
                    requestTravel();
                } else {
                    resetRequestWindow();
                    map.clearMarkers();
                    handler.removeCallbacks(runnable);
                    compoundButton.setText(getString(R.string.off));
                    if (myLocation != null) {
                        map.moveCamera(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), 0.5f);
                        map.setZoom(14, 0.5f);
                    }
                }
            }
        });

    }

    private void closeFinishTravelWindow() {
        layoutFinishTravel.setVisibility(View.GONE);
    }

    private void resetRequestWindow() {
        if (mapPolyline != null) {
            map.removePolyline(mapPolyline);
        }
        if (sourceMarker != null) {
            map.removeMarker(sourceMarker);
        }
        if (destinationMarker != null) {
            map.removeMarker(destinationMarker);
        }
        destinationRouting = false;
        layoutRequest.setVisibility(View.GONE);
        layoutPrice.setVisibility(View.GONE);
        btnPassengerGotOff.setVisibility(View.GONE);
        btnAcceptTravel.setVisibility(View.VISIBLE);
        layoutSource.setVisibility(View.VISIBLE);
        layoutRouting.setVisibility(View.GONE);
        layoutUserName.setVisibility(View.GONE);
        layoutNavigation.setVisibility(View.GONE);
    }

    private void showFinishTravelWindow() {
        layoutFinishTravel.setVisibility(View.VISIBLE);
        lblFinishText.setText(String.format(getString(R.string.you_earned_cash), String.valueOf(travel.getPrice())));
    }

    private void requestTravel() {
        handler.postDelayed(runnable, 4000);
    }

    private void initLayoutReferences() {
        initViews();

        initMap();
    }

    private void initMap() {
        map.moveCamera(new LatLng(35.767234, 51.330743), 0);
    }

    private void initViews() {
        map = findViewById(R.id.map);
        layoutRequest = findViewById(R.id.layout_request);
        lblSourceAddress = findViewById(R.id.lbl_source_address);
        lblDestinationAddress = findViewById(R.id.lbl_destination_address);
        lblPrice = findViewById(R.id.lbl_price);
        switchGoOnline = findViewById(R.id.switch_go_online);
        btnAcceptTravel = findViewById(R.id.btn_accept);
        btnReachedToSource = findViewById(R.id.btn_reached_to_source);
        lblUserName = findViewById(R.id.lbl_user_name);
        layoutUserName = findViewById(R.id.layout_user_name);
        layoutRouting = findViewById(R.id.layout_routing);
        layoutNavigation = findViewById(R.id.layout_navigation);
        imgCall = findViewById(R.id.img_call);
        layoutSource = findViewById(R.id.layout_source);
        layoutDestination = findViewById(R.id.layout_destination);
        layoutPrice = findViewById(R.id.layout_price);
        btnPassengerBoarded = findViewById(R.id.btn_passenger_boarded);
        btnPassengerGotOff = findViewById(R.id.btn_passenger_got_off);
        layoutFinishTravel = findViewById(R.id.layout_finish_travel);
        lblFinishText = findViewById(R.id.lbl_finish_text);
        btnOk = findViewById(R.id.btn_ok);
    }

    private Travel makeFakeRequest() {
        Travel travel = new Travel();
        travel.setSourceLat(35.71834).setSourceLng(51.28660).setDestinationLat(35.74969066).setDestinationLng(51.401011967).setSourceAddress("تهران، ورزشگاه آزادی، شیشه مینا").setDestinationAddress("تهران، شیراز، همت، شیراز جنوبی");
        travel.setPrice(53000);
        User user = new User();
        user.setId(UUID.randomUUID().toString()).setName("فرهاد کاکایی");
        user.setPhoneNumber("+9891212345678");
        travel.setUser(user);
        return travel;
    }

    private void callNumber(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRequest(Travel travel) {
        map.clearMarkers();
        layoutPrice.setVisibility(View.VISIBLE);
        lblSourceAddress.setText(travel.getSourceAddress());
        lblDestinationAddress.setText(travel.getDestinationAddress());
        lblPrice.setText(new StringBuilder().append(travel.getPrice()).append(" ").append(getString(R.string.tooman)).toString());
        lblUserName.setText(travel.getUser().getName());
        sourceLatLng = new LatLng(travel.getSourceLat(), travel.getSourceLng());
        destinationLatLng = new LatLng(travel.getDestinationLat(), travel.getDestinationLng());
        sourceMarker = addMarker(sourceLatLng, R.drawable.source_marker);
        destinationMarker = addMarker(destinationLatLng, R.drawable.destination_marker);

        double minLat = Math.min(sourceLatLng.getLatitude(), destinationLatLng.getLatitude());
        double minLng = Math.min(sourceLatLng.getLongitude(), destinationLatLng.getLongitude());

        double maxLat = Math.max(sourceLatLng.getLatitude(), destinationLatLng.getLatitude());
        double maxLng = Math.max(sourceLatLng.getLongitude(), destinationLatLng.getLongitude());

        northEast = new LatLng(maxLat, minLng);
        southWest = new LatLng(minLat, maxLng);

        mapSetPosition(northEast, southWest);

        layoutRequest.setVisibility(View.VISIBLE);
    }

    private Marker addMarker(LatLng loc, int markerDrawable) {
        AnimationStyleBuilder animStBl = new AnimationStyleBuilder();
        animStBl.setFadeAnimationType(AnimationType.ANIMATION_TYPE_SMOOTHSTEP);
        animStBl.setSizeAnimationType(AnimationType.ANIMATION_TYPE_SPRING);
        animStBl.setPhaseInDuration(0.5f);
        animStBl.setPhaseOutDuration(0.5f);
        AnimationStyle animSt = animStBl.buildStyle();

        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(30f);
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), markerDrawable)));
        markStCr.setAnimationStyle(animSt);
        MarkerStyle markSt = markStCr.buildStyle();

        Marker marker = new Marker(loc, markSt);

        map.addMarker(marker);
        return marker;
    }

    private void getMyFakeLocation() {
        myLocation = new Location("");
        myLocation.setLatitude(35.701433073);
        myLocation.setLongitude(51.337892468);
        myLocationLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        addUserMarker(myLocationLatLng);
        map.moveCamera(myLocationLatLng, .5f);
    }

    private void getMyLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                myLocation = locationResult.getLastLocation();

                onLocationChange();
                stopLocationUpdates();
            }
        };

        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();

    }

    public void stopLocationUpdates() {
        fusedLocationClient
                .removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                    }
                });
    }

    private void onLocationChange() {
        if (myLocation != null) {
            addUserMarker(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()));
        }
    }

    private void addUserMarker(LatLng loc) {
        if (userMarker != null) {
            map.removeMarker(userMarker);
        }
        MarkerStyleBuilder markStCr = new MarkerStyleBuilder();
        markStCr.setSize(25f);
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.car)));
        MarkerStyle markSt = markStCr.buildStyle();

        userMarker = new Marker(loc, markSt);

        map.addMarker(userMarker);
    }

    private void neshanRoutingApi(LatLng source, LatLng destination) {
        //TODO: Replace API-KEY with your api key
        new NeshanDirection.Builder("API-KEY", source, destination)
                .build().call(new Callback<NeshanDirectionResult>() {
                    @Override
                    public void onResponse(Call<NeshanDirectionResult> call, Response<NeshanDirectionResult> response) {
                        if (response != null && response.body() != null && response.body().getRoutes() != null && !response.body().getRoutes().isEmpty()) {
                            Route route = response.body().getRoutes().get(0);
                            decodedStepByStepPath = new ArrayList<>();

                            // decoding each segment of steps and putting to an array
                            for (DirectionStep step : route.getLegs().get(0).getDirectionSteps()) {
                                decodedStepByStepPath.addAll(PolylineEncoding.decode(step.getEncodedPolyline()));
                            }

                            if (mapPolyline != null) {
                                map.removePolyline(mapPolyline);
                            }
                            mapPolyline = new Polyline(decodedStepByStepPath, getLineStyle());

                            //draw polyline between route points
                            map.addPolyline(mapPolyline);

                            // focusing camera on first point of drawn line
                            mapSetPosition(source, destination);
                            Location sourceLocation = new Location("");
                            sourceLocation.setLatitude(source.getLatitude());
                            sourceLocation.setLongitude(source.getLongitude());

                            layoutRouting.setVisibility(View.GONE);
                            layoutNavigation.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<NeshanDirectionResult> call, Throwable t) {
                        Log.e("err", t.toString());
                    }
                });
    }

    private LineStyle getLineStyle() {
        LineStyleBuilder lineStCr = new LineStyleBuilder();
        lineStCr.setColor(new Color((short) 2, (short) 119, (short) 189, (short) 190));
        lineStCr.setWidth(10f);
        lineStCr.setStretchFactor(0f);
        return lineStCr.buildStyle();
    }

    private void mapSetPosition(LatLng source, LatLng destination) {

        double minLat = Math.min(source.getLatitude(), destination.getLatitude());
        double minLng = Math.min(source.getLongitude(), destination.getLongitude());

        double maxLat = Math.max(source.getLatitude(), destination.getLatitude());
        double maxLng = Math.max(source.getLongitude(), destination.getLongitude());

        LatLng northEast = new LatLng(maxLat, maxLng + 0.0005);
        LatLng southWest = new LatLng(minLat, minLng - 0.0005);

        map.moveToCameraBounds(new LatLngBounds(northEast, southWest), new ScreenBounds(new ScreenPos(0, 0), new ScreenPos(map.getWidth(), map.getHeight())), true, 0);
        LatLng shiftToTopMapLatLng = new LatLng(map.getCameraTargetPosition().getLatitude() - 0.01, map.getCameraTargetPosition().getLongitude());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                map.moveCamera(shiftToTopMapLatLng, 0);
            }
        }, 200);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CALL_PHONE_PERMISSION_REQUEST_CODE) {
            callNumber(travel.getUser().getPhoneNumber());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean checkCallPhonePermission() {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getCallPhonePermission() {
        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, CALL_PHONE_PERMISSION_REQUEST_CODE);
    }
}