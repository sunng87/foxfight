requirejs.config({
  //In order for proper loading of depenencies in Terraformer modules set the path up in requirejs.config
  paths: {
    Leaflet: "leaflet",
    jQuery: "vendor/jquery-1.9.0.min",
    jRumble: "jrumble/jquery.jrumble.min"
  },
   shim: {
    'Leaflet': {
      exports: 'L'
    },
     'jQuery': {
       exports: '$'
     }
  }
});

requirejs([
  "jQuery",
  "modules/map",
  "modules/battle"
], function ($,m,b) {
  if($("#map").length > 0) {
    m.initialize();
    m.viewCurrent();

    var clientId;

    WebSocket = WebSocket || MozWebSocket;
    var locws = new WebSocket("ws://"+location.host+"/loc/");
    locws.onmessage = function(e){
      var data = JSON.parse(e.data);

      switch (data.type) {
        case "init":
        clientId = data["client-id"];
        sessionStorage.clientId = clientId;
        m.trackCurrent(function(p){
          locws.send(JSON.stringify({"client-id": clientId,
                                     "location": [p.coords.longitude, p.coords.latitude],
                                     "type": "update-location"}));
        });
        m.addControl("<button class=\"btn\"><i class=\"icon-eye-open\"></i></button>",
                    function(e){
                      var btn = $(this);
                      btn.attr("disabled", "disabled");
                      
                      m.viewCurrent(function(){
                        $.ajax("/location/nearby", {
                          data: {lat: m.getCurrentPosition().lat,
                                 lon: m.getCurrentPosition().lng},
                          type: "GET",
                          success: function(r){
                            m.toggleCurrentositionIndicator();

                            var markers = [];
                            r.forEach(function(i){
                              if (i["client-id"] === clientId) {
                                return ;
                              } else {
                                var coords = [i.loc.coordinates[1], i.loc.coordinates[0]];
                                var marker = m.newMarkerWithEvent(coords, function(e){
                                  var resp = window.confirm("Request a fight ?");
                                  locws.send(JSON.stringify({
                                    "target-client-id": i["client-id"],
                                    "type": "request-fighting"
                                  }));
                                });
                                markers.push(marker);
                              }
                            });

                            var lg = m.newMarkerGroup(markers);
                            lg.addTo(m.getMap());
                            setTimeout(function(){
                              m.getMap().removeLayer(lg);
                              m.toggleCurrentositionIndicator();
                            }, 15000);
                          }
                        });
                      });
                      
                      setTimeout(function(){
                        btn.removeAttr("disabled");
                      }, 15000);
                    })

        
        break;


        case "fighting-confirm":
        var from_client_id = data["from-client-id"];
        if (navigator.vibrate) navigator.vibrate(1000);
        if (navigator.mozNotification) {
          var notif = navigator.mozNotification.createNotification(
            "FoxFight", "Warning: someone is going to start a duel with you!");
          notif.show();
        }

        if (window.webkitNotification && window.webkitNotification.checkPermission() == 0) {
          var notif = window.webkitNotification.createNotification(
            null, "FoxFight", 
            "Warning: someone is going to start a duel with you!");
          notif.show();
        }
          
        var accepted = window.confirm(from_client_id.substring(0,5)+"*** wants to fight a duel with you. ");
        locws.send(JSON.stringify({"target-client-id": from_client_id,
                                   "accept": accepted,
                                   "type": "fighting-response"}));
        break;

        
        case "battle-cacncelled":
        break;

        case "battle-start":
        var bid = data["duel-id"];
        window.location = "/battle?bid=" + bid;
        break;
      }


      
    };
  }

  if ($(".battle").length  > 0) {
    b.setup();
  }


  
});

