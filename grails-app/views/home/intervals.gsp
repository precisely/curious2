<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="main" />
<title>Curious</title>
<meta name="description" content="A platform for health hackers" />
<c:jsCSRFToken keys="getPeopleDataCSRF" />
</head>
<body class="community">
<!-- MAIN -->
<div class="main container-fluid" >
	<g:if test="${flash.message}">
		<div class="communityMessage">${flash.message.encodeAsHTML()}</div>
	</g:if>
	<div class="row red-header">
		<div class="col-md-3">
			<div id="actions">
				<span class="icon-triangle icon-triangle-right toggle"></span>
				<ul>
					<li><a Ref="/home/community" >Nav Item 1</a></li>
					<li><a Ref="/home/community" >Nav Item 2</a></li>
				</ul>
			</div>
		</div>
		<div class="col-md-9">
			<h1 class="clearfix">
				<span id="queryTitle">${groupFullname}</span>
			</h1>
		</div>
	</div>
	<div class="row">
		<div class="col-md-3">
			<h2 class="subscription-list"> LEFT COLUMN HEADING </h2>
			<ul class="subscriptions">
				<li>
					<a > LEFT LABEL</a>
				</li>
				<g:each var="membership" in="${groupMemberships}">
					<li>
						<a href = "/home/community?userGroupNames=${membership[0].name}"> ${membership[0].fullName} </a>
					</li>
				</g:each>
			</ul>
		</div>
		<div id="graphList" class="col-md-9">
			<br>
			Main content
			<br>
			<br>
		</div>

	</div>

</div>
<!-- /MAIN -->

<div style="clear: both;"></div>

</body>
</html>
