//Main Function .INO FILE


void connectToAWSandServer() {
  DBG_OUTPUT_PORT.println("");
  DBG_OUTPUT_PORT.println("Starting AWS Services and connection");

  //fill AWS parameters
  awsWSclient.setAWSRegion(aws_region);
  awsWSclient.setAWSDomain(aws_endpoint);
  awsWSclient.setAWSKeyID(aws_key);
  awsWSclient.setAWSSecretKey(aws_secret);
  awsWSclient.setUseSSL(true);

  if (connect ()) {
    subscribe ();
  }

}

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
  DBG_OUTPUT_PORT.println("Disconnected!!!");
  connectionBlinkTimmer.start();
}

void connectTimmerReached(MillisTimer &mt) {
  DBG_OUTPUT_PORT.println("Connect timmer reached");
  connectTimmer.reset();
  connectTimmer.stop();
  if (bathRunning) {
    DBG_OUTPUT_PORT.println("A bath is running, reseting after bath");
    resetAfterBath = true;
  } else {
    DBG_OUTPUT_PORT.println("Reseting");
    while (1)ESP.restart();
  }
}

void startConnectionSettings() {
  resetAfterBath = false;
  connectionState = "DISCONECTED";
  WiFi.mode(WIFI_STA);
  mDisconnectHandler = WiFi.onStationModeDisconnected(&onDisconnected);
  if (WiFi.SSID() != NULL) {
    connectionState = "RECONNECTING";
    DBG_OUTPUT_PORT.println(connectionState);
    connectTimmer.setInterval(30000);
    connectTimmer.expiredHandler(connectTimmerReached);
  } else {
    connectionState = "CONNECTING";
    DBG_OUTPUT_PORT.println(connectionState);
    WiFi.beginSmartConfig();
  }

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


void setup(void) {
  //WiFi.disconnect();

  //   deplay for 2 sec for smartConfig
  //  Serial.println("2 sec before clear SmartConfig");
  //  delay(2000);

  DBG_OUTPUT_PORT.begin(115200);
  DBG_OUTPUT_PORT.print("\n");
  DBG_OUTPUT_PORT.setDebugOutput(true);
  configureGPIO();
  configureServer();
  initBathConfiguration();
  startConnectionSettings();

  // if wifi cannot connect start smartconfig
  //  while (WiFi.status() != WL_CONNECTED) {
  //    delay(500);
  //    DBG_OUTPUT_PORT.print(".");
  //    if (cnt++ >= 15) {
  //      WiFi.beginSmartConfig();
  //      while (1) {
  //        delay(500);
  //        if (WiFi.smartConfigDone()) {
  //          DBG_OUTPUT_PORT.println("SmartConfig Success");
  //          break;
  //        }
  //      }
  //    }
  //  }
  //
}

void loop(void) {
  server.handleClient();
  bathProcess();

  if (WiFi.status() != WL_CONNECTED) {
    connectionBlinkTimmer.run();
  } else {
    digitalWrite(connectionLed, LOW);
    connectionBlinkTimmer.reset();
    connectionBlinkTimmer.stop();
  }

  // check if the pushbutton is pressed.
  // if it is, the buttonState is HIGH:

  //  if (WiFi.status() == WL_CONNECTED) {
  //    if (awsWSclient.connected ()) {
  //      client.loop ();
  //    } else {
  //      //handle reconnection
  //      if (connect ()) {
  //        subscribe ();
  //      }
  //    }
  //  }
   // Reset Wifi button
    buttonResetState = digitalRead(buttonResetPin);
     if (buttonResetState == HIGH) {
      // Reset Wifi
      WiFi.disconnect();
      delay(1000);
       while (1)ESP.restart();
     
    }

}
