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
		<g:if test="${params.userGroupNames}" >
			{userGroupNames:JSON.stringify(['${params.userGroupNames}'])},
		</g:if>
		function(data){
			if (!checkData(data))
				return;
		
			jQuery.each(data, function() {
			    var iconImage='comment-icon.png';

				if (this.isPlot) {
					iconImage='graph-icon.png';
				}

				$("#graphList").append('<div class="graphItem media">\
					<div class="media-object" href="#">\
						<img src="/images/'+ iconImage +'" alt="...">\
					</div>\
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
							</div>\
						</div>\
						<h4 class="media-heading">\
							<a href="/home/discuss?discussionId=' + this['id'] + '">' + minLinkName(this['name']) + '</a>\
						</h4>'
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
							<li><a Ref="/home/community" >Home Feed</a></li>
						</ul>
					</div>
					<span id="queryTitle">${groupFullname}</span>
				</h1>
			</div>
		</div>
	</div>
	<div class="row">
		<div class="col-md-2">
			<h2 class="subscription-list"> YOUR SUBSCRIPTIONS </h2>
			<ul class="subscriptions">
				<li>
					<a href = "/home/community"> Home Feed </a>
				</li>
				<g:each var="membership" in="${groupMemberships}">
					<li>
						<a href = "/home/community?userGroupNames=${membership[0].name}"> ${membership[0].fullName} </a>
					</li>
				</g:each>
			</ul>
		</div>
		<div id="graphList" class="col-md-10">
			<div class="new-post">
				<form action="/discussion/create" method="post">
					<input class="full-width" type="text" placeholder="New question or discussion topic?" name="name" />
					<textarea class="full-width"  name="discussionPost"></textarea>
					<input type="hidden" name="group" value="${groupName}" />
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
