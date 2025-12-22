// ESP32 Smart Doorbell with Firebase & Real-Time Tracking
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <time.h>

// ========== Configuration ==========
// Wi-Fi
#define WIFI_SSID "Wifi SSID" // Wifi SSID sample
#define WIFI_PASSWORD " Wifi Password" // Wifi Password sample

// Firebase
#define FIREBASE_HOST "FIREBASE_HOST"
#define FIREBASE_AUTH "FIREBASE_AUTH"

// Device
#define DEVICE_ID "DOORBELL_001" // Sample of a device id
#define AUTH_KEY "12345" // Sample of a auth key


// Time (Philippines GMT+8)
#define NTP_SERVER "pool.ntp.org"
#define GMT_OFFSET_SEC 28800  // 8 * 3600
#define DAYLIGHT_OFFSET_SEC 0

// Hardware Pins
#define BUTTON_PIN 15  // D15
#define BUZZER_PIN 18  // D18

// ========== Global Objects ==========
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;
bool buttonWasPressed = false;
unsigned long lastHeartbeat = 0;
const unsigned long HEARTBEAT_INTERVAL = 5000; // 5 seconds for faster, more accurate status

// ========== Setup ==========
void setup() {
  Serial.begin(115200);
  Serial.println("\n=== ESP32 Doorbell System ===\n");
  
  setupPins();
  setupWiFi();
  setupTime();
  setupFirebase();
  registerDevice();  // Register device with auth key
  
  Serial.println("\n🔔 Doorbell System Ready!");
  Serial.println("Button: D15 (GPIO 15)");
  Serial.println("Buzzer: D18 (GPIO 18)\n");
}

// ========== Main Loop ==========
void loop() {
  bool buttonPressed = !digitalRead(BUTTON_PIN);
  
  if (buttonPressed && !buttonWasPressed) {
    playDoorbellSound();
    
    if (WiFi.status() == WL_CONNECTED) {
      sendDoorbellPress();
    } else {
      Serial.println("⚠️ No WiFi - Event not logged");
    }
  }
  
  buttonWasPressed = buttonPressed;
  
  // Send heartbeat to Firebase every 10 seconds
  if (WiFi.status() == WL_CONNECTED && (millis() - lastHeartbeat >= HEARTBEAT_INTERVAL)) {
    updateDeviceStatus();
    lastHeartbeat = millis();
  }
  
  delay(50);
}

// ========== Hardware Setup ==========
void setupPins() {
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
}

// ========== WiFi Setup ==========
void setupWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println(" ✅");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println(" ❌");
    Serial.println("WiFi failed - operating in offline mode");
  }
}

// ========== Time Setup ==========
void setupTime() {
  if (WiFi.status() != WL_CONNECTED) return;
  
  configTime(GMT_OFFSET_SEC, DAYLIGHT_OFFSET_SEC, NTP_SERVER);
  Serial.print("Syncing time");
  
  int attempts = 0;
  while (time(nullptr) < 1000000000 && attempts < 10) {
    delay(1000);
    Serial.print(".");
    attempts++;
  }
  
  if (time(nullptr) > 1000000000) {
    Serial.println(" ✅");
  } else {
    Serial.println(" ❌");
    Serial.println("Time sync failed");
  }
}

// ========== Firebase Setup ==========
void setupFirebase() {
  if (WiFi.status() != WL_CONNECTED) return;
  
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  
  Firebase.reconnectNetwork(true);
  fbdo.setBSSLBufferSize(2048, 512);
  fbdo.setResponseSize(1024);
  
  Firebase.begin(&config, &auth);
  Firebase.setReadTimeout(fbdo, 30000);
  Firebase.setwriteSizeLimit(fbdo, "tiny");
  
  Serial.println("Firebase initialized ✅");
}

// ========== Device Registration ==========
void registerDevice() {
  if (WiFi.status() != WL_CONNECTED) return;
  
  // Store auth_key at device level
  String authPath = "/devices/" + String(DEVICE_ID) + "/auth_key";
  if (Firebase.setString(fbdo, authPath, String(AUTH_KEY))) {
    Serial.println("📝 Auth key stored at device level");
  } else {
    Serial.println("⚠️ Auth key storage failed");
  }
  
  // Store other device info
  updateDeviceStatus();
}

// ========== Update Device Status (Heartbeat) ==========
void updateDeviceStatus() {
  if (WiFi.status() != WL_CONNECTED) return;
  
  String infoPath = "/devices/" + String(DEVICE_ID) + "/info";
  FirebaseJson json;
  json.set("device_id", String(DEVICE_ID));
  json.set("location", "front_door");
  json.set("type", "doorbell");
  json.set("status", "online");
  json.set("last_boot", (int)time(nullptr));
  json.set("last_seen", (int)time(nullptr));  // Heartbeat timestamp
  json.set("last_seen_millis", (unsigned long)millis());  // More precise heartbeat
  
  if (Firebase.setJSON(fbdo, infoPath, json)) {
    Serial.println("💓 Heartbeat sent - Device Active");
  } else {
    Serial.println("⚠️ Heartbeat failed");
  }
}

// ========== Firebase Send ==========
void sendDoorbellPress() {
  String path = "/devices/" + String(DEVICE_ID) + "/events/" + String(millis());
  
  // Get current time with milliseconds
  time_t now = time(nullptr);
  struct tm timeinfo;
  localtime_r(&now, &timeinfo);
  
  struct timeval tv;
  gettimeofday(&tv, NULL);
  int millisec = tv.tv_usec / 1000;
  
  // Format time string with milliseconds
  char baseTimeStr[20];
  char timeStr[30];
  char dateStr[20];
  
  strftime(baseTimeStr, sizeof(baseTimeStr), "%I:%M:%S", &timeinfo);
  snprintf(timeStr, sizeof(timeStr), "%s.%03d %s", baseTimeStr, millisec, 
           (timeinfo.tm_hour >= 12) ? "PM" : "AM");
  strftime(dateStr, sizeof(dateStr), "%Y-%m-%d", &timeinfo);
  
  // Create JSON payload
  FirebaseJson json;
  json.set("timestamp", now);
  json.set("time", String(timeStr));
  json.set("date", String(dateStr));
  json.set("status", "pressed");
  json.set("device_id", String(DEVICE_ID));
  json.set("location", "front_door");
  
  // Send with retry logic
  for (int retry = 0; retry < 3; retry++) {
    if (Firebase.setJSON(fbdo, path, json)) {
      Serial.println("🔔 Doorbell logged: " + String(timeStr));
      return;
    }
    
    if (retry < 2) {
      Serial.println("🔄 Retry " + String(retry + 1) + "/3...");
      delay(1000);
    }
  }
  
  Serial.println("❌ Failed to log event after 3 attempts");
}

// ========== Buzzer Control ==========
void playDoorbellSound() {
  Serial.println("🔊 Doorbell!");
  
  // Active buzzer - simple on/off pattern
  digitalWrite(BUZZER_PIN, HIGH);
  delay(150);
  digitalWrite(BUZZER_PIN, LOW);
  delay(100);
  
  digitalWrite(BUZZER_PIN, HIGH);
  delay(150);
  digitalWrite(BUZZER_PIN, LOW);
  delay(100);
  
  digitalWrite(BUZZER_PIN, HIGH);
  delay(300);
  digitalWrite(BUZZER_PIN, LOW);
}