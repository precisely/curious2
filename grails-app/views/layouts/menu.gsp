<g:applyLayout name="logo">
<html>
<head>
<title><g:layoutTitle/></title>
<meta property="og:image" content="${grailsApplication.config.grails.serverURL}images/curious-share.png" />
<meta property="og:image:width" content="450"/>
<meta property="og:image:height" content="298"/>
<meta property="og:type" content="website"/>
<g:layoutHead/>
<script type="text/javascript" src="/js/curious/autocomplete.js?ver=22"></script>
<script type="text/javascript" src="/js/curious/treeview.js?ver=22"></script>
<script type="text/javascript" src="/js/curious/taglist.js?ver=22"></script>
<script type="text/javascript" src="/js/curious/curiosities.js?ver=22"></script>
<script type="text/javascript" src="/js/curious/profileTag.js?ver=23"></script>
<script type="text/javascript" src="/js/jquery/jquery.infinite.scroll.js"></script>
<c:jsCSRFToken keys="createHelpEntriesDataCSRF, saveSurveyDataCSRF, getPeopleDataCSRF, hideHelpDataCSRF, 
		getGroupsList, getInterestTagsCSRF, getSurveyDataCSRF"/>
</head>
<body class="${pageProperty(name: 'body.class') ?: '' }">
<content tag="menu">
	<%--<ul class="signin pull-right">
		<li><span id="displayUser"></span></li>
		<li><a href="/home/logout" id="logoutLink">sign out</a></li>
	</ul>
	--%>
	<div class="pull-right search-bar left-addon">
		<g:form name="global-search" controller="search" method="GET">
			<i class="fa fa-search"></i>
			<input type="text" placeholder="Search Curious" name="q" value="${params.q}" >
		</g:form>
	</div>
	<div class="text-center clearfix">
		<ul class="mainLinks headerLinks">
			<li><g:link controller='home' action="index">Track</g:link></li>
			<li><g:link controller='home' action="graph">Chart</g:link></li>
			<li><a href="/home/social#all" id="social-menu">Social</a></li>
			<li><a href="/home/sprint#all">Trackathons</a></li>
			<li><g:link controller='home' action="curiosities">Curiosities</g:link></li>
			<c:ifAdmin>
				<li><g:link controller="admin" action="dashboard">Admin</g:link></li>
			</c:ifAdmin>
			<c:ifLoggedin>
				<li class="dropdown">
					<a href="#" data-toggle="dropdown" class="dropdown-toggle"><b class="caret"></b></a>
					<ul class="dropdown-menu" role="menu">
						<li>
							<span id="displayUser"></span>
						</li>
						<li class="divider"></li>
						<li>
							<a href="/home/logout" id="logoutLink">Logout</a>
						</li>
					</ul>
				</li>
			</c:ifLoggedin>
		</ul>
	</div>
</content>
<script>
	var surveyCode = getSearchParams().surveyCode;
	var showHelpModal = ${session.showHelp?: false};
	var notificationCount;

	var processUserData = function(data) {
		if (!checkData(data))
			return;

		var found = false;
	
		jQuery.each(data, function() {
			if (!found) {
				// set first user id as the current
				setUserId(this['id']);
				setUserName(this['username']);
				setNotificationBadge(this['notificationCount']);
				found = true;
			}
			addPerson(this['name'], this['username'], this['id'], this['sex']);
			return true;
		});
	}
	
	<g:pageProperty name="page.processUserData"/>
	
	$(function() {
		queueJSON("getting login info", "/home/getPeopleData?callback=?",
				getCSRFPreventionObject("getPeopleDataCSRF"),
				function(data) {
					processUserData(data);
				});
		});

		$(window).load(function () {
			$('ul.mainLinks a').each(function() {
				var href = $(this).attr('href');
				if (href.indexOf(window.location.pathname) != -1) {
					$(this).parent().addClass("active");
					return false;
				}
			});

			if ((typeof surveyCode != 'undefined') && surveyCode) {
				var interestTagList;
				var params = {code: surveyCode};
				queuePostJSON("Getting survey data", "/home/getSurveyData", 
						getCSRFPreventionObject('getSurveyDataCSRF', params), function(data) {

					if (data && data.success) {
						var carouselInnerContent = $('#survey-carousel-content .carousel-inner');
						carouselInnerContent.html(data.htmlContent);

						var questionCount = carouselInnerContent.find('.item').length;
						if (questionCount == 1) {
							var submitButtonLink = $('#navigate-right'); 

							var submitButton = submitButtonLink.find('button');
							submitButton.text('SUBMIT');
							submitButton.prop('type', 'submit');

							submitButtonLink.prop('href', '#');
						}

						$('#takeSurveyOverlay').modal({show: true});
					} else {
						showAlert(data.message);
					}
				}, function(xhr) {
					console.log('xhr:', xhr);
				});
			}
			if (showHelpModal) {
				$('#helpWizardOverlay').modal({show: true});
			}
		});
		jQuery.curCSS = jQuery.css;
	
