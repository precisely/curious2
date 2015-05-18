(* ::Package:: *)

BeginPackage["DateListPlotUI`"];

setUpPlot::usage = "DateListPlotUI`setUp[]";
initMarkers::usage = "DateListPlotUI`init[]";

startMarker = makeStartMarker[];
stopMarker  = makeStopMarker[];

savedStartMarkers = {};
savedStopMarkers = {};


makeMarker[date_] := {date, App`maxHeight}
makeStartMarker[] := makeMarker[DateList[{App`markerStartYear, App`markerStartMonth, App`markerStartDay, App`markerStartHour, App`markerStartMinute, App`markerStartSecond}]];
makeStopMarker[]  := makeMarker[DateList[{App`markerStopYear, App`markerStopMonth, App`markerStopDay, App`markerStopHour, App`markerStopMinute, App`markerStopSecond}]];

nullMarker = {{2012, 1, 1, 2, 2, 0.}, 0}
nullList = {nullMarker}

makeStartBoundaries[] := (
  startMarker = makeStartMarker[];
  startBoundaries =  Join[nullList, Map[makeMarker, Boundary`getBoundaryDates[App`getUserId[], App`getTagId[], Boundary`startType]]];
  );

makeStopBoundaries[] := (
  stopMarker = makeStopMarker[];
  stopBoundaries = Join[nullList, Map[makeMarker, Boundary`getBoundaryDates[App`getUserId[], App`getTagId[], Boundary`stopType]]];
  );

Begin["`Private`"];

initMarkers[] := (
  makeStartBoundaries[];
  makeStopBoundaries[];
);

setUpPlot[] := (
  initMarkers[];
  Dynamic[DateListPlot[ {App`bins, startBoundaries, stopBoundaries, {startMarker}, {stopMarker}},
    PlotStyle->{RGBColor[0,0,255,0.5], RGBColor[0,255,0,0.5], RGBColor[255,0,0,0.5], Darker[Green], Orange},
    Filling->Bottom, 
    Joined->False, 
    PlotMarkers->".",
    FillingStyle->Directive[CapForm["Butt"],Thickness[0.001]],
    AspectRatio->1/5,
    ImageSize-> 1000,
    PlotRange -> {{
    DateList[Map[Floor,{App`startYear,App`startMonth,App`startDay}]], 
    DateList[Map[Floor,{App`stopYear, App`stopMonth, App`stopDay}]]}, Automatic}
    ]]);

End[];

EndPackage[];

