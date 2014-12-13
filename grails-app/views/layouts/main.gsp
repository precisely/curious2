<!DOCTYPE html>
<html>
<head>
<g:render template="/layouts/mainhead" model="['templateVer':templateVer]" />
<g:layoutHead />
</head>
<body class="${pageProperty(name: 'body.class') ?: '' }">
<g:render template="/layouts/mainbody" model="['templateVer':templateVer]" />
</body>
</html>
