<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<g:setProvider library="jquery" />
<html>
<head>
<meta name="layout" content="logo" />
<title>Curious Terms of Service</title>
<style type="text/css">
ol {
	padding-left:20px;
}
ul {
	padding-left:20px;
}
<g:if test="${templateVer == 'lhp'}">
.terms {
	height:500;
	overflow-y:scroll;
    font-size:10pt;
    margin:10px;
    color:#;
    border:1px solid #555;
}
.terms2 {
	height:200;
	overflow-y:scroll;
    font-size:10pt;
    margin:10px;
    color:#;
    border:1px solid #555;
}
</g:if>
<g:else>
.terms {
	height:700;
	overflow-y:scroll;
    font-size:10pt;
    margin:10px;
    color:#;
}
</g:else>
h3 {
    font-size:14pt;
	font-weight:bold;
}
strong {
	font-weight:bold;
	font-size:11pt;
}
p {
	margin-top: 14pt;
}
</style>

<script type="text/javascript">

$(function() {
});
</script>
</head>
<body>
<!-- MAIN -->
<div class="main" id="termsmain">

<g:render template="/home/termsOfService" />
<g:if test="${templateVer == 'lhp'}">

</g:if>

</div>
<div style="clear: both;"></div>

<!-- /MAIN -->


</body>
</html>
