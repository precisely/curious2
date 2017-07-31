<!DOCTYPE html>
<head>
    <meta name="layout" content="menu" />
    <title>precise.ly - CSV Upload</title>
    <meta name="description" content="A platform for health hackers" />
</head>

<body>
    <div>
        <pre>
            CSV upload result -
            ${message}
        </pre>
        <div>
            <g:link controller="admin" action="dashboard">Back to Admin dashboard</g:link><br>
            <g:link controller="admin" action="uploadTagInputTypeCSV">Back to CSV file upload page.</g:link>
        </div>
    </div>
</body>
</html>