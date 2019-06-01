//BathLogic FUNCTIONS .INO FILE

void computeBathStatistics(boolean bathReached) {
  double duration = 0;
  double litters = 0;

  if (bathReached) {
    duration = bathTime;
  } else {
    duration = ceil(((bathTime * 1000 * 60) - bathRemainingTime) / 60000);
  }

  litters = (duration / 60) * l_hour;
  updateBathStatistics(duration, litters);
  l_hour = 0;
}

void bathWaitTimerReached(MillisTimer &mt) {

  waiting = false;
  stopPressed = false;
  digitalWrite(rele, LOW);

  // TODO - Compute the bath statistics
  DBG_OUTPUT_PORT.println("Bath waiting time reached! Now shower is enabled!");
  flow_frequency = 0;

  //Reseting the timers
  bathDurationTimer.reset();
  bathStopTimer.reset();
  bathWaitingTimer.reset();

  // Setting the interval again to ensure that it will be available for a new bath with the correct time
  bathDurationTimer.setInterval(60 * 1000 * bathTime);
  bathWaitingTimer.setInterval(60 * 1000 * bathWaitTime);
  bathStopTimer.setInterval(60 * 1000 * bathStoppedTime);

  if (resetAfterBath) {
    DBG_OUTPUT_PORT.println("Reseting");
    while (1)ESP.restart();
  }
}

void bathTimeReached(MillisTimer &mt) {
  computeBathStatistics(true);
  //The bathTime was reached, turnoff the shower
  bathRunning = false;
  waiting = true;
  showerIsOn = false;

  bathBlinkTimmer.stop();
  bathBlinkTimmer.reset();
  digitalWrite(rele, LOW);
  digitalWrite(Led_Aviso, LOW);

  bathWaitingTimer.setInterval(60 * 1000 * bathWaitTime);
  bathWaitingTimer.expiredHandler(bathWaitTimerReached);
  bathWaitingTimer.start();
  DBG_OUTPUT_PORT.println("Bath time reached! Triggering the wait time of: " + (String)bathWaitTime);

}

void bathStoppedTimerReached(MillisTimer &mt) {
  computeBathStatistics(false);
  bathRunning = false;
  waiting = true;
  showerIsOn = false;

  bathBlinkTimmer.stop();
  bathBlinkTimmer.reset();
  digitalWrite(rele, LOW);
  digitalWrite(Led_Aviso, LOW);

  //TODO - Start wait time decreasing the stopped time reached
  bathWaitingTimer.setInterval(60 * 1000 * bathWaitTime);
  bathWaitingTimer.expiredHandler(bathWaitTimerReached);
  bathWaitingTimer.start();
  DBG_OUTPUT_PORT.println("Bath stop reached! Triggering the wait time of: " + (String)bathWaitTime);

}

void bathBlinkTimerReached(MillisTimer &mt) {
  if (digitalRead(Led_Aviso) == HIGH) {
    digitalWrite(Led_Aviso, LOW);
  } else {
    digitalWrite(Led_Aviso, HIGH);
  }
  bathBlinkTimmer.reset();
  bathBlinkTimmer.start();
}

void buzzerTimerReached(MillisTimer &mt) {
  digitalWrite(buzzer, LOW);
  bathBuzzerTimmer.reset();
  bathBuzzerTimmer.stop();

}

void checkRemainingTimeForBuzzer(boolean *buzzerEnabled) {
  if ((bathDurationTimer.getRemainingTime() < 30000) && (*buzzerEnabled == true)) {
    DBG_OUTPUT_PORT.println(bathDurationTimer.getRemainingTime());
    DBG_OUTPUT_PORT.println(bathDurationTimer.isRunning());
    bathBuzzerTimmer.setInterval(3000);
    bathBuzzerTimmer.expiredHandler(buzzerTimerReached);
    bathBuzzerTimmer.start();
    digitalWrite(buzzer, HIGH);
    *buzzerEnabled = false;
  }
  bathBuzzerTimmer.run();
}

