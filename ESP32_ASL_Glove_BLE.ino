#include <Wire.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// BLE UUIDs - THESE MUST MATCH YOUR ANDROID APP
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define FLEX_CHAR_UUID      "beb5483e-36e1-4688-b7f5-ea07361b26a8"
#define LETTER_CHAR_UUID    "beb5483e-36e1-4688-b7f5-ea07361b26a9"
#define BATTERY_CHAR_UUID   "beb5483e-36e1-4688-b7f5-ea07361b26aa"

// FLEX SENSOR SETUP
#define THUMB_PIN  25
#define INDEX_PIN  27
#define MIDDLE_PIN 14
#define RING_PIN   12
#define PINKY_PIN  26

const int flexPins[5] = {THUMB_PIN, INDEX_PIN, MIDDLE_PIN, RING_PIN, PINKY_PIN};
const char* fingerNames[5] = {"Thumb", "Index", "Middle", "Ring", "Pinky"};

// Final calibrated values from your glove
const int flexMin[5] = {847, 2510, 1206, 2625, 949};   // Straight
const int flexMax[5] = {3056, 4095, 2412, 3611, 2304}; // Bent

// LSM6DS3 IMU SETUP
#define LSM6DS3_ADDR 0x6A
#define CTRL1_XL     0x10
#define CTRL2_G      0x11
#define CTRL3_C      0x12
#define OUTX_L_XL    0x28

// BLE Variables
BLEServer* pServer = NULL;
BLECharacteristic* pFlexCharacteristic = NULL;
BLECharacteristic* pLetterCharacteristic = NULL;
BLECharacteristic* pBatteryCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint8_t batteryLevel = 75; // Mock battery level for demo

// Gesture detection variables
float accelX = 0, accelY = 0;
char detectedLetter = '?';
float angles[5] = {0};
int rawValues[5] = {0};

// BLE Callbacks
class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    Serial.println("Device connected");
  }

  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    Serial.println("Device disconnected");
    // Restart advertising
    pServer->startAdvertising();
  }
};

// Characteristic callbacks for handling writes from the app
class MyCallbacks: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    // Get the raw data as a pointer to uint8_t
    uint8_t* dataPtr = pCharacteristic->getData();
    size_t length = pCharacteristic->getValue().length();
    
    if (length > 0) {
      Serial.print("Received: ");
      for (int i = 0; i < length; i++) {
        Serial.print(dataPtr[i], HEX);
        Serial.print(" ");
      }
      Serial.println();
      
      // Check if calibration command (0xC5)
      if (dataPtr[0] == 0xC5) {
        Serial.println(" Calibration command received");
        // Here you could implement calibration if needed
      }
    }
  }
};

void writeRegister(uint8_t reg, uint8_t val) {
  Wire.beginTransmission(LSM6DS3_ADDR);
  Wire.write(reg); Wire.write(val);
  Wire.endTransmission();
}

int16_t read16(uint8_t reg) {
  Wire.beginTransmission(LSM6DS3_ADDR);
  Wire.write(reg);
  Wire.endTransmission(false);
  Wire.requestFrom(LSM6DS3_ADDR, 2);
  return Wire.read() | (Wire.read() << 8);
}

