<html>
<head>
<meta name="layout" content="grailsmain" />
<title>Login</title>
<style type="text/css">
html * {
    margin: 0;
    /*padding: 0; SELECT NOT DISPLAYED CORRECTLY IN FIREFOX */
}

/* GENERAL */

.spinner {
    padding: 5px;
    position: absolute;
    right: 0;
}

/*
body {
    background: #fff;
    color: #333;
    font: 9px verdana, arial, helvetica, sans-serif;
}*/
#grailsLogo {
	padding:20px;
}

a:link, a:visited, a:hover {
    font-weight: normal;
    text-decoration: none;
}

h1 {
    color: #48802c;
    font-weight: normal;
    font-size: 16px;
    margin: .8em 0 .3em 0;
}

ul {
    padding-left: 15px;
}

input, textarea {
    background-color: #fcfcfc;
    border: 1px solid #ccc;
    font: 11px verdana, arial, helvetica, sans-serif;
    margin: 2px 0;
    padding: 2px 4px;
}
select {
  border: none;
  margin: 0px 0;
  padding: 0px 0px 0px 0px;
  /* margin: 0px 0;
   padding: 0px 0px 0px 0; */
}
textarea {
	width: 250px;
	height: 150px;
	vertical-align: top;
}

input:focus, select:focus, textarea:focus {
    border: 1px solid #b2d1ff;
}

.body {
    float: center;
    margin: 0 15px 10px 15px;
}

.plain {
    float: left;
    margin: 5px 5px 5px 5px;
}

/* NAVIGATION MENU */

.nav {
    background: #fff url(/images/skin/shadow.jpg) bottom repeat-x;
    border: 1px solid #ccc;
    border-style: solid none solid none;
    margin-top: 5px;
    padding: 7px 12px;
}

.menuButton {
    font-size: 10px;
    padding: 0 5px;
}
.menuButton a {
    color: #333;
    padding: 4px 6px;
}
.menuButton a.home {
    background: url(/images/skin/house.png) center left no-repeat;
    color: #333;
    padding-left: 25px;
}
.menuButton a.list {
    background: url(/images/skin/database_table.png) center left no-repeat;
    color: #333;
    padding-left: 25px;
}
.menuButton a.create {
    background: url(/images/skin/database_add.png) center left no-repeat;
    color: #333;
    padding-left: 25px;
}

/* MESSAGES AND ERRORS */

.registermessage {
    background: #f3f8fc url(/images/skin/information.png) 8px 50% no-repeat;
    border: 1px solid #08f;
	color: #006dba;
    margin: 10px 0 5px 0;
    padding: 5px 5px 5px 30px;
    width: 336px;
}
.loginmessage {
    background: #f3f8fc url(/images/skin/information.png) 8px 50% no-repeat;
    border: 1px solid #08f;
	color: #006dba;
    margin: 10px 0 5px 0;
    padding: 5px 5px 5px 30px;
    width: 236px;
}

div.errors {
    background: #fff3f3;
    border: 1px solid red;
    color: #cc0000;
    margin: 10px 0 5px 0;
    padding: 5px 0 5px 0;
}
div.errors ul {
    list-style: none;
    padding: 0;
}
div.errors li {
	background: url(/images/skin/exclamation.png) 8px 0% no-repeat;
    line-height: 16px;
    padding-left: 30px;
}

td.errors select {
    border: 1px solid red;
}
td.errors input {
    border: 1px solid red;
}

/* TABLES */

table {
    border: 1px solid #ccc;
    width: 100%
}
tr {
    border: 0;
}
td, th {
    font: 11px verdana, arial, helvetica, sans-serif;
    line-height: 12px;
    padding: 5px 6px;
    text-align: left;
    vertical-align: top;
}
th {
    background: #fff url(/images/skin/shadow.jpg);
    color: #666;
    font-size: 11px;
    font-weight: bold;
    line-height: 17px;
    padding: 2px 6px;
}
th a:link, th a:visited, th a:hover {
    color: #333;
    display: block;
    font-size: 10px;
    text-decoration: none;
    width: 100%;
}
th.asc a, th.desc a {
    background-position: right;
    background-repeat: no-repeat;
}
th.asc a {
    background-image: url(/images/skin/sorted_asc.gif);
}
th.desc a {
    background-image: url(/images/skin/sorted_desc.gif);
}

.odd {
    background: #f7f7f7;
}
.even {
    background: #fff;
}

/* LIST */

.list table {
    border-collapse: collapse;
}
.list th, .list td {
    border-left: 1px solid #ddd;
}
.list th:hover, .list tr:hover {
    background: #b2d1ff;
}

/* PAGINATION */

