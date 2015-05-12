(* ::Package:: *)

BeginPackage["WDD`Utils`"]

  (* Need to keep te row's keys sorted in order for rows to be comparable. *)
  regularizeBin[bin_] := Map[KeySort, Normal[bin]];

  (* create *)
  saveRow[row_, bin_] := DatabinAdd[bin, KeySort[row]];

  (* read *)
  getSeriesData[userId_, tagId_, bin_] :=
    Select[bin, #[["userId"]] == userId && #[["tagId"]] == tagId &];


EndPackage[];
