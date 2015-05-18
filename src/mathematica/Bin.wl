(* ::Package:: *)

BeginPackage["Bin`"]

daily::usage = "Bin`daily[dates]"

Begin["`Private`"]

daily[dates_] := Map[
  {#[[1]], Length[#]}&,
  Gather@Sort[Map[#[[;;3]]&, dates]]];

binify[dates_, scale_] := Map[{DateList[scale*First[#]],Length[#]}&,
  Gather@Sort@Map[Round[#/scale]&, Map[AbsoluteTime,dates]]];

End[]
EndPackage[]
