# 🚚 LafargeLog Driver Mobile Application

Android mobile application developed during my internship at **LafargeHolcim Meknes**.

This application assists truck drivers by providing nearby points of interest, GPS navigation, trip history, and real-time monitoring. It communicates with Firebase Realtime Database to send trip information and alerts, while a Laravel web application allows administrators to monitor drivers in real time.

> **Note:** This project was developed for educational and internship purposes.

---

## 📱 Features

- 📍 Display the driver's current location
- 🗺️ Show nearby places using Google Places API
- 🚗 Display navigation route to a selected destination
- 📜 Store and display trip history
- 🕒 Record trip start and end date/time
- 📡 Send driver's GPS location to Firebase Realtime Database
- 🚨 Detect prolonged stops (e.g., 30 minutes)
- 🔔 Automatically send alerts to Firebase
- 🔄 Synchronize data with the Laravel administration web application

---

## 🛠️ Technologies

- Java
- Android SDK
- Google Maps API
- Google Places API
- Google Location Services
- Firebase Realtime Database
- Firebase Authentication
- Material Design

---

## 🏗️ Architecture

```
Android Driver App
        │
        │
Firebase Realtime Database
        │
        │
Laravel Admin Dashboard
```

The Android application continuously sends trip information and alerts to Firebase.

The Laravel web application retrieves this data and allows administrators to monitor drivers, visualize their GPS positions, and receive alerts when abnormal situations occur.

---

## 📂 Main Features

- Driver authentication
- GPS tracking
- Nearby places search
- Route navigation
- Trip recording
- Trip history
- Real-time synchronization
- Automatic stop detection
- Alert management

---

## 📸 Screenshots

Screenshots will be added soon.

---

## 🚀 Getting Started

### Clone the repository

```bash
git clone https://github.com/yourusername/lafargelog-driver-mobile.git
```

### Open the project

Open the project using Android Studio.

Configure your own Firebase project and add:

```
google-services.json
```

before running the application.

---

## 🔐 Firebase

This repository does **not** include sensitive Firebase credentials.

Create your own Firebase project and configure:

- Realtime Database
- Authentication
- Google Maps API Key
- Google Places API

This repository is shared for portfolio and educational purposes.
