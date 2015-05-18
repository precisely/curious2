(* ::Package:: *)

BeginPackage["Boundary`"];

getBoundaries[] :=If[shouldRefresh[], refreshCache[], cachedBoundaries];

refreshCache[] := (lastRefresh = AbsoluteTime[Now]; Return@updateCache[CloudSymbol[App`CSBase <> "boundaries"]]);

updateCache[v_] := (
  cachedBoundaries = v;
  Return[cachedBoundaries];)

secondsSinceLastRefresh[] := AbsoluteTime[Now] - lastRefresh;

shouldRefresh[] := secondsSinceLastRefresh[] > refreshSeconds;

getLastRefresh[] := Return[lastRefresh];

createBoundary[row_]:=Module[{utcRow=KeySort@useUTC[row]},
  updateCache[Append[cachedBoundaries, utcRow]];
  CloudEvaluate[AppendTo[CloudSymbol[App`CSBase <> "boundaries"],utcRow]];
  refreshCache[];];

getStartBoundary[] :=
  <|"date"->DateList[{App`markerStartYear,App`markerStartMonth,App`markerStartDay, App`markerStartHour, App`markerStartMinute, App`markerStartSecond}],
    "tagId" -> App`getTagId[], "userId"->App`getUserId[], "type"->Boundary`startType|>

getStopBoundary[] :=
  <|"date"->DateList[{App`markerStopYear,App`markerStopMonth,App`markerStopDay, App`markerStopHour, App`markerStopMinute, App`markerStopSecond}],
    "tagId" -> App`getTagId[], "userId"->App`getUserId[], "type"->Boundary`stopType|>

saveStartBoundary[] := createBoundary[getStartBoundary[]];

saveStopBoundary[] := createBoundary[getStopBoundary[]];

upload[] := CloudSymbol[App`CSBase <> "boundaries"] = cachedBoundaries;

download[] := refreshCache[];

deleteBoundaryByParams[userId_, tagId_, type_, date_] := Module[{
  idx4 = index4[],
  row = KeySort@makeKey4[userId, tagId, type, date]},
  If[KeyExistsQ[idx4, row],
    (cachedBoundaries = Delete[cachedBoundaries, idx4[row]]; upload[]),
    Return@{}]];

indexBy[cols_] := PositionIndex[Map[KeySort, getBoundaries[][[All, cols]]]];

index3[] := indexBy[{"userId", "tagId", "type"}];
index4[] := indexBy[{"userId", "tagId", "type", "date"}];

makeKey3[userId_, tagId_,type_] := KeySort[<|"userId"->userId, "tagId"->tagId, "type"->type |>]
makeKey4[userId_, tagId_,type_, date_] := KeySort[<|"userId"->userId, "tagId"->tagId, "type"->type,"date"->date |>]

getBoundariesWhere[userId_,tagId_, type_]:= Module[{idx=index3[], k=makeKey3[userId, tagId, type]},
  If[KeyExistsQ[idx, k],
    Part[getBoundaries[], idx[k]],
    {}]];

getBoundaryDates[userId_,tagId_,type_] := getBoundariesWhere[userId, tagId, type][[All, "date"]];



truncate[] := (
  CloudSymbol[App`CSBase <> "boundaries"] = {};
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
cachedBoundaries =  refreshCache[];
lastRefresh = AbsoluteTime[Now];

EndPackage[];
