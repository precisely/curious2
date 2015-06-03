<g:applyLayout name="logo">
<html>
<head>
<title><g:layoutTitle/></title>
<g:layoutHead/>
<script type="text/javascript" src="/js/curious/autocomplete.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/treeview.js?ver=21"></script>
<script type="text/javascript" src="/js/curious/feeds.js?ver=21"></script>
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
			<li><a href="/home/feed#all">Feed</a></li>
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
		});
		jQuery.curCSS = jQuery.css;
	
</script>
<g:render template="/survey/takeSurveyModal" /> 
<g:render template="/help/helpWizardOverlay" /> 

<g:layoutBody/>

<!-- FOOTER -->
<br>
<g:render template="/layouts/footer"/>
<!-- /FOOTER -->
<div style="clear:both;"></div>

</body>
</html>
</g:applyLayout>
