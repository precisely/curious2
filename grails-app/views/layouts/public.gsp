<html>
<head>
<g:render template="/layouts/publichead" model="['templateVer':templateVer]" />
</head>
<body class="${pageProperty(name: 'body.class') ?: '' }">
<g:render template="/layouts/publicbody" model="['templateVer':templateVer, 'headerLinks':headerLinks]" />
</body>
</html>
