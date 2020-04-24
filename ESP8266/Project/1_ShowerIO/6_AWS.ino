//generate random mqtt clientID
char* generateClientID () {
  char* cID = new char[23]();
  for (int i = 0; i < 22; i += 1)
    cID[i] = (char)random(1, 256);
  return cID;
}


void callback(char* topic, byte* payload, unsigned int length) {
  int lastPoint = 0;
  int substringTimes = 0;
  int bathTime;
  int waitTime;
  int stoppedTime;
  String message;
  char charList[20];
  String topicString = (String) topic;
  int messageLength;
  DBG_OUTPUT_PORT.print("Message arrived [");
  DBG_OUTPUT_PORT.print(topic);
  DBG_OUTPUT_PORT.print("] ");
  for (int i = 0; i < length; i++) {
    DBG_OUTPUT_PORT.print((char)payload[i]);
    message = message + (char)payload[i];
  }
  DBG_OUTPUT_PORT.println();
  if (topicString.equals(aws_topic_times)) {
    messageLength = message.length();
    message.toCharArray(charList, messageLength);
    for ( int j = 0; j < messageLength + 1; j++) {
      if (substringTimes == 2) {
        waitTime = message.substring(lastPoint, j).toInt();
      }
      if (charList[j] == *"-") {
        if (lastPoint == 0) {
          bathTime =  message.substring(lastPoint, j).toInt();
          substringTimes = 1;
        } else if (substringTimes == 1) {
          stoppedTime =  message.substring(lastPoint, j).toInt();
          substringTimes = 2;
        }
        lastPoint = j + 1;
      }
    }
    setBathTime(bathTime);
    setWaitTime(waitTime);
    setStoppedTime(stoppedTime);
  }
  if (topicString.equals(aws_topic_conf)) {
    WiFi.disconnect();
  }

  if (topicString.equals((String)ESP.getChipId() + "/check")) {
    sendResponseToAppCheck();
  }
  DBG_OUTPUT_PORT.println();
}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    DBG_OUTPUT_PORT.print("Attempting MQTT connection...");
    // Attempt to connect
    if (client.connect("ESPthing")) {
      DBG_OUTPUT_PORT.println("connected");
      subscribe ();
      getBathParams();

    } else {
      DBG_OUTPUT_PORT.print("failed, rc=");
      DBG_OUTPUT_PORT.print(client.state());
      DBG_OUTPUT_PORT.println(" try again in 5 seconds");

      char buf[256];
      espClient.getLastSSLError(buf,256);
      DBG_OUTPUT_PORT.print("WiFiClientSecure SSL error: ");
      DBG_OUTPUT_PORT.println(buf);

      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}


//subscribe to a mqtt topic
void subscribe () {
  client.setCallback(callback);
  //subscript to a topic
  client.subscribe(aws_topic_times);
  client.subscribe(aws_topic_conf);
  client.subscribe(aws_statistics_topic);
  client.subscribe(aws_topic_check);

  DBG_OUTPUT_PORT.println("MQTT subscribed");
}
