package com.example.applicationstage;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationRequest;

import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;


public class HistoriqueActivity extends AppCompatActivity {

    private DatabaseReference databaseRef;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    FirebaseUser currentUser;


    Trajet nouveauTrajet;
    String trajetId;
    int index = 1;
    private List<LocationEntry> locationHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historique);

        nouveauTrajet = new Trajet();

        currentUser = getIntent().getParcelableExtra("user");

        afficherHistoriqueListe();

        databaseRef = FirebaseDatabase.getInstance().getReference("trajets");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
                        enregistrerPositionUtilisateur(position);
                    }
                }
            }
        };

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        // Create a new instance of LocationRequest with the updated priority
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(10000)
                .setMaxUpdateDelayMillis(60000)
                .build();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        Log.d("LocationUpdates", "Interval: " + locationRequest.getIntervalMillis() + " ms");
        Log.d("LocationUpdates", "Fastest Interval: " + locationRequest.getMinUpdateIntervalMillis() + " ms");
    }


    private static class LocationEntry {
        LatLng position;
        long timestamp;

        LocationEntry(LatLng position, long timestamp) {
            this.position = position;
            this.timestamp = timestamp;
        }
    }


    private void enregistrerPositionUtilisateur(LatLng position) {
        long currentTime = System.currentTimeMillis();

        // Ajoute le point avec horodatage
        nouveauTrajet.addPoint(position, currentTime);

        checkTimeSpentAtSameLocation(position, currentTime);
        locationHistory.add(new LocationEntry(position, currentTime));

        String currentHeure = new SimpleDateFormat("EEEE HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> pointData = new HashMap<>();
        pointData.put("latitude", position.latitude);
        pointData.put("longitude", position.longitude);
        pointData.put("timestamp", currentTime);

        if (nouveauTrajet.getPoints().size() == 1) {
            nouveauTrajet.setHeure(currentHeure);
            nouveauTrajet.setUtilisateurId(currentUser.getUid());

            databaseRef.push().setValue(nouveauTrajet, (databaseError, databaseReference) -> {
                if (databaseError != null) {
                    Log.e("Firebase", "Erreur lors de l'enregistrement du trajet: " + databaseError.getMessage());
                } else {
                    trajetId = databaseReference.getKey();
                }
            });
        } else {
            // Mise à jour des points
            Map<String, Object> update = new HashMap<>();
            update.put(trajetId + "/points/" + index, pointData);

            databaseRef.updateChildren(update)
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Point enregistré avec succès"))
                    .addOnFailureListener(e -> Log.e("Firebase", "Erreur lors de l'enregistrement du point: " + e.getMessage()));

            index++;
        }
    }


    private void checkTimeSpentAtSameLocation(LatLng newPosition, long currentTime) {
        for (LocationEntry entry : locationHistory) {
            if (isSameLocation(entry.position, newPosition)) {
                long timeSpent = currentTime - entry.timestamp;
                if (timeSpent >= 3 * 60 * 1000) { // 10 minutes in milliseconds
                    envoyerNotificationDepassement(newPosition, currentTime, timeSpent);
                    return;
                }
            }
        }
    }

    private boolean isSameLocation(LatLng pos1, LatLng pos2) {
        final double tolerance = 0.0001; // Tolérance en degrés pour considérer deux positions comme identiques
        return Math.abs(pos1.latitude - pos2.latitude) < tolerance && Math.abs(pos1.longitude - pos2.longitude) < tolerance;
    }

    private void envoyerNotificationDepassement(LatLng position, long currentTime, long timeSpent) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("latitude", position.latitude);
            jsonBody.put("longitude", position.longitude);
            jsonBody.put("timestamp", currentTime);
            jsonBody.put("timeSpent", timeSpent);
            jsonBody.put("userId", currentUser.getUid());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("Points", "notification sent");

        String url ="https://us-central1-my-appstage.cloudfunctions.net/notifyAdmin";

        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    // Traitement de la réponse si nécessaire
                },
                error -> {
                    // Gestion des erreurs
                });

        requestQueue.add(jsonObjectRequest);
    }





    private void definirHeureFin() {
        // Fetch the start time from Firebase
        databaseRef.child(trajetId).child("heure").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve the start time
                    String startHeure = dataSnapshot.getValue(String.class);

                    // Get the current time formatted as "DayOfWeek HH:mm:ss"
                    String endHeure = new SimpleDateFormat("EEEE HH:mm:ss", Locale.getDefault()).format(new Date());

                    // Combine start time with end time
                    String combinedHeure = startHeure + " ===> " + endHeure;

                    // Create a map to update the trip's time in the database
                    Map<String, Object> update = new HashMap<>();
                    update.put("heure", combinedHeure);

                    // Update the database with the end time
                    databaseRef.child(trajetId).updateChildren(update)
                            .addOnSuccessListener(aVoid -> Log.d("Firebase", "Heure de fin enregistrée avec succès"))
                            .addOnFailureListener(e -> Log.e("Firebase", "Erreur lors de l'enregistrement de l'heure de fin: " + e.getMessage()));
                } else {
                    Log.e("Firebase", "Erreur: heure de début non trouvée");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Erreur lors de la récupération de l'heure de début: " + databaseError.getMessage());
            }
        });
    }



    private void afficherHistoriqueListe() {
        DatabaseReference trajetsRef = FirebaseDatabase.getInstance().getReference("trajets");

        trajetsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> dates = new ArrayList<>();
                for (DataSnapshot trajetSnapshot : dataSnapshot.getChildren()) {
                    String heureDebut = trajetSnapshot.child("heure").getValue(String.class);
                    dates.add(heureDebut);
                    Log.d("Points", "heure : " + heureDebut);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(HistoriqueActivity.this, android.R.layout.simple_list_item_1, dates);
                ListView listView = findViewById(R.id.listView);
                listView.setAdapter(adapter);

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    String heureDebut = dates.get(position);

                    // Parcourir les enfants du DataSnapshot pour trouver le trajet correspondant à l'heure de début
                    for (DataSnapshot trajetSnapshot : dataSnapshot.getChildren()) {
                        String heureDebutSnapshot = trajetSnapshot.child("heure").getValue(String.class);
                        if (heureDebut.equals(heureDebutSnapshot)) {
                            String trajetId = trajetSnapshot.getKey();
                            Log.d("Points", "trajet id : " + trajetId);
                            afficherItineraireSurCarte(trajetId);
                            break; // Sortir de la boucle une fois que l'ID du trajet est trouvé
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Erreur lors de la récupération des données: " + databaseError.getMessage());
            }
        });
    }

    private void afficherItineraireSurCarte(String trajetId) {
        DatabaseReference trajetRef = FirebaseDatabase.getInstance().getReference("trajets").child(trajetId);

        trajetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<LatLng> points = new ArrayList<>();
                    for (DataSnapshot pointSnapshot : dataSnapshot.child("points").getChildren()) {
                        double latitude = pointSnapshot.child("latitude").getValue(Double.class);
                        double longitude = pointSnapshot.child("longitude").getValue(Double.class);
                        LatLng latLng = new LatLng(latitude, longitude);
                        points.add(latLng);
                        Log.d("Points", "Latitude: " + latitude + ", Longitude: " + longitude);
                    }



                    // Définir le premier point comme le point de départ
                    LatLng depart = points.get(0);
                    // Définir le dernier point comme le point d'arrivée
                    LatLng destination = points.get(points.size() - 1);

                    String uriString = "https://www.google.com/maps/dir/?api=1";
                    uriString += "&origin=" + depart.latitude + "," + depart.longitude;
                    uriString += "&destination=" + destination.latitude + "," + destination.longitude;

                    // Ajouter les points intermédiaires comme waypoints
                    if (points.size() > 2) {
                        StringBuilder waypoints = new StringBuilder();
                        for (int i = 1; i < points.size() - 1; i++) {
                            LatLng point = points.get(i);
                            if (waypoints.length() > 0) {
                                waypoints.append("|");
                            }
                            waypoints.append(point.latitude).append(",").append(point.longitude);
                        }
                        uriString += "&waypoints=" + waypoints.toString();
                    }

                    // Création de l'intention implicite
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
                    mapIntent.setPackage("com.google.android.apps.maps");
                    mapIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    // Vérification de la disponibilité de l'application Google Maps
                    PackageManager packageManager = getPackageManager();
                    if (mapIntent.resolveActivity(packageManager) != null) {
                        // Si Google Maps est disponible, ouvrir l'application avec l'itinéraire
                        startActivity(mapIntent);
                    } else {
                        // Si Google Maps n'est pas disponible, afficher un message à l'utilisateur
                        Toast.makeText(HistoriqueActivity.this, "Google Maps n'est pas installé sur cet appareil", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("Firebase", "Trajet non trouvé dans la base de données");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("Firebase", "Erreur lors de la récupération des données: " + databaseError.getMessage());
            }
        });
    }





    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        definirHeureFin();
    }


    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }


}