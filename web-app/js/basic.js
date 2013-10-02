/**
 * Basic logout code to execute in every page.
 */

function refreshPage() {
}
function doLogout() {
    callLogoutCallbacks();
}
$(function() {
    initTemplate();

    $.getJSON("/home/getPeopleData?callback=?", function(data) {
        if (!checkData(data))
            return;

        found = false;

        jQuery.each(data, function() {
            if (!found) {
                // set first user id as the current
                setUserId(this['id']);
                found = true;
            }
            addPerson(this['first'] + ' ' + this['last'], this['username'], this['id'], this['sex']);
            return true;
        });
    });
});