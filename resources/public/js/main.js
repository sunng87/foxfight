requirejs.config({
  //In order for proper loading of depenencies in Terraformer modules set the path up in requirejs.config
  paths: {
    Leaflet: "leaflet",
    jQuery: "vendor/jquery-1.9.0.min"
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
  "modules/map"
], function ($,m) {
  if($("#map").length > 0) {
    m.initialize();
    m.viewCurrent();

    var clientId;

    WebSocket = WebSocket || MozWebSocket;
    var locws = new WebSocket("ws://"+location.host+"/loc/");
    locws.onmessage = function(e){
      var data = JSON.parse(e.data);
      clientId = data["client-id"];
      m.setClientId(clientId);
      m.trackCurrent(function(p){
        locws.send(JSON.stringify({"client-id": clientId,
                                   "location": [p.coords.longitude, p.coords.latitude],
                                   "type": "update-location"}));
      });
    };
  }
});

