<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
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
			backgroundJSON("deleting discussion", "/home/deleteDiscussionId?id=" + escape(id) + "&callback=?",
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
	
	backgroundJSON("getting comments", "/home/listDiscussionData?callback=?",
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
						+ '<a class="delete" href="#" onclick="deleteDiscussionId(' + this['id'] + ')"><img height="12" width="12" src="/static/images/x.gif"></a>' : '')
					+ '</div></div>');
				return true;
			});
		});
});
</script>
</head>
<body class="community">
<!-- MAIN -->
<div class="main" id="dashmain">

	<div class="discussHeader" style="padding-bottom:10px">
	<g:if test="${flash.message}">
		<div class="communityMessage">${flash.message.encodeAsHTML()}</div>
	</g:if>

	<a href="/home/discuss?createTopic=true">+ New question or comment</a>
	</div>
	<div id="graphList">
	</div>

</div>
<!-- /MAIN -->

<div style="clear: both;"></div>

</body>
</html>
