<g:applyLayout name="logo">
<html>
<head>
<title><g:layoutTitle/></title>
<g:layoutHead/>
<script type="text/javascript" src="/js/curious/autocomplete.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/treeview.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/taglist.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/signals.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/interestTagList.js?ver=21"></script>
<script type="text/javascript" src="/js/jquery/jquery.infinite.scroll.js"></script>
<script type="text/javascript" src="/js/curious/templates/feed-templates.js?ver=21"></script>
<c:jsCSRFToken keys="createHelpEntriesDataCSRF, saveSurveyDataCSRF, getPeopleDataCSRF, 
		getInterestTagsDataCSRF"/>
</head>
<body class="${pageProperty(name: 'body.class') ?: '' }">
<content tag="menu">
	<%--<ul class="signin pull-right">
		<li><span id="displayUser"></span></li>
		<li><a href="/home/logout" id="logoutLink">sign out</a></li>
	</ul>
	--%>
	<div class="pull-right search-bar left-addon">
		<form>
			<i class="fa fa-search"></i>
			<input type="text" placeholder="Search Curious" required >
		</form>
	</div>
	<div class="text-center clearfix">
		<ul class="mainLinks headerLinks">
			<li><g:link controller='home' action="index">Track</g:link></li>
			<li><g:link controller='home' action="graph">Chart</g:link></li>
			<li><a href="/home/social#all">Social</a></li>
			<li><g:link controller='home' action="signals">Signals</g:link></li>
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
	var showModal = ${(session.survey == 'compass')? true: false};
	var showHelpModal = ${session.registrationSuccessful?: false};

	var processUserData = function(data) {
		if (!checkData(data))
			return;

		var found = false;
	
		jQuery.each(data, function() {
			if (!found) {
				// set first user id as the current
				setUserId(this['id']);
				setUserName(this['username']);
				found = true;
			}
			addPerson(this['name'],
					this['username'], this['id'], this['sex']);
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
			})
			if ((typeof showModal != 'undefined') && showModal) {
				var interestTagList;
				$.ajax({
					url: '/home/getSurveyData',
					success: function(data) {
						if (data != null) {
							$('#survey-carousel-content .carousel-inner').html(data);
							var questionCount = $('#survey-carousel-content .carousel-inner').find('.item').length;
							console.log(questionCount)
							if (questionCount == 1) {
								console.log('ha ha question count');
								$('#navigate-right').html('<button type="submit" class="navigate-carousel-right">SUBMIT</button>');
							}
	
							queueJSON("getting login info", "/home/getPeopleData?callback=?", function(data) { 
								if (!checkData(data))
									return;
	
								this.interestTagList = new InterestTagList("interestTagInput", "interestTagList");
							});
	
							$('#takeSurveyOverlay').modal({show: true});
						} else {
							console.log('data error!');
						}
					},
					error: function(xhr) {
						console.log('xhr:', xhr);
					}
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
	    <div class="col-xs-2 col-xs-offset-1">
	    	<ul> 
	    	<li> <span class="ul-head"> Company </span><br></li>
	    		<li ><a href="#">About</a> </li>
	    		<li ><a href="#">Jobs</a> </li>
	    		<li ><a href="#">Contacts</a> </li>
	    	</ul>
	    </div>
	    <div class="col-xs-2">
	    	<ul> 
	    	 <li><span class="ul-head">Policies</span><br></li>
	    		<li ><a href="#">Community Guideline</a> </li>
	    		<li > <g:link controller='home' action="termsofservice" >Terms of Service</g:link></li>
	    		<li ><a href="#">Privacy</a> </li>
	    	</ul>
	    </div>
	    <div class="col-xs-2">
	    	<ul> 
	    		<li> <span class="ul-head">Support</span> <br></li>
	<c:ifLoggedin>
	<li>
		<a data-toggle="modal" href="#" data-target="#helpWizardOverlay">Help</a>
	</li>
	</c:ifLoggedin>
	       		<li><a href="#">Wiki</a> </li>
	       		<li><a href="#">FAQS</a> </li>
	       		<li><a href="#">Email Help</a> </li>
	       	</ul>
	       </div>
	       <div class="col-xs-2">
	       	<ul>
	       	<li> <span class="ul-head">Follow</span><br></li>
	       		<li ><a href="#"> Blog </a></li>
	       		<li ><a href="#">Twitter</a> </li>
	       		<li ><a href="#">Facebook</a> </li>
	       	</ul>
	       </div>
	       <div class="col-xs-2">
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