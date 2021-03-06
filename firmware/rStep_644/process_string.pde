enum return_status_t {
  RET_UNKNOWN,//status not stated, error state!
  RET_OK, //print OK
  RET_ERR, //print error
  RET_SILENT //no not print out anything
};

//steps per inch or mm
float _units[3];
float curve_section;

void setXYZ(FloatPoint *fp) {
  fp->x = (command_exists('X')) ? (getValue('X') + ((config.abs_mode) ? 0 : xaxis->current_units)) :   
  xaxis->current_units;
  fp->y = (command_exists('Y')) ? (getValue('Y') + ((config.abs_mode) ? 0 : yaxis->current_units)) :   
  yaxis->current_units;
  fp->z = (command_exists('Z')) ? (getValue('Z') + ((config.abs_mode) ? 0 : zaxis->current_units)) :   
  zaxis->current_units;
}

//Read the string and execute instructions
void process_string(uint8_t  *instruction) {
  uint8_t code;
  uint16_t k;
  float temp;
  //command commands = NULL;
  FloatPoint fp;
  // this can be configured for various return status
  // should be set to RET_OK if everything is fine
  enum return_status_t return_status = RET_UNKNOWN;



  //the character / means delete block... used for comments and stuff.
  if (instruction[0] == '/') 	{
    Serial.println("OK");
    // this may return silently instead of OK, no?
    return;
  }

  enable_steppers();
  purge_commands(); //clear old commands
  parse_commands(instruction); //create linked list of arguments
  if (command_exists('G')) {
    code = getValue('G');

    switch(code) {
    case 0: //Rapid Motion
      setXYZ(&fp);
      set_target(&fp);
      r_move(0); //fast motion in all axis
      return_status = RET_OK;
      break;
    case 1: //Coordinated Motion
      setXYZ(&fp);
      set_target(&fp);
      if (command_exists('F')) {
        feedrate = getValue('F');
        if (feedrate > getMaxFeedrate()) feedrate = getMaxFeedrate();
      }
      r_move( feedrate );
      return_status = RET_OK;
      break;
    case 2://Clockwise arc
    case 3://Counterclockwise arc
      {
        FloatPoint cent;
        float angleA, angleB, angle, radius, length, aX, aY, bX, bY;
        long feedrate_micros = 0;

        //Set fp Values
        setXYZ(&fp);
        // Centre coordinates are always relative
        cent.x = xaxis->current_units + getValue('I');
        cent.y = yaxis->current_units + getValue('J');

        aX = (xaxis->current_units - cent.x);
        aY = (yaxis->current_units - cent.y);
        bX = (fp.x - cent.x);
        bY = (fp.y - cent.y);

        if (code == 2) { // Clockwise
          angleA = atan2(bY, bX);
          angleB = atan2(aY, aX);
        } 
        else { // Counterclockwise
          angleA = atan2(aY, aX);
          angleB = atan2(bY, bX);
        }

        // Make sure angleB is always greater than angleA
        // and if not add 2PI so that it is (this also takes
        // care of the special case of angleA == angleB,
        // ie we want a complete circle)
        if (angleB <= angleA) angleB += 2 * M_PI;
        angle = angleB - angleA;

        radius = sqrt(aX * aX + aY * aY);
        length = radius * angle;
        int steps, s, step;
        steps = (int) ceil(length / curve_section);

        FloatPoint newPoint;
        for (s = 1; s <= steps; s++) {
          step = (code == 3) ? s : steps - s; // Work backwards for CW
          newPoint.x = cent.x + radius * cos(angleA + angle * ((float) step / steps));
          newPoint.y = cent.y + radius * sin(angleA + angle * ((float) step / steps));
          newPoint.z = zaxis->current_units;
          set_target(&newPoint);

          // Need to calculate rate for each section of curve
          feedrate_micros = (feedrate > 0) ? feedrate : getMaxFeedrate();

          // Make step
          r_move(feedrate_micros);
        }
      }
      return_status = RET_OK;
      break;
    case 4: //Dwell
      //delay((int)getValue('P'));
      return_status = RET_OK;
      break;
    case 20: //Inches for Units
      _units[0] = config.steps_inch.x;
      _units[1] = config.steps_inch.y;
      _units[2] = config.steps_inch.z;
      curve_section = CURVE_SECTION_INCHES;
      calculate_deltas();
      return_status = RET_OK;
      break;
    case 21: //mm for Units
      _units[0] = (uint16_t)((float)config.steps_inch.x / 25.4);
      _units[1] = (uint16_t)((float)config.steps_inch.y / 25.4);
      _units[2] = (uint16_t)((float)config.steps_inch.z / 25.4);
      curve_section = CURVE_SECTION_MM;
      calculate_deltas();
      return_status = RET_OK;
      break;
    case 28: //go home.
      set_target(&zeros);
      r_move(getMaxFeedrate());
      return_status = RET_OK;
      break;
    case 30://go home via an intermediate point.
      //Set Target
      setXYZ(&fp);
      set_target(&fp);
      //go there.
      r_move(getMaxFeedrate());
      //go home.
      set_target(&zeros);
      r_move(getMaxFeedrate());
      return_status = RET_OK;
      break;
    case 81: // drilling operation
      temp = zaxis->current_units;
      //Move only in the XY direction
      setXYZ(&fp);
      set_target(&fp);
      zaxis->target_units = temp;
      calculate_deltas();
      r_move(getMaxFeedrate());
      //Drill DOWN
      zaxis->target_units = getValue('Z') + ((config.abs_mode) ? 0 : zaxis->current_units);
      calculate_deltas();
      r_move(getMaxFeedrate());
      //Drill UP
      zaxis->target_units = temp;
      calculate_deltas();
      r_move(getMaxFeedrate());
      return_status = RET_OK;
      break;
    case 90://Absolute Positioning
      config.abs_mode = true;
      config_save();
      return_status = RET_OK;
      break;
    case 91://Incremental Positioning
      config.abs_mode = false;
      config_save();
      return_status = RET_OK;
      break;
    case 92://Set as home
      set_position(&zeros);
      return_status = RET_OK;
      break;
    case 93://Inverse Time Feed Mode
      Serial.println("ERR NOT_SUPPORTED");
      break;  //TODO: add this 
    case 94://Feed per Minute Mode
      Serial.println("ERR NOT_SUPPORTED");
      break;  //TODO: add this
    default:
      Serial.println("ERR NOT_SUPPORTED");
    }
  }
  if (command_exists('M')) {
    code = getValue('M');
    switch(code) {
    case 3: // turn on motor
    case 4:
      motor_on();
      return_status = RET_OK;
      break;
    case 5: // turn off motor
      motor_off();
      return_status = RET_OK;
      break;

      /*  case 82:
       DDRC |= _BV(1);
       PORTC &= ~_BV(1);
       DDRC &= ~_BV(0);
       PORTC |= _BV(0);
       // setup initial position
       for (int i=0; i<20; i++) {
       k=0;
       PORTB |= _BV(5); //go down
       while(PINC & _BV(0)) {
       PORTB |= _BV(2);
       delayMicroseconds(1);
       PORTB &= ~_BV(2);
       delayMicroseconds(200);
       k++;
       }
       //print result for this point
       Serial.println(k,DEC);
       PORTB &= ~_BV(5);  //move up to origin        
       while (k--) {
       PORTB |= _BV(2);
       delayMicroseconds(1);
       PORTB &= ~_BV(2);
       delayMicroseconds(12.5*config.stepping);
       }
       }
       break;
       */
    case 100: //specify currents in AMPS
      if (command_exists('X')) 
        if (code = setCurrent(CHAN_X, getValue('X'))) config.current.x = code;
      if (command_exists('Y')) 
        if (code = setCurrent(CHAN_Y, getValue('Y'))) config.current.y = code;
      if (command_exists('Z')) 
        if (code = setCurrent(CHAN_Z, getValue('Z'))) config.current.z = code;
      config_save();
      return_status = RET_OK;
      break;
    case 101: //specify steps/inch
      if (command_exists('X')) config.steps_inch.x = getValue('X');
      if (command_exists('Y')) config.steps_inch.y = getValue('Y');
      if (command_exists('Z')) config.steps_inch.z = getValue('Z');
      config_save();
      return_status = RET_OK;
      break;
    case 102: //specify max feedrate
      if (command_exists('X')) config.max_feedrate.x = getValue('X');
      if (command_exists('Y')) config.max_feedrate.y = getValue('Y');
      if (command_exists('Z')) config.max_feedrate.z = getValue('Z');
      config_save();
      return_status = RET_OK;
      break;
    case 103: //M99 S{1,2,4,16} -- set stepping mode
      if (command_exists('S')) {
        code = getValue('S');
        if (code == 1 || code == 2 || code == 4 ||  code == 16) {
          config.stepping = code;
          setStep(config.stepping);
          config_save();
        }
      }
      return_status = RET_OK;
      break;
    case 104: //X(0|1) Y(0|1) Z(0|1) - set direction per axis
      if (command_exists('X')) config.dir = (getValue('X')) ? (config.dir&~0x01) : (config.dir|0x01);
      if (command_exists('Y')) config.dir = (getValue('Y')) ? (config.dir&~0x02) : (config.dir|0x02);
      if (command_exists('Z')) config.dir = (getValue('Z')) ? (config.dir&~0x04) : (config.dir|0x04);
      config_save();
      return_status = RET_OK;
      break;
    case 105: //S(1-255) set the PWM output for the motor speed
      if (command_exists('S')) config.motorSpeed = getValue('S');
      config_save();
      return_status = RET_OK;
      break;
    case 200:
      config_save();
      return_status = RET_OK;
      break;
    case 201:
      config_dump();
      return_status = RET_OK;
      break;
    case 202: //output stauts of limit switches
      Serial.print("MSG X Y Z ESTOP:");
        for (uint8_t i=0; i<3; i++) {
          Serial.print( digitalRead(axis_array[i]->minMax_pin) ? "OPEN  ": "SHORT " );
        }
        Serial.println( digitalRead(ESTOP) ? "OPEN  ": "SHORT ");
      return_status = RET_OK;
      break;
    case 250: //set ZERO location on PCB
      // move down till ESTOP pin shorts (i.e. spindle touches PCB)
      while (digitalRead(ESTOP)) {
        manual_step(ZAXIS, BACKWARD);
        delayMicroseconds(200);
      }
      // move up slowly till ESTOP pin no longer shorts (should be 1 step)
      while (!digitalRead(ESTOP)) {
        manual_step(ZAXIS, FORWARD);
        delay(1);
      }
      zaxis->current_units = 0;
      calculate_deltas();
      return_status = RET_OK;
      break;
    case 251: //set ZERO location on PCB
      {
        uint32_t count=0;
        // move down till ESTOP pin shorts (i.e. spindle touches PCB)
        while (digitalRead(ESTOP)) {
          manual_step(ZAXIS, BACKWARD);
          count++;
          delayMicroseconds(200);
        }
        Message1("STEPS",count);
        // move to start
        while (count--) {
          manual_step(ZAXIS, FORWARD);
          delay(200);
        }
      }
      return_status = RET_OK;
      break;
    default:
      Serial.println("ERR NOT_SUPPORTED");
      break;
    } 
  }
  if (!quiet) {
    switch(return_status) {
    case RET_OK:
      Serial.println("OK"); //command successfult
      break;
    case RET_SILENT:
      //do nothing
      break;
    case RET_ERR:
    case RET_UNKNOWN:
    default:
      Serial.println("ERR"); //XXX need to specify error type
      break;

    }
  }
}



