void setup() {
  Serial.begin(115200);
  Wire.begin(23, 22); // SDA, SCL
  Serial.println(" ASL Glove with BLE Initialized");

  // IMU Setup
  Wire.beginTransmission(LSM6DS3_ADDR);
  if (Wire.endTransmission() != 0) {
    Serial.println(" IMU not detected!");
  } else {
    Serial.println(" LSM6DS3 Connected");
    writeRegister(CTRL1_XL, 0b01010000); // Accelerometer
    writeRegister(CTRL2_G,  0b01000000); // Gyroscope
    writeRegister(CTRL3_C,  0b00000100); // Auto increment
  }

  // BLE Setup
  BLEDevice::init("ASL Glove");
  
  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  
  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create the flex sensor characteristic (NOTIFY)
  pFlexCharacteristic = pService->createCharacteristic(
      FLEX_CHAR_UUID,
      BLECharacteristic::PROPERTY_NOTIFY
  );
  pFlexCharacteristic->addDescriptor(new BLE2902());

  // Create the battery level characteristic (NOTIFY)
  pBatteryCharacteristic = pService->createCharacteristic(
      BATTERY_CHAR_UUID,
      BLECharacteristic::PROPERTY_NOTIFY
  );
  pBatteryCharacteristic->addDescriptor(new BLE2902());

  // Create the predicted letter characteristic (NOTIFY)
  pLetterCharacteristic = pService->createCharacteristic(
      LETTER_CHAR_UUID,
      BLECharacteristic::PROPERTY_NOTIFY | BLECharacteristic::PROPERTY_READ
  );
  pLetterCharacteristic->addDescriptor(new BLE2902());
  
  // Initialize with a known letter to ensure it can be read
  uint8_t initialLetter[1] = {'X'};  // Initialize with 'X' for testing
  pLetterCharacteristic->setValue(initialLetter, 1);

  // Start the service
  pService->start();
  
  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);  // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  
  Serial.println(" BLE advertising started, ready for connections");
}

void loop() {
  // Read flex sensors
  for (int i = 0; i < 5; i++) {
    rawValues[i] = analogRead(flexPins[i]);
    rawValues[i] = constrain(rawValues[i], flexMin[i], flexMax[i]);
    angles[i] = map(rawValues[i], flexMin[i], flexMax[i], 0, 90);
  }

  // Read accelerometer
  int16_t axRaw = read16(OUTX_L_XL);
  int16_t ayRaw = read16(OUTX_L_XL + 2);
  accelX = axRaw * 4.0 / 32768.0;
  accelY = ayRaw * 4.0 / 32768.0;

  // Detect letter
  detectedLetter = detectASL(angles, accelX, accelY);

  // Print to serial (for debugging)
  printDebugInfo();

  // Send data over BLE if connected
  if (deviceConnected) {
    // Create a data packet that includes both flex values and accelerometer data
    // 10 bytes for flex sensors (2 bytes per sensor) + 8 bytes for accelerometer (4 bytes per float)
    uint8_t combinedData[18];
    
    // First add the flex sensor data
    for (int i = 0; i < 5; i++) {
      combinedData[i*2] = rawValues[i] & 0xFF;          // Low byte
      combinedData[i*2+1] = (rawValues[i] >> 8) & 0xFF; // High byte
    }
    
    // Then add the accelerometer data (converting floats to bytes)
    // X accelerometer (4 bytes)
    byte* accelXBytes = (byte*)&accelX;
    for (int i = 0; i < 4; i++) {
      combinedData[10+i] = accelXBytes[i];
    }
    
    // Y accelerometer (4 bytes)
    byte* accelYBytes = (byte*)&accelY;
    for (int i = 0; i < 4; i++) {
      combinedData[14+i] = accelYBytes[i];
    }
    
    // Send the combined data packet
    pFlexCharacteristic->setValue(combinedData, 18);
    pFlexCharacteristic->notify();
    
    // Send letter separately with explicit handling
    // Always send the letter, even if it hasn't changed - this ensures the app receives it
    uint8_t letterData[1] = {(uint8_t)detectedLetter};
    pLetterCharacteristic->setValue(letterData, 1);
    bool notifySuccess = pLetterCharacteristic->notify();
    
    // Debug detailed info about the letter being sent
    Serial.print("Sent letter notification: '");
    Serial.print(detectedLetter);
    Serial.print("' ASCII value: ");
    Serial.print((int)detectedLetter);
    Serial.print(", Hex: ");
    Serial.print(detectedLetter, HEX);
    Serial.print(", Notify success: ");
    Serial.println(notifySuccess ? "SUCCESS" : "FAILED");
    
    // Force a small delay after sending the letter to avoid BLE stack issues
    delay(10);
    
    // Send battery (mock value for now, decreasing slowly)
    batteryLevel = (batteryLevel > 1) ? batteryLevel - 1 : 100;
    uint8_t batteryData[1] = {batteryLevel};
    pBatteryCharacteristic->setValue(batteryData, 1);
    pBatteryCharacteristic->notify();
    
    Serial.println("Sent all notifications");
  }
  
  // Disconnection handling
  if (!deviceConnected && oldDeviceConnected) {
    delay(500); // Give the Bluetooth stack time to get ready
    pServer->startAdvertising(); // Restart advertising
    Serial.println(" Restarting advertising");
    oldDeviceConnected = deviceConnected;
  }
  
  // Connection handling
  if (deviceConnected && !oldDeviceConnected) {
    // Do stuff on connect
    oldDeviceConnected = deviceConnected;
  }
  
  delay(300); // Update interval
}

