(* ::Package:: *)

BeginPackage["App`", {"WDD`Series`", "Bin`", "UserTagUI`"}];

init::usage = "init[]"
initConstants::usage = "initConstants[]"
initData::usage = "initData[]"
initUI::usage = "initUI[]"
getUserId::usage = "getUserId[]"
getTagId::usage = "getTagId[]"
refresh::usage = "refresh[]"

CSBase = "user:thirdreplicator/CloudSymbol/";
mode="local";  (* := "local" | "remote" *)

userIds = {};
tagIds = {};
boundaryBinId = "";
bins = {};
seriesScale = 86400;

Begin["`Private`"]

init[] :=
  (
  initConstants[];
  initData[];
  initUI[];
  )

initConstants[] :=
  (
  seriesBinId =  "4FewoDhB";
  boundaryBinId = "4zPg5wWr";
  userIds =  {1, 4, 51, 101, 113, 115, 117, 127};

  userPos = 1;
  tagPos = 1;

  startYear=2010;
  startMonth = 1;
  startDay = 1;
  stopYear=2015;
  stopMonth = 5;
  stopDay = 1;

  markerStartYear=2012;
  markerStartMonth = 1;
  markerStartDay = 1;
  markerStartHour =0;
  markerStartMinute = 0;
  markerStartSecond= 0.;

  markerStopYear=2013;
  markerStopMonth = 1;
  markerStopDay = 1;
  markerStopHour =0;
  markerStopMinute = 0;
  markerStopSecond= 0.;

  )

initData[] := (
  If[Length[WDD`Series`rSeriesBin] == 0,
    WDD`Series`downloadSeries[],
    Print["Using preloaded series data in WDD`Series`rSeriesBin"]];
  tagIds = WDD`Series`initTagIds[];
  lastTagPos =  Map[1&,tagIds];
  )

getUserId[] :=
  userIds[[userPos]]

getTagId[] :=
  tagIds[[userPos, tagPos]]

refresh[] := (
  bins = Bin`binify[WDD`Series`getDates[getUserId[], getTagId[]],App`seriesScale];
  maxHeight = 2*Max[bins[[All, 2]]];
  DateListPlotUI`initMarkers[]; 
  )

initUI[] := (
  UserTagUI`init2[userIds, tagIds, userPos, tagPos];
);

End[]
EndPackage[];
