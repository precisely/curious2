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

  setUpMarkerUI[] := (
    Column[{
      (* Row 1 *)
      Column[{
        Style["Drag start marker: ", Darker[Green]], 
        Slider[Dynamic[tStart], {AbsoluteTime[{App`startYear, App`startMonth, App`startDay}], AbsoluteTime[{App`stopYear, App`stopMonth, App`stopDay}], 1},
               ImageSize->sliderWidth],
      }],
     (* Row 2 *)
      Row[{
      Row[{Style["Marker Start Year", Darker[Green]], 
       InputField@Dynamic[App`markerStartYear] }, Spacer[10]],
      Row[{Style["Marker Start Month", Darker[Green]], 
       InputField@Dynamic[App`markerStartMonth] }, Spacer[10]],
      Row[{Style["Marker Start Day", Darker[Green]], 
       InputField@Dynamic[App`markerStartDay] }, Spacer[10]]
      }, Spacer[20]],
      Row[{
        Button["Save Start Boundary", (
                Boundary`saveStartBoundary[];
                DateListPlotUI`makeStartBoundaries[];)]
      }],
      (* Row 3 *)
      Row[{Spacer[100]}],
      (* Row 4 *)
      Column[{
        Style["Drag stop marker: ", Darker[Red]], 
        Slider[Dynamic[tStop], {AbsoluteTime[{App`startYear, App`startMonth, App`startDay}], AbsoluteTime[{App`stopYear, App`stopMonth, App`stopDay}], 1},
                ImageSize-> sliderWidth],
      }],
      Row[{
      Row[{Style["Marker Stop Year ", Darker[Red]], 
       InputField@Dynamic[App`markerStopYear] }, Spacer[10]],
      Row[{Style["Marker Stop Month ", Darker[Red]], 
       InputField@Dynamic[App`markerStopMonth] }, Spacer[10]],
      Row[{Style["Marker Stop Day ", Darker[Red]], 
       InputField@Dynamic[App`markerStopDay] }, Spacer[10]]
      },Spacer[20]],
      Row[{
        Button["Save Stop Boundary", (
           Boundary`saveStopBoundary[];
           DateListPlotUI`makeStopBoundaries[];)]
      }]
    }]

  Dynamic[Refresh[
  (App`markerStartYear = DateList[tStart][[1]];
   App`markerStartMonth = DateList[tStart][[2]];
   App`markerStartDay = DateList[tStart][[3]];
   DateListPlotUI`makeStartBoundaries[];),
     TrackedSymbols->{tStart}, UpdateInterval->1]]

  Dynamic[Refresh[
  (App`markerStopYear = DateList[tStop][[1]];
  App`markerStopMonth = DateList[tStop][[2]];
  App`markerStopDay = DateList[tStop][[3]];
  DateListPlotUI`makeStopBoundaries[];),
    TrackedSymbols->{tStop}, UpdateInterval->1]]

  Dynamic[Refresh[
  (tStart = AbsoluteTime[{App`markerStartYear, App`markerStartMonth, App`markerStartDay}]),
    TrackedSymbols->{App`markerStartYear,App`markerStartMonth,App`markerStartDay}, UpdateInterval->1]]
   
  Dynamic[Refresh[
  (tStop = AbsoluteTime[{App`markerStopYear, App`markerStopMonth, App`markerStopDay}]),
    TrackedSymbols->{App`markerStopYear,App`markerStopMonth,App`markerStopDay}, UpdateInterval->1]]
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

