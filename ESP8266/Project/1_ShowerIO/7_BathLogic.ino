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
  bathDurationTimer.setInterval((10 * 1000));
  bathWaitingTimer.setInterval(10 * 1000);
  bathStopTimer.setInterval(20 * 1000);
}

void bathTimeReached(MillisTimer &mt) {
  //computeBathStatistics(true);
  //The bathTime was reached, turnoff the shower
  bathRunning = false;
  waiting = true;
  digitalWrite(rele, LOW);
  showerIsOn = false;

  bathWaitingTimer.setInterval(10 * 1000);
  bathWaitingTimer.expiredHandler(bathWaitTimerReached);
  bathWaitingTimer.start();
  DBG_OUTPUT_PORT.println("Bath time reached! Triggering the wait time of: " + (String)bathWaitTime);

}

void bathStoppedTimerReached(MillisTimer &mt) {
  //computeBathStatistics(false);
  bathRunning = false;
  waiting = true;
  digitalWrite(rele, LOW);
  showerIsOn = false;
  //TODO - Start wait time decreasing the stopped time reached
  bathWaitingTimer.setInterval(10 * 1000);
  bathWaitingTimer.expiredHandler(bathWaitTimerReached);
  bathWaitingTimer.start();
  DBG_OUTPUT_PORT.println("Bath stop reached! Triggering the wait time of: " + (String)bathWaitTime);

}

void startBath() {
  DBG_OUTPUT_PORT.println("Initializing a new bath with max time: " + (String)bathTime);
  // TODO - Decrease with the false alarm time
  bathDurationTimer.setInterval(10 * 1000);
  bathDurationTimer.expiredHandler(bathTimeReached);
  bathDurationTimer.start();
  showerIsOn = true;

  DBG_OUTPUT_PORT.println("The bath stopped time is default set as 1 minutes");
  // TODO - CHANGE TO 60 * 1000 and to variable address_espera
  bathStopTimer.setInterval(20 * 1000);
  bathStopTimer.expiredHandler(bathStoppedTimerReached);
  // commented, because the time will go out even if the bath is running
  //bathStopTimer.start();
  bathRunning = true;
  digitalWrite(rele, HIGH);
}


void buttonPressed () // Interrupt function
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

void initBathConfiguration() {
  pinMode(BATH_BUTTON_PIN, INPUT);
  DBG_OUTPUT_PORT.println("BATH CONFIGURED");
  attachInterrupt(BATH_BUTTON_PIN, buttonPressed, RISING);
  sei(); // Enable interrupts
  waiting = false;
  bathRunning = false;
  showerIsOn = false;
  stopPressed = false;
  flow_frequency = 0;
  l_hour = 0;
  flowLastValue = 0;
  totalFlowFrequency = 0;
}

void bathProcess ()
{
  if (waiting != true) {
    if (bathRunning) {
      if (stopPressed == false) {
        if (!showerIsOn) {
          digitalWrite(rele, HIGH);
          bathDurationTimer.start();
          DBG_OUTPUT_PORT.println("bath is running");
          stopRemainingTime = bathStopTimer.getRemainingTime();
          bathStopTimer.reset();
          bathStopTimer.setInterval(stopRemainingTime);
        }
        showerIsOn = true;
        bathDurationTimer.run();

      } else if (stopPressed == true) {
        if (showerIsOn) {
          digitalWrite(rele, LOW);
          bathStopTimer.start();
          DBG_OUTPUT_PORT.println("bath is stopped");
          bathRemainingTime = bathDurationTimer.getRemainingTime();
          bathDurationTimer.reset();
          bathDurationTimer.setInterval(bathRemainingTime);
        }
        showerIsOn = false;
        bathStopTimer.run();
      }
    }
    // TODO - Implement the alert LED that the bath is finishing
  } else {
    bathWaitingTimer.run();
  }
}
