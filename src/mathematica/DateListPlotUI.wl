(* ::Package:: *)

BeginPackage["DateListPlotUI`"];

setUpPlot::usage = "DateListPlotUI`setUp[]";
initMarkers::usage = "DateListPlotUI`init[]";

startMarker = makeStartMarker[];
stopMarker  = makeStopMarker[];

savedStartMarkers = {};
savedStopMarkers = {};


makeMarker[date_] := {date, App`maxHeight}
makeStartMarker[] := makeMarker[{App`markerStartYear, App`markerStartMonth, App`markerStartDay}];
makeStopMarker[]  := makeMarker[{App`markerStopYear, App`markerStopMonth, App`markerStopDay}];

makeStartBoundaries[] :=
  startBoundaries = Join[{makeStartMarker[]}, Map[makeMarker, Boundary`getBoundaryDays[App`getUserId[], App`getTagId[], Boundary`startType]]];

makeStopBoundaries[] :=
  stopBoundaries = Join[{makeStopMarker[]}, Map[makeMarker, Boundary`getBoundaryDays[App`getUserId[], App`getTagId[], Boundary`stopType]]];

Begin["`Private`"];

initMarkers[] := (
  makeStartBoundaries[];
  makeStopBoundaries[];
);

setUpPlot[] := (
  initMarkers[];
  Dynamic[DateListPlot[ {App`bins, startBoundaries, stopBoundaries},
    PlotStyle->{RGBColor[0,0,255,0.5], RGBColor[0,255,0,0.5], RGBColor[255,0,0,0.5]},
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

