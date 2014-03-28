<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<style type="text/css">
#graphList #graphItem {clear:both}
#graphList .summary {padding-top:12px;padding-bottom:12px}
#graphList .summary h2 {width:265px;background:url(images/pointer_right.png) no-repeat center left;padding-left:25px;font-weight:bold;font-size:100%;display:inline-block}
#graphList .summary .date {font-size:80%;display:inline-block;width:9em;color:#666666}
#graphList .summary .userName {font-size:80%;display:inline-block;width:.6em;color:#666666}
#graphList .summary .preview {font-size:80%;display:inline-block;width:50px;padding-bottom:5px;}
#graphList .summary .unpublishButton {width:250px;margin-left:2px;margin-right:4px;padding-top:4px;padding-right:4px;background-color:#08BBF1;color:#FFFFFF;padding-left:4px;padding-bottom:4px;font-size:16px;}
#graphList .summary .publishButton {width:250px;margin-left:2px;margin-right:4px;padding-top:4px;padding-right:4px;background-color:#08BBF1;color:#FFFFFF;padding-left:4px;padding-bottom:4px;font-size:16px;}
#graphList .summary .update {font-size:80%;display:inline-block;width:0px;height:21px;text-align:center;line-height:21px;color:#ffffff ! important;text-transform:uppercase;background:url(images/preview_btn.gif) no-repeat}
#graphList .summary .close {display:inline-block;width:21px;width:10px;height:10px;padding-left:5px;background:url(images/x.gif) no-repeat center center;}
#graphList .summary .close span {display:none}
#graphList .summary .visibility {color:#AAAAAA;width:280px;display:inline-block;text-align:right;}
#graphList .content {padding-top:25px;font-size:80%;color:#666666;}
#graphList .content h2 {color:#000000;font-weight:bold}
#graphList .sidebar {margin-left:25px;width:200px;float:left;}
#graphList .sidebar h2 {padding-top:20px}
#graphList .sidebar ul {list-style-type: none;padding: 0px;margin: 0px;}
#graphList .sidebar li {list-item:none;padding-top:20px;}
#graphList .sidebar li a {color:#666666 ! important}
#graphList .notes {margin-left:25px;float:left;width:600px}
#graphList .notes p {line-height:1.5em;padding-top:20px}
.communityMessage {padding-bottom:1em;}
.discussHeader { padding-top:12px;margin-left:15px;}
</style>
<c:jsCSRFToken keys="getPeopleDataCSRF" />
<script type="text/javascript">

function minLinkName(name) {
	if (name == null || name == "") {
		return "(No Title)";
	}
	return name;
}

function deleteDiscussionId(id) {
	showYesNo("Are you sure you want to delete the saved discussion?", function() {
			queueJSON("deleting discussion", "/home/deleteDiscussionId?id=" + escape(id) + "&callback=?",
				function(entries) {
					if (checkData(entries))
						location.reload(true);
			});
	});
}

function doLogout() {
	callLogoutCallbacks();
}

function refreshPage() {
}

$(function(){
	initTemplate();
	
	queueJSON("getting login info", "/home/getPeopleData?callback=?",
			getCSRFPreventionObject("getPeopleDataCSRF"),
			function(data){
				if (!checkData(data))
					return;
			
			var found = false;
			
			jQuery.each(data, function() {
				if (!found) {
					// set first user id as the current
					setUserId(this['id']);
					found = true;
				}
				addPerson(this['first'] + ' ' + this['last'],
						this['username'], this['id'], this['sex']);
				return true;
			});
		});
	
	queueJSON("getting comments", "/home/listDiscussionData?callback=?",
		function(data){
			if (!checkData(data))
				return;
		
			jQuery.each(data, function() {
				$("#graphList").append('<div class="graphItem">\
					<div class="summary">\
					<a href="/home/discuss?discussionId=' + this['id'] + '"><h2 class="content-size">' + minLinkName(this['name']) + '</h2></a>\
					<div class="date">' + formatShortDate(this['updated']) + '</div> <div class="userName"> ' + this['userName'] + '</div> '
					+ (this['isAdmin'] ?
						(this['isPublic'] ? '<span class="visibility">visiblity: public to the world</span> <a class="publishButton" href="/home/community?discussionId=' + this['id'] + '&unpublish=true">change</a>' : '<span class="visibility">visible in: ' + ("" + this['groupName']).toLowerCase() + '</span> <a class="publishButton" href="/home/community?discussionId=' + this['id'] + '&publish=true">change</a>')
						+ '<a class="delete" href="#" onclick="deleteDiscussionId(' + this['id'] + ')"><img height="12" width="12" src="/images/x.gif"></a>' : '')
					+ '</div></div>');
				return true;
			});
		});
});
</script>
</head>
<body>
<!-- MAIN -->
<div class="main" id="dashmain">

	<div class="discussHeader" style="padding-bottom:10px">
	<g:if test="${flash.message}">
		<div class="communityMessage">${flash.message.encodeAsHTML()}</div>
	</g:if>

	<a href="/home/discuss?createTopic=true">+ New question or comment</a><p/>
	</div>
	<div id="graphList">
	</div>

</div>
<!-- /MAIN -->

<div style="clear: both;"></div>

</body>
</html>
