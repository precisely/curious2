<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="menu" />
<script type="text/javascript">

function deletePlotId(id) {
	showYesNo("Are you sure you want to delete the saved graph?", function() {
			backgroundJSON("deleting saved graph", "/home/deletePlotDataId?id=" + escape(id) + "&callback=?",
				function(entries) {
					if (checkData(entries))
						location.reload(true);
				});
	});
}

function doLogout() {
	callLogoutCallbacks();
}

$(function() {
	backgroundJSON("getting list of saved graphs", "/home/listPlotData?callback=?",
		function(data){
			if (!checkData(data))
				return;
		
			jQuery.each(data, function() {
				$("#graphList").append('<div class="graphItem">\
					<div class="summary">\
					<h2>' + this['name'] + '</h2>\
					<div class="date">' + this['created'].getMonth() +'/' + this['created'].getDay() + '/' + this['created'].getFullYear() + '</div>\
					<a class="preview" href="/home/viewgraph?plotDataId=' + this['id'] + '">load</a>\
					<a href="#" onclick="deletePlotId(' + this['id'] + ')"><img height="12" width="12" src="/images/x.gif"></a>\
					</div>\
				</div>');
				return true;
			});
		
			refreshPage();
		});
});
</script>
</head>
<body class="load-graph">
<!-- MAIN -->
<div class="main">

	<div id="graphList">
	
		<!--div class="graphItem">
			<div class="summary">
			<h2>Graph Title Lorem Ipsum Whatnot This and That</h2>
			<div class="date">00.00.0000</div>
			<a class="preview" href="">preview</a>
			<a class="update" href="">update</a>
			<a class="close" href=""></a>
			</div>
		</div-->

	</div>

</div>
<!-- /MAIN -->

<div style="clear: both;"></div>

</body>
</html>
