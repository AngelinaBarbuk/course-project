$(function () {
    $("#dialog-message-ok").dialog({
        modal: true,
        autoOpen: false,
        buttons: {
            Ok: function () {
                $(this).dialog("close");
            }
        }
    });

    $("#dialog-message-fail").dialog({
        modal: true,
        autoOpen: false,
        buttons: {
            Ok: function () {
                $(this).dialog("close");
            }
        }
    });

    $("#dialog-message-confirm-delete").dialog({
        resizable: false,
        height: "auto",
        width: 400,
        modal: true,
        autoOpen: false,
        buttons: {
            Confirm: function (button) {
                $(this).dialog("close");
                var x = document.getElementsByClassName("selected");
                var p = x[0].getElementsByTagName("input")[0].value;
                $.ajax({
                    url: '/dropbox/delete?path=' + p,
                    type: 'DELETE',
                    success: function (result) {
                        $("#dialog-message-ok").dialog('open');
                        location.reload();
                    },
                    fail: function (result) {
                        $("#dialog-message-fail").dialog('open');
                    }
                });
            },
            Cancel: function (button) {
                $(this).dialog("close");
            },
        }
    });

    $("#createNewFolderDialog").dialog({
        resizable: false,
        height: "auto",
        width: 400,
        modal: true,
        autoOpen: false,
        buttons: {
            Create: function (button) {
                $(this).dialog("close");
                var name = $("#name");

                $.ajax({
                    url: '/dropbox/createFolder',
                    type: 'POST',
                    data: {"path": (urlParam('path') + '/' + name.val())},
                    success: function (result) {
                        $("#dialog-message-ok").dialog('open');
                        location.reload();
                    },
                    fail: function (result) {
                        $("#dialog-message-fail").dialog('open');
                    }
                });
            },
            Cancel: function (button) {
                $(this).dialog("close");
            },
        }
    });

    var filemanager = $('.filemanager'),
        breadcrumbs = $('.breadcrumbs'),
        fileList = filemanager.find('.data');

    if (!fileList.length) {
        filemanager.find('.nothingfound').show();
    }
    else {
        filemanager.find('.nothingfound').hide();
    }

    fileList.animate({'display': 'inline-block'});

    context.init({preventDoubleContext: false});
    context.settings({compress: true});

    context.attach('.context-menu-folder', [
        {header: 'Action'},
        {
            text: 'Open', action: function (e) {
            var x = document.getElementsByClassName("selected");
            window.open(x[0].href);
        }
        },
        {
            text: 'Remove', action: function (e) {
            var x = document.getElementsByClassName("selected");
            var p = x[0].getElementsByTagName("input")[0].value;
            if (p) {
                $("#dialog-message-confirm-delete").dialog('open');
            }
        }
        },
        {divider: true},
        {text: 'Move', href: '#'}
    ]);

    context.attach('.context-menu-file', [
        {header: 'Action'},
        {
            text: 'Download', action: function (e) {
            var x = document.getElementsByClassName("selected");
            window.open(x[0].href);
        }
        },
        {
            text: 'Remove', action: function (e) {
            var x = document.getElementsByClassName("selected");
            var p = x[0].getElementsByTagName("input")[0].value;
            if (p) {
                $("#dialog-message-confirm-delete").dialog('open');
            }
        }
        },
        {divider: true},
        {text: 'Move', href: '#'}
    ]);

    $("#createNewFolder").click(function (e) {
        $("#createNewFolderDialog").dialog('open');
    });

})

function urlParam(name) {
    var results = new RegExp('[\?&]' + name + '=([^&#]*)').exec(window.location.href);
    if (results == null || results[1] == "root") {
        return "";
    }
    else {
        return results[1] || 0;
    }
}

function escapeHTML(text) {
    return text.replace(/\&/g, '&amp;').replace(/\</g, '&lt;').replace(/\>/g, '&gt;');
}

function bytesToSize(bytes) {
    var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    if (bytes == 0) return '0 Bytes';
    var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
    return Math.round(bytes / Math.pow(1024, i), 2) + ' ' + sizes[i];
}