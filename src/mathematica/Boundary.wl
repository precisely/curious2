(* ::Package:: *)

BeginPackage["Boundary`"];

getBoundaries[] :=If[shouldRefresh[], refreshCache[], cachedBoundaries];

refreshCache[] := Return@updateCache[CloudSymbol[Global`CSBase <> "boundaries"]];

updateCache[v_] := (
  cachedBoundaries = v;
  lastRefresh = AbsoluteTime[Now];
  Return[cachedBoundaries];)

secondsSinceLastRefresh[] := AbsoluteTime[Now] - lastRefresh;

shouldRefresh[] := secondsSinceLastRefresh[] > refreshSeconds;

getLastRefresh[] := Return[lastRefresh];

createBoundary[row_]:=Module[{utcRow=KeySort@useUTC[row]},
  updateCache[Append[cachedBoundaries, utcRow]];
  CloudEvaluate[AppendTo[CloudSymbol[Global`CSBase <> "boundaries"],utcRow]];
  refreshCache[];];

getStartBoundary[] :=
  <|"date"->DateList[{App`markerStartYear,App`markerStartMonth,App`markerStartDay}],
    "tagId" -> App`getTagId[], "userId"->App`getUserId[], "type"->Boundary`startType|>

getStopBoundary[] :=
  <|"date"->DateList[{App`markerStopYear,App`markerStopMonth,App`markerStopDay}],
    "tagId" -> App`getTagId[], "userId"->App`getUserId[], "type"->Boundary`stopType|>

saveStartBoundary[] := createBoundary[getStartBoundary[]];

saveStopBoundary[] := createBoundary[getStopBoundary[]];

deleteBoundaryByRow[row_]:=Module[{sRow=KeySort[row]},
  cachedBoundaries = Delete[cachedBoundaries, PositionIndex[cachedBoundaries][sRow]];
  CloudEvaluate[CloudSymbol[Global`CSBase <> "boundaries"] = Delete[CloudSymbol[Global`CSBase <> "boundaries"], PositionIndex[CloudSymbol[Global`CSBase <> "boundaries"]][sRow]]]];

deleteBoundaryByParams[userId_, tagId_, type_, date_] := Module[{row = <|"userId" -> userId, "tagId" -> tagId, "type" -> type, "date" -> date |>},
  deleteBoundaryByRow[row];];

getBoundariesWhere[userId_,tagId_, type_]:=Return@Select[getBoundaries[],#[["userId"]]==userId&&#[["tagId"]]==tagId&&#[["type"]] == type&];

getBoundaryDates[userId_,tagId_,type_] := getBoundariesWhere[userId, tagId, type][[All, "date"]];

getBoundaryDays[userId_,tagId_,type_] := Map[#[[;;3]]&,getBoundaryDates[userId,tagId,type]];


truncate[] := (
  CloudSymbol[Global`CSBase <> "boundaries"] = {};
  updateCache[{}]);

setPermissions[] := (
  boundaryObj = CloudObject["boundaries", $CloudSymbolBase];
  SetOptions[boundaryObj,Permissions->"Public"]
);

allKeys = {"date","tagId","type","userId"};

useUTC[row_] := Module[{nRow=row},
  nRow[["date"]] = DateList[row[["date"]]];
  Return[nRow];
];

startType = 1;
stopType = 2;
refreshSeconds = 1000000;
cachedBoundaries =  getBoundaries[]; (*refreshCache[];*)
lastRefresh = AbsoluteTime[Now];

EndPackage[];
