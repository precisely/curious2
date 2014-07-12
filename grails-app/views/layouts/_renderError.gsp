<ul class="text-danger">
    <g:each in="${instances }" var="${domainInstance }">
        <g:eachError bean="${domainInstance}" var="error">
            <li>
                <g:message error="${error}" />
            </li>
        </g:eachError>
    </g:each>
</ul>