void printDebugInfo() {
  Serial.println(" Flex Angles:");
  for (int i = 0; i < 5; i++) {
    Serial.print(fingerNames[i]);
    Serial.print(": Raw="); Serial.print(rawValues[i]);
    Serial.print(", Angle="); Serial.print(angles[i]); Serial.print("°\t");
  }
  Serial.println();
  
  Serial.print("Accel X: "); Serial.print(accelX);
  Serial.print(" Y: "); Serial.println(accelY);
  
  Serial.print(" Detected Letter: "); Serial.println(detectedLetter);
  printLetterDescription(detectedLetter);
  Serial.println("Angle debug:");
  for (int i = 0; i < 5; i++) {
    Serial.print("Finger "); Serial.print(i); 
    Serial.print(": "); Serial.println(angles[i]);
  }
  Serial.println("--------------------------------------------------");
}

bool inRange(float val, float low, float high) {
  return val >= low && val <= high;
}

char detectASL(float a[], float ax, float ay) {
  if (a[0] <= 30 && a[1] >= 60 && a[2] >= 60 && a[3] >= 60 && a[4] >= 60) return 'A';
  if (a[0] >= 65 && a[1] <= 30 && a[2] <= 15 && a[3] <= 15 && a[4] <= 15) return 'B';

  if (inRange(a[0], 30, 60) && inRange(a[1], 50, 80) && inRange(a[2], 40, 80) &&
     inRange(a[3], 40, 80) && inRange(a[4], 40, 80)) return 'C';

  if (
    a[1] <= 30 && a[2] >= 40 && a[3] >= 35 && a[4] >= 35 && 
    abs(ax) < 1.2 && abs(ay) < 1.2
  ) return 'D';

  if (a[0] >= 65 && a[1] >= 65 && a[2] >= 65 && a[3] >= 55 && a[4] >= 65) return 'E';
  if (a[0] <= 30 && a[1] >= 50 && a[2] <= 20 && a[3] <= 20 && a[4] <= 20) return 'F';
  if (a[0] <= 30 && a[1] <= 20 && a[2] >= 55 && a[3] >= 55 && a[4] >= 60) return 'G';
  if (a[0] >= 40 && a[1] <= 20 && a[2] <= 20 && a[3] >= 55 && a[4] >= 50) return 'H';
  if (a[0] >= 30 && a[1] >= 70 && a[2] >= 55 && a[3] >= 55 && a[4] <= 30 && ay > 0.8) return 'I';

  if (
    a[0] >= 30 && a[1] >= 60 && a[2] >= 50 && a[3] >= 50 && a[4] <= 30 &&  // J pose (like I)
    ay < -1.5                                                              // Downward swipe
  ) return 'J';

  if (a[0] <= 30 && a[1] <= 20 && a[2] <= 20 && a[3] >= 55 && a[4] >= 55) return 'K';
  if (a[0] <= 10 && a[1] <= 18 && a[2] >= 40 && a[3] >= 40 && a[4] >= 40) return 'L';
  if (inRange(a[0],30,75) && inRange(a[1],40,80) && inRange(a[2],40,80) && inRange(a[3],50,85) && a[4] >= 85) return 'M';
  if (inRange(a[0],30,70) && inRange(a[1],40,80) && inRange(a[2],40,80) && a[3] > 85 && a[4] >= 85) return 'N';
  if (a[0] >= 20 && inRange(a[1],30,80) && inRange(a[2],30,85) && inRange(a[3],30,85) && inRange(a[4],30,85) && ay > 0.8) return 'O';
  if (a[0] <= 40 && a[1] <= 15 && a[2] <= 30 && a[3] >= 55 && a[4] >= 55) return 'P';
  if (a[0] <= 15 && a[1] <= 15 && a[2] >= 50 && a[3] >= 50 && a[4] >= 60) return 'Q';
  if (a[0] >= 30 && a[1] <= 10 && inRange(a[2],10,20) && a[3] >= 55 && a[4] >= 55 && ay > 0.7) return 'R';
  if (inRange(a[0],60,85) && a[1] >= 70 && a[2] >= 70 && a[3] >= 70 && a[4] >= 70) return 'S';
  if (inRange(a[0],10,40) && inRange(a[1],20,50) && a[2] >= 80 && a[3] >= 80 && a[4] >= 80) return 'T';
  if (a[0] >= 30 && a[1] <= 10 && a[2] < 10 && a[3] >= 55 && a[4] >= 55) return 'U';
  if (a[0] >= 30 && a[1] <= 10 && a[2] < 10 && a[3] >= 55 && a[4] >= 55) return 'V';
  if (a[0] >= 40 && a[1] <= 10 && a[2] <= 15 && a[3] <= 15 && a[4] >= 55) return 'W';
  if (a[0] >= 30 && inRange(a[1],15,40) && a[2] >= 30 && a[3] >= 30 && a[4] >= 30 && ay > 0.5) return 'X';
  if (a[0] <= 10 && a[1] >= 44 && a[2] >= 40 && a[3] >= 40 && a[4] <= 15) return 'Y';

  if (
    a[1] <= 30 && a[2] >= 40 && a[3] >= 35 && a[4] >= 35 &&  // D pose
    abs(ax) > 1.7                                            // Lowered motion sensitivity
  ) return 'Z';

  return '?';
}