</script>
<g:render template="/survey/takeSurveyModal" /> 
<g:render template="/help/helpWizardOverlay" />

<g:layoutBody/>

<!-- FOOTER -->
<br>
<div class="about-wrapper">
	<div class="row footer-items"> <!-- "row (params.action == 'register' || params.action =='forgot')?'':'orange'">  -->
	    <div class="col-xs-3">
	    	<ul> 
	    	 <li><span class="ul-head">Policies</span><br></li>
	    		<li > <g:link controller='home' action="termsofservice" >Terms of Service</g:link></li>
	    	</ul>
	    </div>
	    <div class="col-xs-3">
	    	<ul> 
	    		<li> <span class="ul-head">Support</span> <br></li>
	<c:ifLoggedin>
	<li>
		<a data-toggle="modal" href="#" data-target="#helpWizardOverlay">Tracking Tutorial</a>
	</li>
	</c:ifLoggedin>
	       		<li><a href="mailto:support@wearecurio.us">Contact Support</a> </li>
	       	</ul>
	       </div>
	       <div class="col-xs-3">
	       	<ul>
	       	<li> <span class="ul-head">Follow</span><br></li>
	       		<li ><a href="https://geo.itunes.apple.com/us/app/we-are-curious/id1063805457?mt=8">iOS App</a></li>
	       		<li ><a href="#">Android Coming Soon</a></li>
	       		<li ><a href="http://www.wearecurio.us/blog/">Blog</a></li>
	       		<li ><a href="https://twitter.com/wearecurious">Twitter</a> </li>
	       		<li ><a href="https://facebook.com/wearecurious">Facebook</a> </li>
	       	</ul>
	       </div>
	       <div class="col-xs-3">
	       	<ul>
	<li> <span class="ul-head">Data</span><br></li>
	<li ><g:link controller='home' action="upload">Import</g:link></li>
	<li ><g:link controller='home' action="download">Export</g:link></li>
	<li ><g:link controller='home' action="polldevices">Poll Devices</g:link></li>
	    	</ul>
	    </div>
	        <%--<ul class="nav nav-pills" style="margin-left: 20px;">
	            <li
	                style="font-size: 16px; padding-left: 0px; display: none"><a style="font-weight: bold;color: #999999"
	                href="#">GET THE APP</a></li>
	            <li style="font-size: 16px; display: none"><a style="font-weight: bold;color: #999999"
	                href="#">TUTORIALS</a></li>
	            <li style="font-size: 16px;"><g:link
	                    controller='home' action="termsofservice" style="font-weight: bold;color: #999999 ">TERMS</g:link></li>
	        </ul> --%>
	</div>
</div>
<link href='//fonts.googleapis.com/css?family=Open+Sans:300normal,300italic,400normal,400italic,600normal,600italic,700normal,700italic,800normal,800italic|Roboto:400normal|Oswald:400normal|Open+Sans+Condensed:300normal|Lato:400normal|Source+Sans+Pro:400normal|Lato:400normal|Gloria+Hallelujah:400normal|Pacifico:400normal|Raleway:400normal|Merriweather:400normal&subset=all' rel='stylesheet' type='text/css'>
<!-- /FOOTER -->
<div style="clear:both;"></div>

</body>
</html>
</g:applyLayout>
