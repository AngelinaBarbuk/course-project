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
                    url: cloudStorage + '/delete?path=' + p,
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

    var filemanager = $('.filemanager'),
        breadcrumbs = $('.breadcrumbs'),
        fileList = filemanager.find('.files');


    if (!fileList.length) {
        filemanager.find('.nothingfound').show();
    }
    else {
        filemanager.find('.nothingfound').hide();
    }

    fileList.animate({'display': 'inline-block'});

    context.init({preventDoubleContext: false});
    context.settings({compress: true});

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
        }
    ]);

    $("#uploadFile").change(function () {
        $("#uploadFileForm").submit();
    });

})
