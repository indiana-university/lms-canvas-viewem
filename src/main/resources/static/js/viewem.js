(function() {   
    $('.publish-worksheet').click(function() {
        if ($(this).is(':visible')) {
            var parentRowObject = $(this).closest("tr.worksheet-row");
            var parentTitle = parentRowObject.find(".sheet-title");
            var publishStatus = parentRowObject.find(".status");
            var hrefText = parentTitle.attr("href");
            var hrefEditText = hrefText.replace("\/view\/","\/edit\/");

            var icon = $('use', this);
            var hrefIcon = icon.attr("href");
            var menuText = $('.publish-text', this);

            if (menuText.text()=='Publish') {
                var confirmPublish = confirm('Are you sure you want to publish this worksheet?');
                if(confirmPublish == true) {
                    menuText.text('Unpublish'); // change menu text
                    publishStatus.text('Published'); // change status text
                    var hrefIconReplacement = hrefIcon.replace("check-circle-breakout","undo"); // replace the icon type
                    icon.attr("href", hrefIconReplacement); // implement new href on icon
                    parentTitle.addClass('rvt-button--success-secondary'); // make worksheet title green
                    $.post(hrefEditText + "/publish"); // ajax call to set publish status
                    var publishAlert = document.getElementById("publish-success-alert");
                    $(publishAlert).removeClass('rvt-display-none'); // show the publish alert
                    publishAlert.focus(); // give the alert focus
                    Dropdown.close($(this).parent().attr("id")); // close the open dropdown
                }
            } else {
                var confirmUnpublish = confirm('Are you sure you want to unpublish this worksheet?');
                if(confirmUnpublish == true) {
                    menuText.text('Publish'); // change menu text
                    publishStatus.text('Unpublished'); // change status text
                    var hrefIconReplacement = hrefIcon.replace("undo","check-circle-breakout"); // replace the icon type
                    icon.attr("href", hrefIconReplacement); // implement new href on icon
                    parentTitle.removeClass('rvt-button--success-secondary'); // make worksheet title green
                    $.post(hrefEditText + "/unpublish"); // ajax call to set publish status
                    var unpublishAlert = document.getElementById("unpublish-success-alert");
                    $(unpublishAlert).removeClass('rvt-display-none'); // show the unpublish alert
                    unpublishAlert.focus(); // give the alert focus
                    Dropdown.close($(this).parent().attr("id")); // close the open dropdown
                }
            }
        }
    });

    $('.delete-worksheet').click(function() {
        if ($(this).is(':visible')) {
            var confirmResult = confirm('Are you sure you want to delete this worksheet?');

            if(confirmResult == true) {
                var parentRowObject = $(this).closest("tr.worksheet-row");
                var hrefText = parentRowObject.find(".sheet-title").attr("href"); //something like /lms-build/viewem/12345/view/123
                var hrefDeleteText = hrefText.replace("\/view\/","\/delete\/");  // change to /lms-build/viewem/12345/delete/123

                var values = hrefDeleteText.split("/");
                var length = values.length;
                var id = values[length-1];  //parse to get the sheet id 123
                var updatedId = "#" + "row_" + id; //see listSheets.html, <li th:id="'row_' + ${sheet.sheetId}" th:each="sheet : ${sheets}">

                $.ajax({
                    url: hrefDeleteText,
                    type: 'DELETE',
                    success: function() {
                        $(updatedId).remove();
                        var deleteAlert = document.getElementById("delete-success-alert");
                        $(deleteAlert).removeClass('rvt-display-none'); // show the delete alert
                        deleteAlert.focus(); // give the alert focus
                    }
                });

            }
        }
    });

    $('#delete-close').click(function() {
        var deleteAlert = document.getElementById("delete-success-alert");
        $(deleteAlert).addClass('rvt-display-none'); // hide the alert after clicking the close button
    });

    $('#publish-close').click(function() {
        var publishAlert = document.getElementById("publish-success-alert");
        $(publishAlert).addClass('rvt-display-none'); // hide the alert after clicking the close button
    });

    $('#unpublish-close').click(function() {
        var unpublishAlert = document.getElementById("unpublish-success-alert");
        $(unpublishAlert).addClass('rvt-display-none'); // hide the alert after clicking the close button
    });

    $('#student-list').on('change', function() {
        var obj = $(this);
        var urlBase = obj.data('urlbase');
        var userId = obj.val();
        var userName = $(":selected").text();
        $("#student-name").text(userName);

        //Need a slash at the end to prevent emails from getting the suffix ignored
        $("#userDataDiv").load(urlBase + userId + "/");
    });

    // Handler for the file upload input so that IE works like other browsers
    $("form#upload input#file").keypress(function (e) {
        if (e.keyCode == 13) {
            //alert('you pressed enter!');
            e.preventDefault();
            $(this).click();
        }
    });


}());