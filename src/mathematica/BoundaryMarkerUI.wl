(* ::Package:: *)

BeginPackage["BoundaryMarkerUI`"]



  tStart = AbsoluteTime[{2012,1,1}];
  tStop = AbsoluteTime[{2015,1,1}];

  sliderWidth = 1000;

  sqlDateFormat = {"Year", "-", "Month", "-", "Day", " ", "Hour", ":", "Minute", ":", "Second"};

  init[] := (
    App`markerStartYear=2012;
    App`markerStartMonth = 1;
    App`markerStartDay = 1;
    App`markerStopYear=2013;
    App`markerStopMonth = 1;
    App`markerStopDay = 1;
    );

  dateElt[label_, color_, v_, width_, space_] := Row[{Style[label, Darker[color]], InputField[v, FieldSize->{width, 1}]}, Spacer[space]];

  instructions[] := Row[{ Style["Drag start/stop marker then click on Save.  Fine tune with the input fields.", Darker[Black]]}, Spacer[20]]

  startMarkerScrubber[] := 
        Slider[Dynamic[tStart], {Dynamic@AbsoluteTime[{App`startYear, App`startMonth, App`startDay}], Dynamic@AbsoluteTime[{App`stopYear, App`stopMonth, App`stopDay}], 1},
               ImageSize->sliderWidth];

  stopMarkerScrubber[] := 
        Slider[Dynamic[tStop], {Dynamic@AbsoluteTime[{App`startYear, App`startMonth, App`startDay}], Dynamic@AbsoluteTime[{App`stopYear, App`stopMonth, App`stopDay}], 1},
                ImageSize-> sliderWidth];

   startMarkerBreakDown[] := Row[{
        dateElt["Marker start year", Green, Dynamic[App`markerStartYear], 4, 10],
        dateElt["month", Green, Dynamic[App`markerStartMonth], 2, 10],
        dateElt["day", Green, Dynamic[App`markerStartDay], 2, 10],
        dateElt["hour", Green, Dynamic[App`markerStartHour], 2, 10],
        dateElt["min", Green, Dynamic[App`markerStartMinute], 2, 10],
        dateElt["sec", Green, Dynamic[App`markerStartSecond], 5, 10]
      }, Spacer[20]]

  stopMarkerBreakDown[] := Row[{ 
        dateElt["Marker stop year", Red, Dynamic[App`markerStopYear], 4, 10],
        dateElt["month", Red, Dynamic[App`markerStopMonth], 2, 10],
        dateElt["day", Red, Dynamic[App`markerStopDay], 2, 10],
        dateElt["hour", Red, Dynamic[App`markerStopHour], 2, 10],
        dateElt["min", Red, Dynamic[App`markerStopMinute], 2, 10],
        dateElt["sec", Red, Dynamic[App`markerStopSecond], 5, 10]}, Spacer[20]]

  startMarkerSaveButton[] := Button["Save Start Boundary", (
                Boundary`saveStartBoundary[];
                DateListPlotUI`makeStartBoundaries[];), ImageSize->Large]

  stopMarkerSaveButton[] := Button["Save Stop Boundary", (
           Boundary`saveStopBoundary[];
           DateListPlotUI`makeStopBoundaries[];), ImageSize->Large]

  updateStartComponents[] :=
    Dynamic[Refresh[DynamicModule[{t=DateList[tStart]},
    (App`markerStartYear = t[[1]];
     App`markerStartMonth = t[[2]];
     App`markerStartDay = t[[3]];
     App`markerStartHour = t[[4]];
     App`markerStartMinute = t[[5]];
     App`markerStartSecond = t[[6]];
     DateListPlotUI`makeStartBoundaries[];)],
       TrackedSymbols->{tStart}, UpdateInterval->1]]

  updateStopComponents[] :=
    Dynamic[Refresh[DynamicModule[{t=DateList[tStop]},
    (App`markerStopYear = t[[1]];
     App`markerStopMonth = t[[2]];
     App`markerStopDay = t[[3]];
     App`markerStopHour = t[[4]];
     App`markerStopMinute = t[[5]];
     App`markerStopSecond = t[[6]];
    DateListPlotUI`makeStopBoundaries[];)],
      TrackedSymbols->{tStop}, UpdateInterval->1]]

  updateSliderVariables[] := (
    updateStartComponents[]
    updateStopComponents[]
  );


syncSliderWithInputFields[] := (
  Dynamic[Refresh[
  (tStart = AbsoluteTime[{App`markerStartYear, App`markerStartMonth, App`markerStartDay, App`markerStartHour, App`markerStartMinute, App`markerStartSecond}]),
    TrackedSymbols->{App`markerStartYear,App`markerStartMonth,App`markerStartDay}, UpdateInterval->1]]
   
  Dynamic[Refresh[
  (tStop = AbsoluteTime[{App`markerStopYear, App`markerStopMonth, App`markerStopDay, App`markerStopHour, App`markerStopMinute, App`markerStopSecond}]),
    TrackedSymbols->{App`markerStopYear,App`markerStopMonth,App`markerStopDay}, UpdateInterval->1]]
  );

  setUpMarkerUI[] := (
    Column[{
      instructions[],
      startMarkerScrubber[],
      startMarkerBreakDown[],
      startMarkerSaveButton[]
      Row[{Spacer[100]}],
      stopMarkerScrubber[],
      stopMarkerBreakDown[],
      stopMarkerSaveButton[]
    }]
    updateSliderVariables[]
    syncSliderWithInputFields[]
  )


deleteBoundaryUI[] :=
  Grid[{{
    Style["Start Boundaries", Black],Style["Stop Boundaries", Black]},{
    Dynamic@Dataset@Map[{DateString[DateList[#], sqlDateFormat], 
    Button["Delete",Boundary`deleteBoundaryByParams[App`getUserId[], App`getTagId[], Boundary`startType, #]]}&,Sort@Boundary`getBoundaryDates[App`getUserId[], App`getTagId[], Boundary`startType]],
    Dynamic@Dataset@Map[{DateString[DateList[#], sqlDateFormat], 
    Button["Delete",Boundary`deleteBoundaryByParams[App`getUserId[], App`getTagId[], Boundary`stopType, #]]}&,Sort@Boundary`getBoundaryDates[App`getUserId[], App`getTagId[], Boundary`stopType]]
  }}, Alignment->{{Center, Center},{Top, Top}}]

EndPackage[]

