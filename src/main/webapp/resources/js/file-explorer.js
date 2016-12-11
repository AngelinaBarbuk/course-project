$(function () {

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
        {text: 'Open', action: function(e){
            var x = document.getElementsByClassName("selected");
            window.open(x[0].href);
        }},
        /*{text: 'Download', action: function(e){
            var x = document.getElementsByClassName("selected");
            var p = x[0].getElementsByTagName("input")[0].value;
            window.open('/dropbox/download?path='+p);
        }},*/
        {text: 'Remove', action: function(e){
            var x = document.getElementsByClassName("selected");
            var p = x[0].getElementsByTagName("input")[0].value;
            window.open('/dropbox/delete?path='+p);
        }},
        {divider: true},
        {text: 'Move', href: '#'}
    ]);

    context.attach('.context-menu-file', [
        {header: 'Action'},
        {text: 'Download', action: function(e){
            var x = document.getElementsByClassName("selected");
            window.open(x[0].href);
        }},
        {text: 'Remove', action: function(e){
            var x = document.getElementsByClassName("selected");
            var p = x[0].getElementsByTagName("input")[0].value;
            window.open('/dropbox/delete?path='+p);
        }},
        {divider: true},
        {text: 'Move', href: '#'}
    ]);

})

function escapeHTML(text) {
    return text.replace(/\&/g, '&amp;').replace(/\</g, '&lt;').replace(/\>/g, '&gt;');
}

function bytesToSize(bytes) {
    var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    if (bytes == 0) return '0 Bytes';
    var i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
    return Math.round(bytes / Math.pow(1024, i), 2) + ' ' + sizes[i];
}