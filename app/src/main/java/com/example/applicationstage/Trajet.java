package com.example.applicationstage;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trajet {
    private String heure;
    private String utilisateurId;
    private List<Map<String, Object>> points;

    public Trajet() {
        heure ="";
        utilisateurId ="";
        points = new ArrayList<>();
    }

    public String getHeure() {
        return heure;
    }

    public void setHeure(String heure) {
        this.heure = heure;
    }

    public String getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(String utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public List<Map<String, Object>> getPoints() {
        return points;
    }

    public void addPoint(LatLng point, long timestamp) {
        Map<String, Object> pointData = new HashMap<>();
        pointData.put("latitude", point.latitude);
        pointData.put("longitude", point.longitude);
        pointData.put("timestamp", timestamp);
        points.add(pointData);
    }
}