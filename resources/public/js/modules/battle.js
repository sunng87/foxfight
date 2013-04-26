define(["jQuery", "jRumble"], function($, _){
  var ws;
  var battleReady = false;
  var p = [];
  var record = false;
  var role;
  var bid;

  Array.prototype.max = function() {
    return Math.max.apply(null, this)
  }

  var dmh = function (e) {
    if(battleReady) {
      var accel = e.accelerationIncludingGravity;
      if (accel) {
        if (accel.x > 18) {
          console.log(accel.x);
          p.push(accel.x - 18);
        }
        
        if (accel.x < 18 && p.length > 0) {
          var power = p.max();
          console.log(power);
          ws.send(JSON.stringify({origin: role,
                                  power: power,
                                  type: "attack",
                                  bid: bid}));
          p.length = 0;
        }
      }
    }
  };

  var rumb; // rumb timeout
  
  return {
    setup: function() {

      $(".rumblable").jrumble();

      bid = $(".battle").attr("data-bid");
      var clientId = sessionStorage.clientId;
      WebSocket = WebSocket || MozWebSocket;
      ws = new WebSocket("ws://" + location.host + "/bat/");

      ws.onmessage = function(e){
        var resp = JSON.parse(e.data);
        
        switch(resp.status) {
        case "start":
          role = resp["from-client-id"] === sessionStorage.clientId ? "from" : "to";
          var from_id = resp["from-client-id"] === sessionStorage.clientId ? 
            "You" : resp["from-client-id"].substring(0, 5) + "***";
          var to_id = resp["to-client-id"] === sessionStorage.clientId ?
            "You" : resp["to-client-id"].substring(0, 5) + "***";
          $(".from h4").html(from_id);
          $(".to h4").html(to_id);

          battleReady = true;
          break;

        case "engadge":
          $(".from progress").attr("value", resp["from-blood"]);
          $(".to progress").attr("value", resp["to-blood"]);

          if (role != resp["attack-origin"]) {
            navigator.vibrate([200]);
            if (rumb) clearTimeout(rumb);
            $(".rumblable").trigger('startRumble');
            rumb = setTimeout(function(){
              $(".rumblable").trigger('stopRumble');
            }, 2000);
          }

          if (resp["from-blood"] < 0 || resp["to-blood"] < 0) {
            battleReady = false;
          }
          
          if (resp[role + "-blood"] < 0) {
            navigator.vibrate([2000,500,2000]);
            alert("You lose");
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

      window.addEventListener("devicemotion", dmh, true);
    }
  }

});
