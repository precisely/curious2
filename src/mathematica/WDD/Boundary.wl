(* ::Package:: *)

BeginPackage["WDD`Boundary`", {"WDD`Utils`"}]; (*  WolframDataDrop access for Boundary data. *)

downloadBoundaries::usage = "WDD`Boundary`downloadBoundaries[]";

Begin["`Private`"];
  (* Download data.  Slow Operation, so only do this in the beginning once. *)
  downloadBoundaries[] := (
    boundaryBin = Databin[App`boundaryBinId];
    rBoundaryBin = WDD`Utils`regularizeBin[boundaryBin];
  );

  getBoundaryData[userId_, tagId_, type_] :=
    Select[rBoundaryBin, #[["userId"]] == userId && #[["tagId"]] == tagId && #[["type"]] == type&];

  getStartBoundaries[userId_, tagId_] :=
    getBoundaryData[userId, tagId, 1]

  getStopBoundaries[userId_, tagId_] :=
    getBoundaryData[userId, tagId, 2]


End[];
EndPackage[];
