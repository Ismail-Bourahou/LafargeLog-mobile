package com.example.applicationstage;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.firebase.auth.FirebaseUser;

import android.Manifest;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;



import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import android.view.MenuItem;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.navigation.NavigationView;


public class HomeActivity extends AppCompatActivity {

    private PlacesClient placesClient;
    FirebaseUser currentUser;
    private HistoriqueFragment historiqueFragment;


    public DrawerLayout drawerLayout;
    public ActionBarDrawerToggle actionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        currentUser = getIntent().getParcelableExtra("user");

        if (savedInstanceState == null) {
            historiqueFragment = new HistoriqueFragment();
            Bundle args = new Bundle();
            args.putParcelable("user", currentUser);
            historiqueFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, historiqueFragment, "HISTORIQUE_FRAGMENT")
                    .commit();
        } else {
            historiqueFragment = (HistoriqueFragment) getSupportFragmentManager().findFragmentByTag("HISTORIQUE_FRAGMENT");
        }

        NavigationView navigationView = findViewById(R.id.nav_view);

        MenuItem navUserEmail = navigationView.getMenu().findItem(R.id.nav_user_email);
        if (currentUser != null && currentUser.getEmail() != null) {
            navUserEmail.setTitle(currentUser.getEmail());
        }

        TextView subTitleTextView = findViewById(R.id.subTitle);


        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_historique) {
                subTitleTextView.setText("View your recent journeys:");
                ListView placesListView = findViewById(R.id.placesListView);
                if (placesListView.getVisibility() == VISIBLE) {
                    replaceFragment(historiqueFragment);
                }
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_logout) {
                logout();
                drawerLayout.closeDrawers();
                return true;

            } else if (id == R.id.nav_home) {
                subTitleTextView.setText("Discover nearby places:");
                ListView placesListView = findViewById(R.id.placesListView);
                if (placesListView.getVisibility() == GONE) {
                    replaceFragment(null);
                }
                drawerLayout.closeDrawers();
                return true;
            }

            return false;
        });







        drawerLayout = findViewById(R.id.my_drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close);

        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }











        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);



        // afficher les places a proximité
        requestNearbyPlaces();
    }


    @Override
    protected void onStart() {
        super.onStart();

        replaceFragment(historiqueFragment);
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            if (currentFragment != null) {
                fragmentTransaction.hide(currentFragment);
            }
            ListView placesListView = findViewById(R.id.placesListView);
            placesListView.setVisibility(View.VISIBLE);
        } else {
            if (currentFragment != null) {
                fragmentTransaction.hide(currentFragment);
            }
            fragmentTransaction.show(fragment);
            ListView placesListView = findViewById(R.id.placesListView);
            placesListView.setVisibility(View.GONE); // Utiliser View.GONE pour masquer complètement
        }

        fragmentTransaction.commit();
    }




    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }






    private void requestNearbyPlaces() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // La permission est accordée, nous pouvons appeler findCurrentPlace()
            List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG);
            FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);
            Log.d("Places", "Lieux récupérés avec succès.");
            placesClient.findCurrentPlace(request).addOnSuccessListener((response) -> {
                List<String> placesNames = new ArrayList<>();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, placesNames);
                ListView placesListView = findViewById(R.id.placesListView);
                placesListView.setAdapter(adapter);

                for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                    placesNames.add(placeLikelihood.getPlace().getName());
                }

                // la methode qui renvoie l'itineraire
                placesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Place selectedPlace = response.getPlaceLikelihoods().get(position).getPlace();
                        LatLng destinationLatLng = selectedPlace.getLatLng();

                        String uriString = "google.navigation:q=" + destinationLatLng.latitude + "," + destinationLatLng.longitude;
                        Uri gmmIntentUri = Uri.parse(uriString);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        } else {
                            Toast.makeText(HomeActivity.this, "Aucune application de cartographie n'est installée.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });







            }).addOnFailureListener((exception) -> {
                if (exception instanceof ApiException) {
                    ApiException apiException = (ApiException) exception;
                    Log.e("Places", "Place not found: " + apiException.getStatusCode());
                }
            });
        } else {
            // La permission n'est pas accordée, demandons-la à l'utilisateur
            requestLocationPermission();
        }
    }




    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestNearbyPlaces();
            } else {
                Toast.makeText(HomeActivity.this, "No place to show", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void logout(){
        AuthUI.getInstance()
                .signOut(HomeActivity.this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(HomeActivity.this, "You Signed Out !", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(HomeActivity.this, MainActivity.class);
                        startActivity(i);
                        finish();
                    }
                });
    }

}