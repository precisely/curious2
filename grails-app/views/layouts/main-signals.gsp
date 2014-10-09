<html>
<head>
<g:render template="/layouts/mainhead-signals" model="['templateVer':templateVer]" />
<g:layoutHead />
</head>
<body class="${pageProperty(name: 'body.class') ?: '' }">
<g:render template="/layouts/mainbody-signals" model="['templateVer':templateVer]" />
</body>
</html>
