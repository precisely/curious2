<html>
<head>
<g:render template="/layouts/barehead" model="['templateVer':templateVer]" />
</head>
<body class="${pageProperty(name: 'body.class') ?: '' }">
<g:render template="/layouts/barebody" model="['templateVer':templateVer]" />
</body>
</html>
