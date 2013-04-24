define(["jQuery"], function($){
  var ws;
  
  return {
    setup: function() {
      var bid = $(".battle").attr("data-bid");
      var clientId = sessionStorage.clientId;
      WebSocket = WebSocket || MozWebSocket;
      ws = new WebSocket("ws://" + location.host + "/bat/");

      ws.onmessage = function(e){
        var resp = JSON.parse(e.data);
        
        console.log(resp)
        switch(resp.status) {
        case "start":
          var from_id = resp["from-client-id"] === sessionStorage.clientId ? 
            "You" : resp["from-client-id"].substring(0, 5) + "***";
          var to_id = resp["to-client-id"] === sessionStorage.clientId ?
            "You" : resp["to-client-id"].substring(0, 5) + "***";
          $(".from h4").html(from_id);
          $(".to h4").html(to_id);
          break;

        case "engadge":
          $(".from progress").attr("value", resp["from-blood"]);
          $(".to progress").attr("value", resp["to-blood"]);

          if (clientId != resp["attack-origin"]) {
            navigator.vibrate([500 200 300]);
          }
          break;

        }
      };

      ws.onopen = function(e) {
        var data = JSON.stringify({"client-id": clientId,
                                   "type": "ready",
                                   "bid": bid});
        ws.send(data);
      };

    }
  }

});
