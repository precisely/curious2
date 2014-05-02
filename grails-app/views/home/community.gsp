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
				$("#graphList").append('<div class="graphItem media">\
					<a class="pull-left" href="#">\
						<img class="media-object" src="..." alt="...">\
					</a>\
					<div class="media-body">\
						<div class="row">\
							<div class="col-md-4">\
								<span class="uppercase">\
									'+'POSTED BY'+'\
									' + this['userName'] + '\
								</span>\
								<span>\
									ON ' + formatShortDate(this['updated']) + '\
								</span>\
							</div>\
							<div class="col-md-4">\
								<span>\
									'+'LAST COMMENT ON '+'\
								' + formatShortDate(this['updated']) + '\
								</span>\
							</div>\
							<div class="col-md-2">\
								<span>\
									'+'SAVE '+'\
								</span>\
								<span class="delete" onclick="deleteDiscussionId(' + this['id'] + ')">x</span>\
							</div>\
						</div>\
						<h4 class="media-heading">\
							<a href="/home/discuss?discussionId=' + this['id'] + '">' + minLinkName(this['name']) + '</a>\
						</h4>\
						' + this['plotDataId'] +
					+ '</div>'
					+ '</div></div>');
				return true;
			});
		});
});
</script>
</head>
<body class="community">
<!-- MAIN -->
<div class="main container-fluid" >
	<g:if test="${flash.message}">
		<div class="communityMessage">${flash.message.encodeAsHTML()}</div>
	</g:if>
	<div class="row">
		<div class="col-md-12">
			<div class="red-header"	>
				<h1 class="clearfix">
					<div id="actions">
						<img src="/images/menu.png">
						<ul>
							<li><a Ref="/home/discuss?createTopic=true" >Home Feed</a></li>
							<li><a href="#" onclick="plot.save()">Save</a></li>
							<li><g:link action="load">Load</g:link></li>
							<li><a href="#" onclick="plot.saveSnapshot()">Share (Publish to Community)  
								<img src="/images/eye.png">	
							</a></li>
						</ul>
					</div>
					<span id="queryTitle">HOME FEED</span>
				</h1>
			</div>
		</div>
	</div>
	<div class="row">
		<div class="col-md-2">
			<ul class="subscriptions">
				<li>Subscription1</li>
				<li>Subscription2</li>
				<li>Subscription3</li>
			</ul>
		</div>
		<div id="graphList" class="col-md-10">
			<div class="new-post">
				<form action="discussion/create" >
					<input class="full-width" type="text" placeholder="New question or discussion topic?" name="name" />
					<textarea class="full-width"  name="discussionPost"></textarea>
					<input type="submit" name="POST" value="POST"  />
				</form>
			</div>
		</div>

	</div>

</div>
<!-- /MAIN -->

<div style="clear: both;"></div>

</body>
</html>
