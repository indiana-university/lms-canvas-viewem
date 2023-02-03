/*-
 * #%L
 * lms-canvas-viewem
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
(function() {
    var token = $('#_csrf').attr('content');
    var header = $('#_csrf_header').attr('content');
    $(document).ajaxSend(function(e,xhr,options) {
       xhr.setRequestHeader(header, token);
    });
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

            const dropdownId = $(this).parent().attr("id");
            const dropdown = document.querySelector('[data-rvt-dropdown="' + dropdownId + '"]')

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

                    // close the open dropdown
                    dropdown.close();
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

                    // close the open dropdown
                    dropdown.close();
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

                const dropdownId = $(this).parent().attr("id");
                const dropdown = document.querySelector('[data-rvt-dropdown="' + dropdownId + '"]')
                dropdown.close();

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

    $('#student-list').on('change', function() {
        var obj = $(this);
        var urlBase = obj.data('urlbase');
        var userId = obj.val();

        // hide the user info if no user is selected
        if (userId && userId.length > 0) {
            $("#userDataDiv").show();

            //Need a slash at the end to prevent emails from getting the suffix ignored
            $("#userDataDiv").load(urlBase + userId + "/");
        } else {
            $("#userDataDiv").hide();
        }
    });

    // Handler for the file upload input so that IE works like other browsers
    $("form#upload input#file").keydown(function (e) {
        if (e.keyCode == 13) {
            //alert('you pressed enter!');
            e.preventDefault();
            $(this).click();
        }
    });

    $(document).on("fileAttached", function(event) {
        $("#file-upload-error").hide();
        $("#file").attr({"aria-invalid": "false", "aria-describedby": "attachment-status"});
    });

    $(".loading-btn").click(function() {
        var actionValue = $(this).data("action");
        $("#viewem-action").val(actionValue);

        $(this).addClass("rvt-button--loading");
        $(this).attr("aria-busy", "true");
        $(".rvt-button").attr("disabled", "true");
        $(this).find(".rvt-loader").removeClass("rvt-display-none");

        const btnId = $(this).attr("id");
        document.getElementById(btnId).form.submit();
    });

}());
