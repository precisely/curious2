<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery"/>
<html>
<head>
<meta name="layout" content="main" />
<title>Curiousy</title>
<meta name="description" content="A platform for health hackers" />
<style type="text/css">
#entry0 .ui-selecting { background: #99CCFF; }
#entry0 .ui-selected { background: #3399FF; color: white; }
#entry0 { list-style-type: none; margin: 0; padding: 0; width: 60%; }
#entry0 li { margin: 0px; padding: 1px; font-size: 12pt; }
</style>
<script type="text/javascript">
function doLogout() {
	callLogoutCallbacks();
}

$(function() {
	initTemplate();
	
	$.getJSON("/home/getPeopleData?callback=?",
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
	    addPerson(this['first'] + ' ' + this['last'],
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
		  <input type="radio" name="csvtype" value="dateDown">Date in first column<br>
		  <input type="radio" name="csvtype" value="dateAcross" checked>Date in second row<br>
		</g:form>
      </div>
    </div>

  </div>
  <!-- /MAIN -->

  <div style="clear:both;"></div>

</body>
</html>
