//Main Function .INO FILE

void blinkingConnectionReached(MillisTimer &mt) {
  if (digitalRead(connectionLed) == HIGH) {
    digitalWrite(connectionLed, LOW);
  } else {
    digitalWrite(connectionLed, HIGH);
  }
  connectionBlinkTimmer.reset();
  connectionBlinkTimmer.start();

}

void onDisconnected(const WiFiEventStationModeDisconnected& event)
{
  //  DBG_OUTPUT_PORT.println("Disconnected!!!");
  //  DBG_OUTPUT_PORT.println("Restarting ESP");
  //  while (1)ESP.restart();
  //  delay(500);
}

void startConnectionSettings() {
  resetAfterBath = false;
  mDisconnectHandler = WiFi.onStationModeDisconnected(&onDisconnected);
  connectionBlinkTimmer.setInterval(500);
  connectionBlinkTimmer.expiredHandler(blinkingConnectionReached);
  connectionBlinkTimmer.start();

}

void configureServer() {

  server.on ( "/check", check);
  server.begin();
  DBG_OUTPUT_PORT.println("HTTP server started");
}

void configureGPIO(void) {

  pinMode(buttonResetPin, INPUT);
  pinMode(rele, OUTPUT);
  pinMode(Led_Aviso, OUTPUT);
  pinMode(buzzer, OUTPUT);
  pinMode(connectionLed, OUTPUT);


  digitalWrite(rele, LOW);
  digitalWrite(Led_Aviso, LOW);
  digitalWrite(buzzer, LOW);
  digitalWrite(connectionLed, LOW);

  //EEPROM Start Function
  EEPROM.begin(512);

  armazenado = EEPROM.read(address_time);
  if (armazenado > 45 || armazenado < 1 ) {
    bathTime = 7;
    EEPROM.write(address_time, bathTime);
    EEPROM.commit();
  }
  else {
    bathTime = armazenado;
  }

  armazenado = EEPROM.read(address_wait);
  if (armazenado > 45 || armazenado < 1 ) {
    bathWaitTime = 5;
    EEPROM.write(address_wait, bathWaitTime);
    EEPROM.commit();
  }
  else {
    bathWaitTime = armazenado;
  }

  armazenado = EEPROM.read(address_stopped);
  if (armazenado > 45 || armazenado < 1 ) {
    bathStoppedTime = 1;
    EEPROM.write(address_stopped, bathStoppedTime);
    EEPROM.commit();
  }
  else {
    bathStoppedTime = armazenado;
  }
}


void setup_wifi(void) {
  int cnt = 0;
  espClient.setBufferSizes(512, 512);

  // set for STA mode
  WiFi.mode(WIFI_STA);
  // if wifi cannot connect start smartconfig
  while (WiFi.status() != WL_CONNECTED && WiFi.SSID() == "") {
    delay(500);
    Serial.print(".");
    if (cnt++ >= 15) {
      WiFi.beginSmartConfig();
      while (1) {
        delay(500);
        if (WiFi.smartConfigDone()) {
          Serial.println("SmartConfig Success");
          break;
        }
      }
    }
  }

  timeClient.begin();
  while (!timeClient.update()) {
    timeClient.forceUpdate();
  }

  espClient.setX509Time(timeClient.getEpochTime());
}

void load_certificates(void) {
  if (!SPIFFS.begin()) {
    DBG_OUTPUT_PORT.println("Failed to mount file system");
    return;
  }

  DBG_OUTPUT_PORT.print("Heap: "); Serial.println(ESP.getFreeHeap());

  // Load certificate file
  File cert = SPIFFS.open("/cert.der", "r"); //replace cert.crt eith your uploaded file name
  if (!cert) {
    DBG_OUTPUT_PORT.println("Failed to open cert file");
  }
  else
    DBG_OUTPUT_PORT.println("Success to open cert file");

  delay(1000);

  if (espClient.loadCertificate(cert))
    DBG_OUTPUT_PORT.println("cert loaded");
  else
    DBG_OUTPUT_PORT.println("cert not loaded");

  // Load private key file
  File private_key = SPIFFS.open("/private.der", "r"); //replace private eith your uploaded file name
  if (!private_key) {
    DBG_OUTPUT_PORT.println("Failed to open private cert file");
  }
  else
    DBG_OUTPUT_PORT.println("Success to open private cert file");

  delay(1000);

  if (espClient.loadPrivateKey(private_key))
    DBG_OUTPUT_PORT.println("private key loaded");
  else
    DBG_OUTPUT_PORT.println("private key not loaded");

  // Load CA file
  File ca = SPIFFS.open("/ca.der", "r"); //replace ca eith your uploaded file name
  if (!ca) {
    DBG_OUTPUT_PORT.println("Failed to open ca ");
  }
  else
    DBG_OUTPUT_PORT.println("Success to open ca");

  delay(1000);

  if (espClient.loadCACert(ca))
    DBG_OUTPUT_PORT.println("ca loaded");
  else
    DBG_OUTPUT_PORT.println("ca failed");

  DBG_OUTPUT_PORT.print("Heap: "); Serial.println(ESP.getFreeHeap());


}

void setup(void) {
  onSmartConfig = false;
//  WiFi.disconnect();

  DBG_OUTPUT_PORT.begin(115200);
  DBG_OUTPUT_PORT.print("\n");
  DBG_OUTPUT_PORT.setDebugOutput(true);
  configureGPIO();
  configureServer();
  initBathConfiguration();
  startConnectionSettings();
  setup_wifi();
  delay(3000);
  load_certificates();


  DBG_OUTPUT_PORT.println("");
  DBG_OUTPUT_PORT.println("Starting AWS Services and connection");
  DBG_OUTPUT_PORT.println(WiFi.SSID());
  WiFi.printDiag(DBG_OUTPUT_PORT);
}

void loop(void) {

  bathProcess();
  
  if (WiFi.status() != WL_CONNECTED) {
    connectionBlinkTimmer.run();
  } else {
    server.handleClient();
    digitalWrite(connectionLed, LOW);
    connectionBlinkTimmer.reset();
    connectionBlinkTimmer.stop();

    if (!client.connected()) {
      reconnect();
    }
  }

  client.loop();

  // check if the pushbutton is pressed.
  // if it is, the buttonState is HIGH:
  //  //  // Reset Wifi button
  //  buttonResetState = digitalRead(buttonResetPin);
  //  if (buttonResetState == HIGH) {
  //    // Reset Wifi
  //    WiFi.disconnect();
  //    delay(1000);
  //    while (1)ESP.restart();
  //
  //  }
}