void startBath() {
  bathWaitTime = EEPROM.read(address_wait);
  bathTime = EEPROM.read(address_time);
  bathStoppedTime = EEPROM.read(address_stopped);

  DBG_OUTPUT_PORT.println("Initializing a new bath with max time: " + (String)bathTime);
  // TODO - Decrease with the false alarm time
  bathDurationTimer.setInterval(60 * 1000 * bathTime);
  bathDurationTimer.expiredHandler(bathTimeReached);
  bathDurationTimer.start();
  showerIsOn = true;

  DBG_OUTPUT_PORT.println("The bath stopped time is default set as 1 minutes");
  // TODO - CHANGE TO 60 * 1000 and to variable address_espera
  bathStopTimer.setInterval(bathStoppedTime * 1000 * 60);
  bathStopTimer.expiredHandler(bathStoppedTimerReached);
  // commented, because the time will go out even if the bath is running
  //bathStopTimer.start();
  bathBlinkTimmer.setInterval(500);
  bathBlinkTimmer.expiredHandler(bathBlinkTimerReached);

  bathRunning = true;
  digitalWrite(rele, HIGH);
  digitalWrite(Led_Aviso, HIGH);
  bathDurationBuzzer = true;
}


void buttonPressed () // Interrupt function
{
  if (!onSmartConfig) {
    unsigned long interrupt_time = millis();
    // If interrupts come faster than 200ms, assume it's a bounce and ignore
    if (interrupt_time - last_interrupt_time > 300)
    {
      DBG_OUTPUT_PORT.println("Button pressed");
      if (bathRunning) {
        if (stopPressed) {
          stopPressed = false;
        } else {
          stopPressed = true;
        }
      } else if (waiting == false && bathRunning == false) {
        startBath();
      }
    }
    last_interrupt_time = interrupt_time;
  }
}

void initBathConfiguration() {
  pinMode(BATH_BUTTON_PIN, INPUT);
  DBG_OUTPUT_PORT.println("Bath configured");
  attachInterrupt(BATH_BUTTON_PIN, buttonPressed, RISING);
  sei(); // Enable interrupts
  waiting = false;
  bathRunning = false;
  showerIsOn = false;
  stopPressed = false;
  flow_frequency = 0;
  last_interrupt_time = 0;
  l_hour = 0;
  totalFlowFrequency = 0;

  bathDurationBuzzer = false;
}

void bathProcess ()
{
  if (waiting != true) {
    if (bathRunning) {
      if (stopPressed == false) {
        if (!showerIsOn) {
          bathBlinkTimmer.stop();
          digitalWrite(rele, HIGH);
          digitalWrite(Led_Aviso, HIGH);
          bathDurationTimer.start();
          DBG_OUTPUT_PORT.println("bath is running");
          stopRemainingTime = bathStopTimer.getRemainingTime();
          bathStopTimer.reset();
          bathStopTimer.setInterval(stopRemainingTime);
        }
        showerIsOn = true;
        bathDurationTimer.run();
        checkRemainingTimeForBuzzer(&bathDurationBuzzer);

      } else if (stopPressed == true) {
        if (showerIsOn) {
          bathBlinkTimmer.start();
          digitalWrite(rele, LOW);
          digitalWrite(buzzer, LOW);
          bathStopTimer.start();
          DBG_OUTPUT_PORT.println("bath is stopped");
          bathRemainingTime = bathDurationTimer.getRemainingTime();
          bathDurationTimer.reset();
          bathDurationTimer.setInterval(bathRemainingTime);
        }
        showerIsOn = false;
        bathStopTimer.run();
        bathBlinkTimmer.run();
      }
    }
    // TODO - Implement the alert LED that the bath is finishing
  } else {
    bathWaitingTimer.run();
  }
}
