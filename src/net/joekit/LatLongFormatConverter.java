package net.joekit;

class LatLongFormatConverter {

  /** Decimal degrees. */
  double dfDecimal;

  /** Degree part of degrees/minutes/seconds. */
  double dfDegree;

  /** Minute part of degrees/minutes/seconds. */
  double dfMinute;

  /** Second part of degrees/minutes/seconds. */
  double dfSecond;
	
  LatLongFormatConverter(double dfDecimalIn) {
    dfDecimal = dfDecimalIn;
    fromDec2DMS();
  }
	
  LatLongFormatConverter(double dfDegreeIn, double dfMinuteIn, double dfSecondIn) {
    dfDegree = dfDegreeIn;
    dfMinute = dfMinuteIn;
    dfSecond = dfSecondIn;
    fromDMS2Dec();
  }
	
  void fromDec2DMS() {
    // Fraction after decimal.
    double dfFrac;
 
    // Fraction converted to seconds.
    double dfSec;
    dfDegree = Math.floor(dfDecimal);
    if (dfDegree < 0) {
      dfDegree = dfDegree + 1;
    }
    dfFrac = Math.abs(dfDecimal - dfDegree);
    dfSec = dfFrac * 3600;
    dfMinute = Math.floor(dfSec / 60);
    dfSecond = dfSec - dfMinute * 60;
    if (Math.rint(dfSecond) == 60) {
      dfMinute = dfMinute + 1;
      dfSecond = 0;
    }

    if (Math.rint(dfMinute) == 60) {
      if (dfDegree < 0) {
        dfDegree = dfDegree - 1;
      } else {
        // used to check if ( dfDegree => 0 )..
        dfDegree = dfDegree + 1;
      }

      dfMinute = 0;
    }

    return;
  }
	
  void fromDMS2Dec() {
    // fraction after decimal
    double dfFrac;
    dfFrac = dfMinute / 60 + dfSecond / 3600;
    if (dfDegree < 0) {
      dfDecimal = dfDegree - dfFrac;
    } else {
      dfDecimal = dfDegree + dfFrac;
    }
    return;
  }
	
  double getDecimal() {
    return dfDecimal;
  }

  double getDegree() {
    return dfDegree;
  }

  double getMinute() {
    return dfMinute;
  }

  double getSecond() {
    return dfSecond;
  }
}
