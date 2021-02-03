(function() {   
    $('.publish-icon').click(function() {
        var parentRowObject = $(this).closest("div.ig-row__layout");
        var hrefText = parentRowObject.find(".ig-title").attr("href");
        var hrefEditText = hrefText.replace("\/view\/","\/edit\/");

        var icon = $('i', this);
        var unpublished = icon.hasClass('icon-unpublished');
        if (unpublished) {
            icon.attr('class', 'icon-publish'); //change icon
            $(this).removeClass('publish-icon-unpublished'); //change color of icon
            $(this).addClass('publish-icon-published'); 
            $(this).parents('.ig-row').addClass('ig-published'); //change color of row
            $('.publish-text', this).text('&nbsp;Published'); //change text for accessibility
            $(this).attr('title', 'Unpublish'); //change tooltip title
            $.post(hrefEditText + "/publish"); // ajax call to set publish status
        }
        else {
            icon.attr('class', 'icon-unpublished');
            $(this).removeClass('publish-icon-published');
            $(this).addClass('publish-icon-unpublished');
            $(this).parents('.ig-row').removeClass('ig-published');
            $('.publish-text', this).text('&nbsp;Unpublished');
            $(this).attr('title', 'Publish');
            $.post(hrefEditText + "/unpublish"); // ajax call to set publish status
        }
    });

    $('.delete-worksheet').click(function() {
        if ($(this).is(':visible')) {
            var confirmResult = confirm('Are you sure you want to delete this worksheet?');

            if(confirmResult == true) {
                var parentRowObject = $(this).closest("div.ig-row__layout");
                var hrefText = parentRowObject.find(".ig-title").attr("href"); //something like /lms-build/viewem/12345/view/123
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
                    }
                });

            }
        }
    });
    
    if ($("#worksheet-tabs").length > 0) {
        $("#worksheet-tabs").tabs();
    }

    $('#student-list').on('change', function() {
        var obj = $(this);
        var urlBase = obj.data('urlbase');
        var userId = obj.val();
        var userName = $(":selected").text();
        $("#student-name").text(userName);

        //Need a slash at the end to prevent emails from getting the suffix ignored
        $("#userDataDiv").load(urlBase + userId + "/");
    });


    // Show/hide the stack trace div, adjusting the icon and text accordingly
    $("#stackTraceHeader").click(function () {
        var headerStateLabel = $("#stackTraceHeaderStateLabel");
        var headerIcon = $("#stackTraceHeader i");
        var content = $("#stackTraceContent");
        content.toggle(200, function () {
            //execute this after toggle is done
            //change text of header based on visibility of content div
            headerStateLabel.text(function () {
                //change text based on condition
                return content.is(":visible") ? "Hide" : "View";
            });
            //change icon
            headerIcon.toggleClass("icon-mini-arrow-right icon-mini-arrow-down");
        });

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