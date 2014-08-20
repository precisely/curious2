<html>
<head>
<g:render template="/layouts/mainhead" model="['templateVer':templateVer]" />
</head>
<body class="${pageProperty(name: 'body.class') ?: '' }">
    <g:render template="/layouts/homebody" model="['templateVer':templateVer]" />
</body>
</html>
