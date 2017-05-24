$(document).ready(function() {
    var info = $('#info');
    var links = $('#links');
    var linksHealth = $('#linksHealth');
    var linksDisplay = false;

    $(".loader").hide();
    $("#loading").hide();
    $("#errors").hide();

    function invokeHttp(req, website, callback) {
        $.ajax({
         method: "GET",
         url: req,
         data: { website: website}
       }).done(function( data ) {
           callback(data)
       }).fail(function() {
         if($(".loader").is(":visible"))
             $(".loader").hide();
          if($("#loading").is(":visible"))
             $("#loading").hide();
         $("#errors").html("Connection failed");
         $("#errors").slideToggle('slow');
       })
    }

    $("#httpUrlForm").submit(function( event ) {
        event.preventDefault();

        if($(".loader").is(":visible"))
            $(".loader").hide();

         if($("#loading").is(":visible"))
            $("#loading").hide();

        if($("#errors").is(":visible"))
            $("#errors").hide();

        if(info.is(":visible"))
            info.hide();

        if(links.is(":visible"))
             links.hide();

        if(linksHealth.is(":visible"))
             linksHealth.hide();

        var website = $("input[name=website]").val();

        if (!website.length) {
            if($(".loader").is(":visible"))
                 $(".loader").hide();
             if($("#loading").is(":visible"))
                $("#loading").hide();
             $("#errors").html("Please provide a website address");
             $("#errors").slideToggle('slow');
        }else {
            if (website.startsWith("http") || website.startsWith("https")) {
                $(".loader").show();
                $("#loading").text("Loading website information...");
                $("#loading").show();
                invokeHttp("/fetch", website, showInfo);
             } else {
                 if($(".loader").is(":visible"))
                   $(".loader").hide();
                 if($("#loading").is(":visible"))
                    $("#loading").hide();
                 $("#errors").html("Protocol not supported should be http or https");
                 $("#errors").slideToggle('slow');
             }
         }
     });

     var showInfo = function(data) {
        info.hide();
        links.hide();
        linksHealth.hide();

        info.html('<h2 class="bg-orange1 text-white">Website Information</h2>');

        var infoTable = "<table class='table table-bordered'> \
                            <tr> \
                                <td>Website:</td> \
                                <td>" + data.url + "</td> \
                            </tr> \
                            <tr> \
                                <td>Title:</td> \
                                <td>" + data.title + "</td> \
                            </tr> \
                            <tr> \
                                <td>Version: </td> <td>" + data.version + "</td> \
                            </tr> \
                            <tr> \
                                <td>Login Page: </td> <td>" + data.isLoginPage + "</td> \
                            </tr> \
                         </table>";
        info.append(infoTable);

        var headingsDisplay = false;

        var headingsTable = "<table class='table table-bordered'> \
                                <thead> \
                                    <tr> \
                                        <th>Heading</th> \
                                        <th>Count</th> \
                                     </tr> \
                                </thead> \
                                <tbody>";

        $.each( data.headings, function( key, value ) {
          headingsTable += '<tr> <td>' + key + ' </td> <td> ' + value + '</td></tr>';
          if (!headingsDisplay)
            headingsDisplay = true;
        });
        headingsTable += "</tbody></table>";

        if (headingsDisplay) {
            info.append("<hr/>");
            info.append("<h2 class='bg-orange1 text-white'>Headings</h2>");
            info.append(headingsTable);
        }

        var linksTable = "<table class='table table-bordered'> \
         <thead> \
             <tr> \
                 <th>Link</th> \
                 <th>Count</th> \
              </tr> \
          </thead> \
          <tbody>";

        $.each( data.links, function( key, value ) {
          linksTable += '<tr> <td> ' + key + '</td> <td>' + value + '</td></tr>';
          if (!linksDisplay)
            linksDisplay = true;
        });
        linksTable += "</tbody></table>";

        if (linksDisplay) {
            links.html("<h2 class='bg-orange1 text-white'>Links</h2>");
            links.append(linksTable);
        }

        info.slideToggle('slow');
        links.slideToggle('slow');

         $(".loader").hide();
         $("#loading").hide();
         linksHealthInfo();
     }

     var linksHealthInfo = function() {
        $(".loader").show();
        $("#loading").text("Performing health check of links...");
        $("#loading").show();
        var website = $("input[name=website]").val();
        invokeHttp("/healthCheck", website, linksHealthShow);
     }

     var linksHealthShow = function(data) {

        if (linksDisplay) {

            var linksHealthTable = "<table class='table table-bordered'> \
                                            <thead \
                                                <tr> \
                                                    <th>Link</th>\
                                                    <th>Status Code</th> \
                                                    <th>Status Message</th> \
                                                    <th>Active</th> \
                                                 </tr> \
                                             </thead> \
                                             <tbody>";
                $.each(data, function( index, value ) {
                    linksHealthTable += '<tr> <td class="wrap"><a href="' + value.link + '" target="_blank">' + value.link + '</a></p></td> <td><p>' + value.statusCode + '</p></td><td class="wrap"><p>' + value.statusMessage + '</p></td> <td><p>' + value.active + '</p></td></tr>'
                });

                linksHealthTable += "</tbody></table>";
                linksHealth.html("<h2 class='bg-orange1 text-white'>Links Health</h2>");
                linksHealth.append(linksHealthTable);
                linksHealth.slideToggle('slow');
                ascrollto("linksHealth");
            }

            $(".loader").hide();
            $("#loading").hide();
        }


        function scrollto(id) {
        	var etop = $('#' + id).offset().top;
        	$(window).scrollTop(etop);
        }

        function ascrollto(id) {
        	var etop = $('#' + id).offset().top;
        	$('html, body').animate({
        	  scrollTop: etop
        	}, 1000);
        }
});
