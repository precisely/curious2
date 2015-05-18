(* ::Package:: *)

BeginPackage["ScaleSelectorUI`", {"Scale`"}]

HOUR=3600;
DAY=24*HOUR;
WEEK=7*DAY;
MONTH=30*DAY;
YEAR=365*DAY;

labels = KeySort[<|
  HOUR-> "1 hour",
  3*HOUR->"3 hrs",
  6*HOUR->"6 hrs",
  DAY-> "1 day",
  3*DAY->"3 days",
  WEEK->"1 week",
  2*WEEK->"2 weeks",
  MONTH->"1 month",
  3*MONTH->"3 months",
  6*MONTH->"6 months",
  YEAR->"1 year"|>];

setSeriesScale[v_] := (
  App`seriesScale = v;
  Scale`update[App`getUserId[], App`getTagId[], v];
  App`refresh[];
);

dropDown[] := ActionMenu["Series scale:", Map[(labels[#] :> setSeriesScale[#])&,Keys[labels]]];

setUp[] := Row[{
  dropDown[],
  Dynamic@ScaleSelectorUI`labels[App`seriesScale],
  "Num Bins",
  Dynamic@Length[App`bins]}, Spacer[10]]


EndPackage[]
