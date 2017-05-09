$(function () {
    var cloudStorage = document.getElementById("cloudStorage").value;
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
                    url: cloudStorage + '/delete?path=' + p,
                    type: 'DELETE',
                    success: function (result) {
                        /*$("#dialog-message-ok").dialog('open');*/
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
                var data;
                if (cloudStorage == '/dropbox') {
                    data = {"path": (urlParam('path') + '/' + name.val())};
                } else if (cloudStorage == '/yandex') {
                    data = {"path": (urlParam('path') + name.val())};
                } else if (cloudStorage == '/google') {
                    data = {
                        "path": (urlParam('path')),
                        "folderName": name.val()
                    }
                }

                $.ajax({
                    url: cloudStorage + '/createFolder',
                    type: 'POST',
                    data: data,
                    success: function (result) {
                        /*$("#dialog-message-ok").dialog('open');*/
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

    /*$("#moveDialog").dialog({
        resizable: false,
        height: "auto",
        width: 400,
        modal: true,
        autoOpen: false,
        buttons: {
            Cancel: function (button) {
                $(this).dialog("close");
            },
        }
    });*/

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
        /*{divider: true},
        {text: 'Move', href: '#'}*/
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
        }/*,
        {divider: true},
        {
            text: 'Move', action: function (e) {
            var x = document.getElementsByClassName("selected");
            var p = x[0].getElementsByTagName("input")[0].value;
            if (p) {
                var url;
                if (cloudStorage == '/dropbox') {
                    url = "/google";
                } else if (cloudStorage == '/google') {
                    url = "/dropbox";
                }
                $.ajax({
                    url: url + '/getFolders?path=root',
                    type: 'GET',
                    success: function (result) {
                        var folders = result['folders'];
                        var x = document.getElementById("moveDialogList");
                        x.innerHTML = '';
                        for (var i = 0; i < folders.length; i++) {
                            var cloudFile = folders[i];
                            var folder = $('<tr class="row">' +
                                '<td>' + cloudFile['showName'] + '</td>' +
                                '<td><input type="button" name="moveDialogOpenFolderBtn" href="' + '/getFolders?path=' + cloudFile['path'] + '" class="btn btn-default" value="Open folder"/></td>' +
                                '<td><input type="button" name="moveDialogSelectFolderBtn" href="' + cloudFile['path'] + '" class="btn btn-default" value="Select folder"/></td>' +
                                '</tr>');
                            folder.appendTo(x);
                        }
                        $("#moveDialog").dialog('open');
                        /!*$("#dialog-message-ok").dialog('open');
                         location.reload();*!/
                    },
                    fail: function (result) {
                        $("#dialog-message-fail").dialog('open');
                    }
                });
            }
        }
        },
        {
            text: 'Encrypt', action: function (e) {
            var x = document.getElementsByClassName("selected");
            var p = x[0].getElementsByTagName("input")[0].value;
            if (p) {
                $.ajax({
                    url: cloudStorage + '/encrypt?path=' + p,
                    type: 'GET',
                    success: function (result) {
                        $("#dialog-message-ok").dialog('open');
                        location.reload();
                    },
                    fail: function (result) {
                        $("#dialog-message-fail").dialog('open');
                    }
                });
            }
        }
        }*/
    ]);

    $("#createNewFolder").click(function (e) {
        $("#createNewFolderDialog").dialog('open');
    });

    $("#uploadFile").change(function () {
        $("#uploadFileForm").submit();
    });

    $('#moveDialog').on('click', 'input[name="moveDialogOpenFolderBtn"]', function (e) {
        var x = document.getElementsByClassName("selected");
        var p = x[0].getElementsByTagName("input")[0].value;
        if (p) {
            var path = e.target.getAttribute("href");
            var url;
            if (cloudStorage == '/dropbox') {
                url = "/google";
            } else if (cloudStorage == '/google') {
                url = "/dropbox";
            }

            $.ajax({
                url: url + path,
                type: 'GET',
                success: function (result) {
                    var folders = result['folders'];
                    var prev = result['prevFolder'];
                    var prevBtn = document.getElementById("upBtn");
                    if (prev != '') {
                        prevBtn.removeAttribute("style");
                        prevBtn.setAttribute("href", '/getFolders?path=' + prev);
                    } else {
                        prevBtn.setAttribute("style", "display: none;");
                    }

                    var x = document.getElementById("moveDialogList");
                    x.innerHTML = '';
                    for (var i = 0; i < folders.length; i++) {
                        var cloudFile = folders[i];
                        var folder = $('<tr class="row">' +
                            '<td>' + cloudFile['showName'] + '</td>' +
                            '<td><input type="button" name="moveDialogOpenFolderBtn" href="' + '/getFolders?path=' + cloudFile['path'] + '" class="btn btn-default" value="Open folder"/></td>' +
                            '<td><input type="button" name="moveDialogSelectFolderBtn" href="' + cloudFile['path'] + '" class="btn btn-default" value="Select folder"/></td>' +
                            '</tr>');
                        folder.appendTo(x);
                    }
                    $("#moveDialog").dialog('open');
                },
                fail: function (result) {
                    $("#dialog-message-fail").dialog('open');
                }
            });

        }
    });


    $('#moveDialog').on('click', 'input[name="moveDialogSelectFolderBtn"]', function (e) {
        $("#moveDialog").dialog('close');
        var x = document.getElementsByClassName("selected");
        var p = x[0].getElementsByTagName("input")[0].value;
        if (p) {
            var path = e.target.getAttribute("href");
            var url;
            if (cloudStorage == '/dropbox') {
                url = "/dropbox_to_google";
            } else if (cloudStorage == '/google') {
                url = "/google_to_dropbox";
            }
            var data = {
                "fileToMove": p,
                "pathToMove": path
            };
            $.ajax({
                url: url,
                type: 'POST',
                data: data,
                success: function (result) {
                    $("#dialog-message-ok").dialog('open');
                },
                fail: function (result) {
                    $("#dialog-message-fail").dialog('open');
                }
            });

        }
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