.paginateButtons {
    background: #fff url(/images/skin/shadow.jpg) bottom repeat-x;
    border: 1px solid #ccc;
    border-top: 0;
    color: #666;
    font-size: 10px;
    overflow: hidden;
    padding: 10px 3px;
}
.paginateButtons a {
    background: #fff;
    border: 1px solid #ccc;
    border-color: #ccc #aaa #aaa #ccc;
    color: #666;
    margin: 0 3px;
    padding: 2px 6px;
}
.paginateButtons span {
    padding: 2px 3px;
}

/* ACTION BUTTONS */

.registerdialog .buttons {
    width: 800px;
    margin-left: 85px;
    height: 30px;
}

.registerdialog .button {
    background: #fff url(/images/skin/shadow.jpg) bottom repeat-x;
    border: 1px solid #ccc;
    color: #666;
    font-size: 10px;
    margin-top: 5px;
    padding: 0;
    width: 110px;
    height: 20px;
    margin-left: 10px;
    display: inline-block;
}

.logindialog .loginbuttons {
    background: #fff url(/images/skin/shadow.jpg) bottom repeat-x;
    border: 1px solid #ccc;
    color: #666;
    font-size: 10px;
    margin-top: 5px;
    overflow: hidden;
    padding: 0;
    width: 100px;
    margin-left: 131px;
}

.buttons input {
    background: #fff;
    border: 0;
    color: #333;
    cursor: pointer;
    font-size: 10px;
    font-weight: bold;
    margin-left: 3px;
    overflow: visible;
    padding: 2px 6px;
}
.buttons input.save {
    width: 92px;
    margin-left: 10px;
}
.buttons input.recover {
    width: 150px;
    margin-left: 10px;
}

/* DIALOG */

.registerdialog {
	width: 850px;
    padding: 5px 0;
    text-align: left;
    padding: 0px;
}
.registererror {
	color: #f00;
}
.registerline {
	height:20px;
	padding-top:5px;
}
.registerlabel {
	display: inline-block;
	width: 150px;
	font-family: helvetica, sans-serif;
	text-align: right;
	padding-right: 10px;
}
.registerfield {
	display: inline-block;
}
.registerfield input {
	width: 170px;
}
.logindialog {
	width: 250px;
    padding: 5px 0;
    text-align: left;
    border: 1px solid #000;
    padding: 10px;
}
.loginline {
	height: 30px;
}
.loginlabel {
	display: inline-block;
	width: 70px;
	font-family: helvetica, sans-serif;
	font-size: 10pt;
	text-align: right;
	padding-right: 10px;
}
.loginfield {
	display: inline-block;
}
.loginfield input {
	width: 150px;
}
.loginregisterline {
	padding-top: 10px;
	padding-left: 3px;
font-family: helvetica, sans-serif;
	font-size: 10pt;
}
.prop {
    padding: 5px;
}
.prop .name {
    text-align: left;
    width: 15%;
    white-space: nowrap;
}
.prop .value {
    text-align: left;
    width: 85%;
}
.loginbuttons input {
    background: #fff;
    border: 0;
    color: #333;
    cursor: pointer;
    font-size: 10px;
    font-weight: bold;
    margin-left: 3px;
    overflow: visible;
    padding: 2px 6px;
}
.loginbuttons input.delete {
    background: transparent url(/images/skin/database_delete.png) 5px 50% no-repeat;
    padding-left: 28px;
}
.loginbuttons input.edit {
    background: transparent url(/images/skin/database_edit.png) 5px 50% no-repeat;
    padding-left: 28px;
}
.loginbuttons input.save {
    width: 92px;
}
.registersex {
	width: 120px;
	display: inline-block;
}
.registersex input {
	width: 25px;
}

/* TERMS */
.termsIntro {
	margin: 0px 0px 0px 0px;
	font-size: 11pt;
	font-family: sans-serif;
}
.termsIntro p {
	margin-top: 14pt;
}
<g:if test="${templateVer == 'lhp'}">
.terms {
	height:100;
	overflow-y:scroll;
    border: 5px solid #aaa;
    background-color:#eee;
    padding: 10px;
    margin-bottom: 15px;
    font-size:10pt;
    color:#555;
}
.terms2 {
	height:100;
	overflow-y:scroll;
    border: 5px solid #aaa;
    background-color:#eee;
    padding: 10px;
    margin-bottom: 15px;
    font-size:10pt;
    color:#555;
}
</g:if>
<g:else>
.terms {
	height:100;
	overflow-y:scroll;
    border: 5px solid #aaa;
    background-color:#eee;
    padding: 10px;
    margin-bottom: 15px;
    font-size:10pt;
    color:#555;
}
</g:else>
.terms p {
	margin-top: 14pt;
}
.terms ol {
	padding-left:20px;
}
.terms ul {
	padding-left:20px;
}
</style>
<script type="text/javascript">
 $(function(){
$("input:text:visible:first").focus();
 });
