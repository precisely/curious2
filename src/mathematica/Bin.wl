(* ::Package:: *)

BeginPackage["Bin`"]

daily::usage = "Bin`daily[dates]"

Begin["`Private`"]

daily[dates_] := Map[
  {#[[1]], Length[#]}&,
  Gather@Sort[Map[#[[;;3]]&, dates]]];

End[]
EndPackage[]
