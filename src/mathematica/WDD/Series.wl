(* ::Package:: *)

BeginPackage["WDD`Series`", {"WDD`Utils`"}]; (*  WolframDataDrop access for Series data. *)

downloadSeries::usage = "WDD`Series`download[]";
getDates::usage = "WDD`Series`getDates[App`getUserId[], App`getTagId[]]"
initTagIds::usage = "WDD`Series`initTagIds[]";
seriesBin::usage = "WDD`Series`seriesBin";
rSeriesBin::usage = "WDD`Series`rSeriesBin";
getTagIds::usage = "WDD`Series`getTagIds[App`getUserId[]]";
getAllowedUserIds::usage = "getAllowedUserIds[]";
getAllUserIds::usage = "getAllUserIds[]";

Begin["`Private`"];

  (* Download data.  Slow Operation, so only do this in the beginning once. *)
  downloadSeries[] := (
    If[FileExistsQ["rSeriesBin.csv"],(
      rSeriesBin = ReadList["rSeriesBin.csv"];
    ), (
      seriesBin = Databin[App`seriesBinId];
      rSeriesBin = WDD`Utils`regularizeBin[seriesBin]; (* regularized series bin *)
      Export["rSeriesBin.csv", rSeriesBin];
    )];
  );

  getDates[userId_, tagId_] :=
    WDD`Utils`getSeriesData[userId, tagId,rSeriesBin][[All, "date"]];

  (* to populate drop-down menu for tags *)
  getTagIds[userId_] := Sort@DeleteDuplicates[Select[rSeriesBin, #[["userId"]] == userId&][[All, "tagId"]]];

  initTagIds[] := Map[getTagIds, App`userIds]

  getAllowedUserIds[] :=  {1, 4, 51, 101, 113, 115, 117, 127}

  getAllUserIds[] := DeleteDuplicates[rSeriesBin[[All, "userId"]]]

End[];
EndPackage[];
