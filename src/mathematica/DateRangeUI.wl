(* ::Package:: *)

BeginPackage["DateRangeUI`"]

 initDateRange[] := (
    App`startYear=2014;
    App`startMonth = 1;
    App`startDay = 1;
    App`stopYear=2015;
    App`stopMonth = 1;
    App`stopDay = 1;
    );

  setUp[] := (
    Column[{
      Row[{
      Row[{Style["Plot Start Year", Black], 
       InputField@Dynamic[App`startYear] }, Spacer[10]]
      ,
      Row[{Style["Plot Start Month", Black], 
       InputField@Dynamic[App`startMonth] }, Spacer[10]]
      ,
      Row[{Style["Plot Start Day", Black], 
       InputField@Dynamic[App`startDay] }, Spacer[10]]

      }, Spacer[20]]
      ,
      Row[{
      Row[{Style["Plot Stop Year ", Black], 
       InputField@Dynamic[App`stopYear] }, Spacer[10]]
      ,
      Row[{Style["Plot Stop Month ", Black], 
       InputField@Dynamic[App`stopMonth] }, Spacer[10]]
      ,
      Row[{Style["Plot Stop Day ", Black], 
       InputField@Dynamic[App`stopDay] }, Spacer[10]]

      }, Spacer[20]]
    }]
   )

EndPackage[]



