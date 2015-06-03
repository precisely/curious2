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