void printLetterDescription(char letter) {
  if (letter == '?') {
    Serial.println(" Description: Unknown or unclear gesture.");
    return;
  }

  Serial.print(" Description: ");
  switch (letter) {
    case 'A': Serial.println("All fingers fully bent (custom)"); break;
    case 'B': Serial.println("Fingers STRAIGHT, thumb across palm"); break;
    case 'C': Serial.println("All fingers curved to form a 'C'"); break;
    case 'D': Serial.println("Index STRAIGHT, rest BENT"); break;
    case 'E': Serial.println("Fingers curled into palm"); break;
    case 'F': Serial.println("Thumb & index form O, rest up"); break;
    case 'G': Serial.println("Thumb & index out sideways"); break;
    case 'H': Serial.println("Index and middle STRAIGHT, rest BENT"); break;
    case 'I': Serial.println("Only pinky STRAIGHT"); break;
    case 'J': Serial.println("Draw J with pinky"); break;
    case 'K': Serial.println("Index & middle up, thumb to middle"); break;
    case 'L': Serial.println("Index & thumb form L"); break;
    case 'M': Serial.println("Thumb under 3 bent fingers"); break;
    case 'N': Serial.println("Thumb under 2 bent fingers"); break;
    case 'O': Serial.println("Fingers form O shape"); break;
    case 'P': Serial.println("K shape tilted down"); break;
    case 'Q': Serial.println("G shape pointing down"); break;
    case 'R': Serial.println("Index and middle crossed"); break;
    case 'S': Serial.println("Fist, thumb over fingers"); break;
    case 'T': Serial.println("Thumb between index & middle"); break;
    case 'U': Serial.println("Index & middle up together"); break;
    case 'V': Serial.println("Peace sign"); break;
    case 'W': Serial.println("3 fingers up"); break;
    case 'X': Serial.println("Index hooked"); break;
    case 'Y': Serial.println("Thumb and pinky out (shaka)"); break;
    case 'Z': Serial.println("Draw Z in air with index"); break;
    default:  Serial.println("Unknown pose."); break;
  }
}
