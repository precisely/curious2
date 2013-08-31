<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<style type="text/css">
#graphList #graphItem {clear:both}
#graphList .summary {padding-top:50px}
#graphList .summary h2 {background:url(images/pointer_right.png) no-repeat center left;padding-left:25px;font-weight:bold;font-size:100%;display:inline-block;width:485px}
#graphList .summary .date {font-size:80%;display:inline-block;width:100px;color:#666666}
#graphList .summary .preview {font-size:80%;display:inline-block;width:60px;}
#graphList .summary .update {font-size:80%;display:inline-block;width:68px;height:21px;text-align:center;line-height:21px;color:#ffffff ! important;text-transform:uppercase;background:url(images/preview_btn.gif) no-repeat}
#graphList .summary .close {display:inline-block;width:21px;height:10px;padding-left:5px;background:url(images/x.gif) no-repeat center center;}
#graphList .summary .close span {display:none}
#graphList .content {padding-top:25px;font-size:80%;color:#666666;}
#graphList .content h2 {color:#000000;font-weight:bold}
#graphList .sidebar {margin-left:25px;width:200px;float:left;}
#graphList .sidebar h2 {padding-top:20px}
#graphList .sidebar ul {list-style-type: none;padding: 0px;margin: 0px;}
#graphList .sidebar li {list-item:none;padding-top:20px;}
#graphList .sidebar li a {color:#666666 ! important}
#graphList .notes {margin-left:25px;float:left;width:600px}
#graphList .notes p {line-height:1.5em;padding-top:20px}
</style>
<script type="text/javascript">

function deletePlotId(id) {
	if (!confirm("Are you sure you want to delete the saved graph?"))
		return;
	$.getJSON("/home/deletePlotDataId?id=" + escape(id) + "&callback=?",
			function(entries) {
			if (checkData(entries))
				location.reload(true);
			});
}

function doLogout() {
	callLogoutCallbacks();
}

$(function(){
	initTemplate();
	
	$.getJSON("/home/listPlotData?callback=?",
		function(data){
			if (!checkData(data))
				return;
		
			jQuery.each(data, function() {
				$("#graphList").append('<div class="graphItem">\
					<div class="summary">\
					<h2>' + this['name'] + '</h2>\
					<div class="date">' + this['created'].getMonth() +'/' + this['created'].getDay() + '/' + this['created'].getFullYear() + '</div>\
					<a class="preview" href="/home/viewgraph?plotDataId=' + this['id'] + '">load</a>\
					<a href="#" onclick="deletePlotId(' + this['id'] + ')"><img src="/images/x.gif"></a>\
					</div>\
				</div>');
				return true;
			});
		
			refreshPage();
		});
});
</script>
</head>
<body>
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
