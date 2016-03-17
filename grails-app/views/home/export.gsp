<g:setProvider library="jquery"/>
<html>
<head>
<meta name="layout" content="menu" />
<script type="text/javascript">
function doLogout() {
	callLogoutCallbacks();
}

$(function() {
	queueJSON("getting login info", "/home/getPeopleData?callback=?",
	  function(data){
	    if (!checkData(data))
	    return;
	
	  found = false;
	
	  jQuery.each(data, function() {
	    if (!found) {
	      // set first user id as the current
	      setUserId(this['id']);
	      found = true;
	    }
	    addPerson(this['name'],
	      this['username'], this['id'], this['sex']);
	    return true;
	  });
	
	  refreshPage();
	});
});
</script>
</head>
<body>
  <!-- MAIN -->
  <div class="main">

    <div id="area0">
      <div id="addData">
		<g:if test="${flash.message}">
		  <div class="message">${flash.message.encodeAsHTML()}</div>
		</g:if>
		<div>First row: import set name in first column, time zone (PDT, EST, etc.) in second, time zone offset in seconds in third</div>
		<g:form action="doUpload" method="post" enctype="multipart/form-data">
		  <input type="file" name="csvFile" style="float:left;" />
		  <input type="image" src="/images/add_data.gif" value="Add Data" class="buttonInput" />
		  <div style="clear:both"></div>
		</g:form>
      </div>
    </div>

  </div>
  <!-- /MAIN -->

  <div style="clear:both;"></div>

</body>
</html>
