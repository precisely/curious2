(* ::Package:: *)

BeginPackage["Scale`"]

CSBase = "user:thirdreplicator/CloudSymbol/";

v[] := Scale`CSBase <> "scales";

initContainer[] := CloudSymbol[CSBase <> "scales"] = {};
initCache[] := cache = {};
init[] := (initCache[]; initContainer[];)

(* Schema summary: userId\[Rule]Number, tagId\[Rule]Number, scale\[Rule]Number *)

all[] := CloudSymbol[Scale`v[]];

upload[] := all[] = cache; 

download[] := cache = all[];

create[row_] := (
  cache = Append[cache, KeySort[row]];
  upload[]
  );

positionIndex[userId_, tagId_] :=
  PositionIndex[cache[[All, {"userId", "tagId"}]]];

pos[userId_, tagId_] :=Module[
  {idx=positionIndex[userId, tagId],
    k=makeKey[userId, tagId]},
  If[KeyExistsQ[idx,k], First[idx[k]], False]];

value[userId_, tagId_] := Module[{i=pos[userId, tagId]},
  If[NumberQ[i],
     cache[[i]]["scale"],
     Null]
  ];

existsQ[userId_, tagId_] := NumberQ[pos[userId, tagId]];

update[userId_, tagId_, scale_] := 
  Module[{i=pos[userId, tagId]},
    If[NumberQ[i],
         (cache = ReplacePart[cache, pos[userId, tagId] -> makeRow[userId, tagId,scale]];
           upload[]),
          create[makeRow[userId, tagId, scale]]
         ];
  ];


Begin["`Private`"]

makeRow[userId_, tagId_, scale_] := KeySort[<|"userId"->userId, "tagId"->tagId, "scale"->scale|>];
makeKey[userId_, tagId_] := <|"userId"->userId, "tagId"->tagId|>;

End[]
EndPackage[]
