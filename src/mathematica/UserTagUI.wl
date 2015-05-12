(* ::Package:: *)

BeginPackage["UserTagUI`"]
  userN = 0
  tagN
  uPos = Null
  tPos = Null


  init2[uids_,tids_, userPos2_, tagPos2_] := (
    userN = Length[uids];
    tagN = Map[Length, tids];
    uPos = userPos2;
    tPos = tagPos2; )

  setUserPos[pos_] := (
    App`userPos = pos;
    App`tagPos = App`lastTagPos[[pos]];
    App`refresh[];
  );

  setTagPos[pos_] := (
    App`tagPos = pos;
    App`lastTagPos[[App`userPos]] = pos;
    App`refresh[];
  );

  incPos[pos_, n_] := Mod[pos, n] + 1;

  decPos[pos_, n_] := Mod[pos-2, n] + 1;

  range[n_] := Table[i, {i, n}];

  setUp[] :=
    Grid[{{
        ActionMenu["User",Map[(ToString[#] :> setUserPos[#]) &,range[userN]]],
        Dynamic[App`userPos],
        Button["Next user", setUserPos[incPos[App`userPos,userN]]],
        Button["Previous user", setUserPos[decPos[App`userPos,userN]]],
        Spacer[20],
        Style["user id:", Black],
        Dynamic[App`getUserId[]]
      },{
        ActionMenu["Tag",Map[(ToString[#] :> setTagPos[#]) &,range[tagN[[App`userPos]]]]],
        Dynamic[App`tagPos],
        Button["Next tag", setTagPos[incPos[App`tagPos,tagN[[App`userPos]]]]],
        Button["Previous tag", setTagPos[decPos[App`tagPos,tagN[[App`userPos]]]]],
        Spacer[20],
        Style["tag id:", Black],
        Dynamic[App`getTagId[]]
    }}]

EndPackage[]