</script>
</head>
<body>
<div class="body">
<g:if test="${flash.message}">
<div class="registermessage">${flash.message.encodeAsHTML()}</div>
</g:if>
<div class="registerdialog">
<div class="termsintro">
Our goal at Curious is to provide you ways to ask questions about your health, wellness and your life in general, and discover
answers on your own or with others. You choose what information and data you upload and how you wish it to be shared.
Your data belongs to you. We will not share it with any third party without your consent.
We expect that you have the rights to any content you enter or upload.
<p>Clicking on the "Register" button means you accept the <a href="/home/termsofservice">Privacy Policy and Terms of Service</a>.
<br/>&nbsp;
<g:render template="/home/termsOfService" />
</div>
	<div><h1>New User Signup</h1></div>
	<g:form action="doregister" method="post" >
		<input type="hidden" name="precontroller" value="${precontroller}"/>
		<input type="hidden" name="preaction" value="${preaction}"/>
		
		<div class="registerline">
			<div class="registerlabel ${hasErrors(bean:flash.user,field:'username','registererror')}">
			<label for="username">Username*:</label>
			</div>
			<div class="registerfield">
			<input type="text" name="username" value="${fieldValue(bean:flash.user,field:'username')}"/>
			</div>
		</div>
		
		<div class="registerline">
			<div class="registerlabel ${flash.user?.hasErrors() ? 'registererror':''}">
			<label for="password">Password*:</label>
			</div>
			<div class="registerfield">
			<input type="password" name="password" value=""/>
			</div>
		</div>

		<div class="registerline">
			<div class="registerlabel ${hasErrors(bean:flash.user,field:'email','registererror')}">
			<label for="email">Email*:</label>
			</div>
			<div class="registerfield">
			<input type="text" name="email" value="${fieldValue(bean:flash.user,field:'email')}"/>
			</div>
		</div>
		
		<div class="registerline">
			<div class="registerlabel ${hasErrors(bean:flash.user,field:'name','registererror')}">
			<label for="name">Name:</label>
			</div>
			<div class="registerfield">
			<input type="text" name="name" value="${fieldValue(bean:flash.user,field:'name')}"/>
			</div>
		</div>
		
		<div class="registerline">
			<div class="registerlabel ${hasErrors(bean:flash.user,field:'birthdate','registererror')}">
			<label for="birthdate">Birthdate (MM/DD/YYYY):</label>
			</div>
			<div class="registerfield">
			<input type="text" name="birthdate" value="${fieldValue(bean:flash.user,field:'birthdate')}"/>
			</div>
		</div>
		
		<div class="registerline">
			<div class="registerlabel ${hasErrors(bean:flash.user,field:'sex','registererror')}">
			<label for="sex">Sex:</label>
			</div>
			<div class="registerfield registersex">
			<g:radioGroup name="sex" labels="['Male','Female']" values="['M','F']" value="${fieldValue(bean:flash.user,field:'sex')}" >
			${it.label} ${it.radio}
			</g:radioGroup>
			</div>
		</div>
		
<g:if test="${templateVer == 'lhp'}">
		<div class="registerline">
			<div class="registerlabel">
			<label for="agree">Agree to share de-identified data for LAM research*:</label>
			</div>
			<input type="hidden" name="metaTagName1" value="lhpresearchconsent">
			<input type="hidden" name="metaTagName2" value="lhpmember">
			<input type="hidden" name="metaTagValue2" value="true">
			<div class="registerfield registersex">
			<g:radioGroup name="metaTagValue1" labels="['Y','N']" values="['Y','N']" value="Y" >
			${it.label} ${it.radio}
			</g:radioGroup>
			</div>
		</div>
</g:if>
		
<g:if test="${templateVer == 'lhp'}">
		<input type="hidden" name="groups" value="['announce','lhp','lhp announce']">
</g:if>
<g:else>
		<input type="hidden" name="groups" value="['announce','curious','curious announce']">
</g:else>
		<br/>
		
		<div class="registerline">
			<div class="registerlabel">
			</div>
			<div class="registerfield">
			*Required
			</div>
		</div>
		
		<div class="buttons">
			<div class="button">
			<input class="save" type="submit" name="cancel" value="Cancel" />
			</div>
			<div class="button">
			<input class="save" type="submit" name="register" value="Register" />
			</div>
		</div>
	</g:form>
</div>
</div>
</body>
</html>
